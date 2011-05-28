

Mx : AbstractPlayer {
	
	var <channels,<cables;

	var allocator,unitGroups,busses;
	
	*new { arg channels, cables;
		^super.new.init(channels,cables)
	}
	init { arg chans,cabs;
		channels = chans ?? {Array.new(8)};
		cables = cabs ? [];
		allocator = NodeIDAllocator(0,1);
	}
	nextID {
		^allocator.alloc
	}
	
	addChannel { arg ... sources;
		var chan;
		chan = MxChannel(sources);
		channels = channels.add(chan);
		// am I playing ?
		^chan
	}
	// insertChannel
	// removeChannel
	
	/*
	put { arg point,source;
		var unit;
		unit = MxUnit.make(source);
		if(units.at(point).notNil,{
			// inherit any cables you can
			units.at(point).remove;
		});
		units[point] = unit;
	}
	at { arg point;
		^units[point].source
	}
	colsDo { arg func;
		var col,ci;
		col = nil;
		units.do({ arg un;
			if(ci != un.point.x,{
				if(col.notNil,{
					func.value(col,ci);
				});
				col = [];
				ci = un.point.x;
			});
			col = col.add( un );
		});
		if(col.notNil,{
			func.value(col,ci);
		});
	}
	*/
	
	// outletID, inletID		
	connect { arg outlet,inlet,mapping=nil,bundle=nil;
		var oldcable,cable,key,doitnow=false;
		bundle = this.makeBundle(bundle);
		key = outlet -> inlet;
		// remove any that goes to this inlet
		cables.keysValuesDo({ arg k,oldcable;
			if(k.value === inlet,{
				if(oldcable.active,{
					this.disconnectCable(oldcable,bundle);
				});
			})
		});
		cable = MxCable( outlet, inlet, mapping );
		// play to head of inlet.unit
		// cable should not create a bus 
		cable.prepareToBundle(inlet.unit.group,bundle,bus:this.busForUnit(inlet.unit));
		cable.spawnToBundle(bundle);
		cables[key] = cable;
		bundle.addFunction({ cable.pending = false });
	}
	disconnectCable { arg cable,bundle;
		var key;
		key = cable.outlet -> cable.inlet;
		cable.freeToBundle(this.makeBundle(bundle));
		cable.pending = true;
		bundle.addFuction({ if(cables[key] === cable,{ cables.removeAt(key) }) })
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
	makeBundle { arg bundle;
		^bundle ?? {if(this.isPlaying,{MixedBundle.new},{EagerBundle.new})}
	}
}


MxChannel {
	
	var <units,<>level=0,<>mute=false,<>solo=false,<>mixToMaster=true;
	
	
}



