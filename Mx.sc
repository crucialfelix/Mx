

Mx : AbstractPlayerProxy {
	
	var <channels,<cables;
	var inlets,outlets;
	
	var allocator,register,unitGroups,busses;
	var master;
	var removing,adding, cableEndpoints;
	
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
		};
		register = IdentityDictionary.new;
		source = MxChannel(this.nextID,nil,[]);
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
		// problem: need to sort the adding I think
		// maybe not. as long as cables happen after units its fine
		adding = adding.add(unit);
		channel.put(unit);
	}
	
	// API
	findInlet { arg point,index;
		var unit;
		unit = channels[point.x].units[point.y];
		^unit.inlets[index]
	}
	findOutlet { arg point,index;
		var unit;
		unit = channels[point.x].units[point.y];
		^unit.outlets[index]
	}
		
	// outletID, inletID		
	connect { arg outlet,inlet,mapping=nil;
		var oldcable,cable,key;
		key = outlet -> inlet;
		// remove any that goes to this inlet
		// but audio points are supposed to mix ?
		// only the mixer heads
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
	
	//adding / removing to get to current state
	addAutoCables {
		var patched,autos;
		patched = IdentitySet.new;
		autos = [];
		cables.do { arg c;
			patched.add( c.outlet.unit );
		};
		channels.do { arg chan;
			chan.units.do { arg unit;
				if(patched.includes(unit).not,{
					autos = autos.add( MxAutoCable(	unit.outlets.first, chan.inlet ) );
				})
			}
		};
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
		// adding
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
	spawnCablesToBundle { arg bundle;
		var pool;
		pool = Dictionary.new;
		cables.do(_.spawnToBundle(bundle),pool);
		
		// pool[inlet] = [outlet,outlet,...]
		// spawn the mixers
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



