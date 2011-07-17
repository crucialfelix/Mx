

Mx : AbstractPlayerProxy {
	
	var <channels,<cables;
	var myUnit,<inlets,<outlets;
	
	var allocator,register,unitGroups,busses;
	var <master;
	var removing,adding, cableEndpoints,<autoCables;
	
	*new { arg channels, cables,inlets,outlets;
		^super.new.init(channels,cables,inlets,outlets)
	}
	storeArgs { 
		^[channels.collect(_.saveData), cables.collect(_.saveData) ] 
		// inlets, outlets not saved yet
	}
	init { arg chans,cabs,ins,outs;
		register = IdentityDictionary.new;
		allocator = NodeIDAllocator(0,0);

		inlets = ins ? [];
		outlets = outs ?? {
			this.addOutput;
			outlets
		};
		channels = (chans ? []).collect(MxChannel.loadData(_,this));
		adding = channels.copy; // not needed really

		cables = (cabs ? []).collect(MxCable.loadData(_,this));
		autoCables = [];
	}
	nextID {
		^allocator.alloc
	}
	// registerIOlet
	register { arg object,uid;
		uid = uid ?? { this.nextID };
		register[uid] = object;
		object.uid = uid;
		^uid
	}
	atID { arg uid;
		^register[uid]
	}
	unregister { arg uid;
		register.removeAt(uid)
	}
	
	add { arg ... objects;
		^this.insertChannel(channels.size,objects)
	}
	insertChannel { arg index, objects;
		var chan,units;
		units = (objects ? []).collect(MxUnit.make(_,this));
		chan = MxChannel(units,cableTo:master.uid);
		chan.makeUnit(this).registerWithMx(this);
		
		if(channels.size < (index+1),{
			channels = channels.extend(index+1,nil)
		});
		channels.put(index,chan);
		this.updatePoints;
		chan.pending = true;
		adding = adding.add(chan);
		^chan
	}
	removeChannel { arg index;
		var chan;
		chan = channels.removeAt(index);
		chan.pending = true;
		removing = removing.add( chan );
		this.updatePoints;
	}
	updatePoints {
		channels.do { arg ch,ci;
			ch.units.do { arg un,ri;
				un.point = ci@ri
			}
		};
		// should be all outlets
		master.units.do { arg un,ri;
			un.point = channels.size@ri
		};
		// temporary hack to give it a point
		// will refactor the master anyway
		master.myUnit.point = channels.size@0;
	}
	addOutput { arg rate='audio',numChannels=2;
		// change this to keep the master as just a normal channel on grid
		// created by .mixer
		// the MxOutlet can specify how to find that
		// and master will be just for the player to use as its out
		// only trick then is to make addChannel insert before the output channels
		// add audio output
		var chan,out;
		chan = MxChannel.loadData([],this);
		if(master.isNil,{ // first added output channel becomes the master
			master = source = chan;
		});
		
		// do not like
		out = MxOutlet("out",outlets.size,'audio'.asSpec,MxPlaysOnBus({chan.bus}));
		this.register(out);
		out.unit = chan.myUnit;
		
		outlets = outlets.add(out);
		adding = adding.add(chan);
		^chan
	}
	at { arg chan,index;
		^(channels.at(chan) ? []).at(index)
	}
	put { arg chan,index,object;
		var channel,unit;
		channel = channels[chan];// should create chans if needed
		unit = MxUnit.make(object,this);
		channel.put(index,unit);
		this.updatePoints
	}
	
	// API
	getInlet { arg point,index;
		var unit;
		unit = channels[point.x].units[point.y] ?? {Error("no unit at" + point).throw};
		^unit.getInlet(index)
	}
	getOutlet { arg point,index;
		var unit;
		unit = channels[point.x].units[point.y] ?? {Error("no unit at" + point).throw};
		^unit.getOutlet(index)
	}
		
	connect { arg fromUnit,outlet, toUnit, inlet, mapping=nil;
		/*
			unit: channelNumber@slotNumber
			outlet/inlet: 
				\outletName 
				integer Index 
				nil meaning first 
				outlet/inlet object
		*/
		var cable;
		// should be in API
		if(outlet.isKindOf(MxOutlet).not,{
			outlet = this.getOutlet(fromUnit,outlet);
		});
		if(inlet.isKindOf(MxInlet).not,{	
			inlet = this.getInlet(toUnit,inlet);
		});
		
		// actual connection here
		// remove any that goes to this inlet
		// only the MxChannel inputs are supposed to mix multiple inputs
		// normal patch input points do not
		cables.do { arg oldcable;
			if(oldcable.inlet === inlet,{
				if(oldcable.active,{
					this.disconnectCable(oldcable);
				});
			})
		};
		cable = MxCable( outlet, inlet, mapping );
		if(this.isPlaying,{
			cable.pending = true;
			adding = adding.add( cable );
		});
		cables = cables.add( cable );
	}
	disconnect { arg fromUnit,outlet, toUnit, inlet;
		// TODO
	}
	mute { arg channel,boo=true;
		channels[channel].mute = boo
	}
	// enact all changes on the server after things have been added/removed dis/connected
	update { arg bundle=nil;
		var b;
		b = bundle ?? { MixedBundle.new };
		removing.do { arg r; r.freeToBundle(b) };
		// new channels
		adding.do { arg a;
			var g,prev,ci;
			if(a.isKindOf(MxChannel),{
				g = Group.basicNew(group.server);
				ci = channels.indexOf(a);
				prev = channels[ ci - 1];
				if(prev.notNil,{
					b.add( g.addAfterMsg( prev.group ) )
				},{
					b.add( g.addToHeadMsg( group ) )
				});
				a.prepareToBundle(group,b,true,groupToUse: g); 
			},{
				a.prepareToBundle(group,b,true);
			}); 
			a.spawnToBundle(b) 
		};
		channels.do { arg chan; chan.update(b); };
		b.addFunction({
			// TODO
			//removing.do { arg r;
			//	r.unregisterWithMx
				
			removing = adding = nil
		});
		this.autoCablesToBundle(b);
		if(bundle.isNil,{
			b.send(this.server)
		});
		^b
	}
	

	//////////  private  ////////////
	disconnectCable { arg cable;
		if(this.isPlaying,{
			removing = removing.add( cable );
			cable.pending = true;
		});
		cables.remove(cable);
	}
	
	children {
		^super.children ++ channels
	}
	loadDefFileToBundle { arg b,server;
		this.children.do(_.loadDefFileToBundle(b,server))
	}		
	spawnToBundle { arg bundle;
		channels.do(_.spawnToBundle(bundle));
		super.spawnToBundle(bundle);
		this.spawnCablesToBundle(bundle);
		bundle.addFunction({
			adding = removing = nil;
		});
	}
	prepareChildrenToBundle { arg bundle;
		channels.do { arg c;
			if(c !== master,{
				c.prepareToBundle(group,bundle,true)
			})
		};
		master.prepareToBundle(group,bundle,false,this.bus);
	}

	spawnCablesToBundle { arg bundle;
		cables.do(_.spawnToBundle(bundle));
		this.autoCablesToBundle(bundle);
	}
	autoCablesToBundle { arg bundle;
		/* updates autoCables, adding and removing to get to current patch state */
		var patched,autoCabled;
		
		// where does channel to get honored ?
		
		// already explicitly cabled
		patched = [];
		cables.do { arg c;
			patched.add( c.outlet.unit );
		};
		// already autoCabled
		autoCabled = IdentityDictionary.new;
		autoCables.do { arg c;
			autoCabled[c.outlet.unit] = c;
		};
		autoCabled.removeAt(master.myUnit);
		channels.do { arg chan;
			var newautos,chanOut;
			chan.units.do { arg unit;
				// should be auto cabled, and isn't already
				if(patched.includes(unit).not,{
					if(autoCabled.at(unit).isNil ,{
						newautos = newautos.add( unit );
					},{
						autoCabled.removeAt( unit );
					});
				});
			};
			newautos.do { arg unit;
				var ac;
				// connect everything to the first inlet which is a recieving api point
				ac = MxAutoCable( unit.outlets.first, chan.myUnit.inlets.first );
				autoCables = autoCables.add( ac );
				ac.spawnToBundle(bundle);
			};
			if(chan !== master,{ // patch channel to master
				if(patched.includes(chan.myUnit).not,{
					if(autoCabled.at(chan.myUnit).isNil,{ 
						{
							var ac;
							ac = MxAutoCable( chan.myUnit.outlets.first, master.myUnit.inlets.first );
							autoCables = autoCables.add( ac );
							ac.spawnToBundle(bundle);
						}.value;
					},{
						autoCabled.removeAt( chan.myUnit );
					});
				});
			});					
		};
		// these are left from a previous patch-state and can be removed now
		autoCabled.keysValuesDo { arg unit,cable;
			cable.stopToBundle(bundle)
		};
	}
	
	guiClass { ^MxGui }
}



