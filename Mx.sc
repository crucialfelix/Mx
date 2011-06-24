

Mx : AbstractPlayerProxy {
	
	var <channels,<cables;
	var myUnit,inlets,outlets;
	
	var allocator,register,unitGroups,busses;
	var <master;
	var removing,adding, cableEndpoints,autoCables;
	
	*new { arg channels, cables,inlets,outlets;
		^super.new.init(channels,cables,inlets,outlets)
	}
	storeArgs { ^[channels,cables] }// inlets, outlets
	init { arg chans,cabs,ins,outs;
		channels = chans ?? {[]};
		adding = channels.copy;
		cables = cabs ? [];
		allocator = NodeIDAllocator(0,0);
		inlets = ins ? [];
		outlets = outs ?? {
			this.addOutput;
			outlets
		};
		register = IdentityDictionary.new;
		autoCables = [];
	}
	nextID {
		^allocator.alloc
	}
	register { arg uid,object;
		register[uid] = object
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
		chan = MxChannel(this.nextID,master.id, units);
		// how would this get reloaded ?  save it with its id intact ?
		chan.myUnit = MxUnit.make(chan,this);
		if(channels.size < (index+1),{
			channels = channels.extend(index+1,nil)
		});
		channels.put(index,chan);
		chan.pending = true;
		adding = adding.add(chan);
		^chan
	}
	removeChannel { arg index;
		var chan;
		chan = channels.removeAt(index);
		chan.pending = true;
		removing = removing.add( chan )
	}
	
	addOutput { arg rate='audio',numChannels=2;
		// add audio output
		var chan,out;
		chan = MxChannel(this.nextID,nil);
		if(master.isNil,{ // first added output channel becomes the master
			master = source = chan;
		});
		chan.myUnit = MxUnit.make(chan,this);
		out = MxOutlet("out",outlets.size,'audio',MxPlaysOnBus({chan.bus}));
		out.uid = this.nextID;
		out.unit = chan.myUnit;
		outlets = outlets.add(out);
		adding = adding.add(chan);
		^chan
	}
	at { arg chan,index;
		^channels.at(chan).at(index)
	}
	put { arg chan,index,object;
		var channel,unit;
		channel = channels[chan];// should create chans if needed
		unit = MxUnit.make(object,this);
		channel.put(unit);
	}
	
	// API
	getInlet { arg point,index;
		var unit;
		unit = channels[point.x].units[point.y];
		^unit.getInlet(index)
	}
	getOutlet { arg point,index;
		var unit;
		unit = channels[point.x].units[point.y];
		^unit.getOutlet(index)
	}
		
	connect { arg fromUnit,outlet, toUnit, inlet, mapping=nil;
		/*
			unit: channelNumber@slotNumber
			outlet/inlet: 
				\outletName 
				integer Index 
				nil meaning first 
		*/
		var cable;
		outlet = this.getOutlet(fromUnit,outlet);
		inlet = this.getInlet(toUnit,inlet);
		
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
			removing = adding = nil
		});
		this.autoCables(b);
		if(bundle.isNil,{
			b.send(this.server)
		});
		^b
	}


	////////// private   ////////////
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
		this.autoCables(bundle);
	}
	autoCables { arg bundle;
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
}



