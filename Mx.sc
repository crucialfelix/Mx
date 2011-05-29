

Mx : AbstractPlayerProxy {
	
	var <channels,<cables;
	var inlets,outlets;
	
	var allocator,unitGroups,busses;
	var master;
	var removing,adding;
	
	*new { arg channels, cables,inlets,outlets;
		^super.new.init(channels,cables,inlets,outlets)
	}
	init { arg chans,cabs,ins,outs;
		channels = chans ?? {Array.new(8)};
		cables = cabs ? [];
		allocator = NodeIDAllocator(0,0);
		inlets = ins ? [];
		outlets = outs ?? {
			this.addOutput;
			outlets
		}
	}
	nextID {
		^allocator.alloc
	}
	
	addChannel { arg ... objects;
		^this.insertChannel(channels.size,objects)
	}
	insertChannel { arg index, objects;
		var chan;
		objects = (objects ? []).collect(MxUnit(_,this));
		chan = MxChannel(this.nextID,master.id, objects);
		chan.pending = true;
		channels = channels.insert(index,chan);
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
		chan = MxChannel(this.nextID,nil,[]);
		if(master.isNil,{
			master = source = chan;
		});
		out = MxOutlet(this.nextID,"out",outlets.size,'audio',MxPlaysOnBus.new);
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
		unit = MxUnit(object,this);
		adding = adding.add(unit);
		channel.put(unit);
	}
	
	// outletID, inletID		
	connect { arg outlet,inlet,mapping=nil;
		var oldcable,cable,key;
		key = outlet -> inlet;
		// remove any that goes to this inlet
		cables.keysValuesDo({ arg k,oldcable;
			if(k.value === inlet,{
				if(oldcable.active,{
					this.disconnectCable(oldcable);
				});
			})
		});
		cable = MxCable( outlet, inlet, mapping );
		cable.pending = true;
		// play to head of inlet.unit
		// cable should not create a bus 
		adding = adding.add( cable );
		// cable.prepareToBundle(inlet.unit.group,bundle,bus:this.busForUnit(inlet.unit));
		// cable.spawnToBundle(bundle);
		cables[key] = cable;
		// bundle.addFunction({ cable.pending = false });
	}
	disconnectCable { arg cable;
		var key;
		key = cable.outlet -> cable.inlet;
		removing = removing.add( cable );
		cable.pending = true;
		cables.removeAt(key);
		// bundle.addFuction({ if(cables[key] === cable,{ cables.removeAt(key) }) })
	}
	autoCables {
		// patch all audio units to the mixer at bottom of channel
	}
	busForUnit { arg unit;
		if(busses.notNil,{
			^busses[unit]
		},{
			^nil
		})
	}
	update {
		var b;
		b = 	MixedBundle.new;
		// sort removing
		// removing.do({ arg r;
		
	}
	/*
	loadDefFileToBundle { arg bundle,server;
		this.units.keysValuesDo({ |p,unit|
			unit.loadDefFileToBundle(bundle,server)
		})	
		
	}(bundle,server);
		this.makePatchOut(group,private,bus,bundle);
		this.makeResourcesToBundle(bundle);
		this.prepareChildrenToBundle(bundle);

		this.loadBuffersToBundle(bundle);
	
	prepareToBundle { arg group,bundle,private,bus;
		unit should know its own group
		make busses
		
		unitGroups = Dictionary.new;
		 this.units.keysValuesDo({ arg p,unit;
			 var g;
			 if(unit.isActive,{
				 groups[p] = g = Group.basicNew;
				 bundle.add( g.newMsg(group,\addToTail) );
			 });
			 unit.prepareToBundle
	*/
	/*makeBundle { arg bundle;
		^bundle ?? {if(this.isPlaying,{MixedBundle.new},{EagerBundle.new})}
	}*/
}



