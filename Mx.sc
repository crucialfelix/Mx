

Mx : AbstractPlayerProxy {
	
	var <channels,<cables;
	var inlets,outlets;
	
	var allocator,register,unitGroups,busses;
	var <master;
	var removing,adding, cableEndpoints,autoCables;
	
	*new { arg channels, cables,inlets,outlets;
		^super.new.init(channels,cables,inlets,outlets)
	}
	init { arg chans,cabs,ins,outs;
		channels = chans ?? {[]};
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
		channels = channels.insert(index,chan);
		if(this.isPlaying,{
			chan.pending = true;
			adding = adding.add(chan);
		});
		^chan
	}
	removeChannel { arg index;
		var chan;
		chan = channels.removeAt(index);
		if(this.isPlaying,{
			chan.pending = true;
			removing = removing.add( chan )
		})
	}
	
	addOutput { arg rate='audio',numChannels=2;
		// add audio output
		var chan,out;
		chan = MxChannel(this,nil,[]);
		if(master.isNil,{ // first added output channel becomes the master
			master = source = chan;
		});
		chan.myUnit = MxUnit.make(chan,this);
		out = MxOutlet(this.nextID,"out",outlets.size,'audio',MxPlaysOnBus({chan.bus}));
		outlets = outlets.add(out);
		^chan
	}
	at { arg chan,index;
		^channels.at(chan).at(index)
	}
	put { arg chan,index,object;
		var channel,unit;
		channel = channels[chan];
		if(channel.at(index).notNil,{
			removing = removing.add( channel.removeAt(index) );
		});
		unit = MxUnit.make(object,this);
		adding = adding.add(unit);
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
		
		// already explicitly cabled
		patched = [];
		cables.do { arg c;
			patched.add( c.outlet.unit );
		};
		// already autoCabled
		autoCabled = [];
		autoCables.do { arg c;
			autoCabled = autoCabled.add( c.outlet.unit ) 
		};
		channels.do { arg chan;
			var newautos,chanOut;
			chan.units.do { arg unit;
				// should be auto cabled, and isn't already
				if(patched.includes(unit).not and: {autoCabled.includes(unit).not} ,{
					newautos = newautos.add( unit );
					autoCabled.remove( unit );
				});
			};
			if(newautos.size > 0,{
				newautos.do { arg unit;
					var ac;
					// connect everything to the first inlet which is a recieving api point
					ac = MxAutoCable( unit.outlets.first, chan.myUnit.inlets.first );
					autoCables = autoCables.add( ac );
					ac.spawnToBundle(bundle);
				};
			});
			if(chan !== master,{ // patch channel to master
				if( patched.includes(chan.myUnit).not and: {autoCabled.includes(chan.myUnit).not},{ 
					{
						var ac;
						ac = MxAutoCable( chan.myUnit.outlets.first, master.myUnit.inlets.first );
						autoCables = autoCables.add( ac );
						ac.spawnToBundle(bundle);
						
						autoCabled.remove( chan.myUnit );
					}.value;
				});
			});					
			
			// MxChannels are patched to master with explicit cables when created
		};
		// these are left from a previous patch-state and can be removed now
		autoCabled.do { arg ac;
			ac.stopToBundle(bundle)
		};
	}

	// enact all changes on the server after things have been added/removed dis/connected
	update { arg bundle=nil;
		var b;
		b = bundle ?? { MixedBundle.new };
		removing.do { arg r; r.freeToBundle(b) };
		// prepare and
		adding.do { arg a; a.spawnToBundle(b) };
		removing = nil;
		adding = nil;
		this.autoCables(b);
		if(bundle.isNil,{
			b.send(this.server)
		});
		^b
	}
}



