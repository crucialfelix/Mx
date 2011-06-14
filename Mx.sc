

Mx : AbstractPlayerProxy {
	
	var <channels,<cables;
	var inlets,outlets;
	
	var allocator,register,unitGroups,busses;
	var master;
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
		source = MxChannel(this.nextID,nil,[]);
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
		unit = MxUnit.make(object,this);
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
		
	connect { arg outlet,inlet,mapping=nil;
		var oldcable,cable,key;
		key = outlet -> inlet;
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
	
	autoCables { arg bundle;
		/* needs to be run as an update:
			remove/create any to get to current patch state */
		var patched,autoCabled,mixers;
		
		// current mixers
		mixers = [];
		channels.do { arg chan;
			if(chan.inletMixer.notNil,{
				mixers.add(chan.inletMixer)
			})
		};
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
			var newautos;
			chan.units.do { arg unit;
				// should be auto cabled, and isn't already
				if(patched.includes(unit).not and: {autoCabled.includes(unit).not} ,{
					newautos = newautos.add( unit );
					autoCabled.remove( unit );
				});
			};
			if(newautos.size > 0,{
				if(chan.inletMixer.isNil,{
					chan.inletMixer = MxUnit.make( MxArCableEndpoint.new, this );
					chan.inletMixer.spawnToBundle(bundle);
				});
				mixers.remove(chan.inletMixer);
				newautos.do { arg unit;
					var ac;
					ac = MxAutoCable( unit.outlets.first, chan.inletMixer.inlets.first );
					autoCables = autoCables.add( ac );
					ac.spawnToBundle(bundle);
				};
			});
			// TODO patch channel to master
			
		};
		// these are left from a previous patch-state and can be removed now
		autoCabled.do { arg ac;
			ac.stopToBundle(bundle)
		};
		mixers.do { arg mix;
			mix.freeToBundle(bundle);
		};
	}

	update { arg bundle;
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
		this.autoCables(bundle);
		
		// pool[inlet] = [outlet,outlet,...]
		// spawn the mixers
	}

	/*makeBundle { arg bundle;
		^bundle ?? {if(this.isPlaying,{MixedBundle.new},{EagerBundle.new})}
	}*/
}



