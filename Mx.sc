

Mx : AbstractPlayerProxy {

	classvar <>defaultFrameRate=24;

	var <channels, <cables;
	var <myUnit, <inlets, <outlets;

	var <>autoCable=true;

	var allocator=0, register, unitGroups, busses;
	var <master;
	var removing, adding, cableEndpoints;
	var <>frameRate=24, sched, ticker, <position, frameRateDevices, preFrameRateDevices;

	*new { arg data;
		^super.new.init(data)
	}
	storeArgs {
		^[MxLoader.saveData(this,register)];
	}
	init { arg data;
		var loader;
		
		register = IdentityDictionary.new;
		cables = MxCableCollection.new;
		
		if(data.isNil,{
			this.register(this,0);
			master = this.prMakeChannel;
			this.registerChannel(master);
			channels = [master];
			inlets = [];
			this.addOutput;
		},{
			loader = MxLoader(register);
			loader.loadData(data);
			this.register(this,0);
			allocator = loader.maxID + 1;
			channels = loader.channels;
			master = loader.master;
			loader.cables.do(cables.add(_));

			inlets = loader.inlets;
			outlets = loader.outlets;
			
			this.allUnits.do { arg unit;
				this.unitAddFrameRateDevices(unit)
			};
		});

		source = master;
		sched = OSCSched.new;
		position = Position.new;
	}
			
	nextID {
		^allocator = allocator + 1;
	}
	register { arg object,uid;
		if(uid.isNil,{
			uid = this.nextID;
		});
		register[uid] = object;
		^uid
	}
	atID { arg uid;
		^register[uid]
	}
	findID { arg object;
		^register.findKeyForValue(object) ?? { Error("ID not found in registery for:"+object).throw }
	}
	unregister { arg uid;
		var item;
		item = register.removeAt(uid);
		if(item.isKindOf(MxChannel),{
			item.units.do { arg u;
				this.unregister(this.findID(u))
			};
			this.unregister(this.findID(item.myUnit));
		},{
			if(item.isKindOf(MxUnit),{
				item.inlets.do({ arg in; this.unregister(this.findID(in)) });
				item.outlets.do({ arg in; this.unregister(this.findID(in)) });
			})
		})
	}
	registerUnit { arg unit,uid;
		uid = this.register(unit,uid);
		unit.inlets.do { arg inlet;
			this.register(inlet); // already registered ?
		};
		unit.outlets.do { arg outlet;
			this.register(outlet); // already registered ?
		};
		this.unitAddFrameRateDevices(unit)
	}
	unitAddFrameRateDevices { arg unit;
		unit.handlers.use {
			~frameRateDevices.value.do { arg dev;
				this.addFrameRateDevice(dev)
			}
		};
	}
	registerChannel { arg chan,uid;
		uid = this.register(chan,uid);
		chan.myUnit.inlets.do { arg inlet;
			this.register(inlet);
		};
		chan.myUnit.outlets.do { arg outlet;
			this.register(outlet);
		};
	}

	add { arg ... objects;
		^this.insertChannel(channels.size-1,objects)
	}
	extendChannels { arg forIndex;
		// create more channels if needed
		// such that there is a channel at forIndex
		// and there is still the master channel after that
		var prior,nuchan,start,stop;
		start = channels.size - 1;// last non-master channel
		stop = forIndex; // where we want to insert
		if(stop >= start,{
			for(start,stop,{ arg i;
				nuchan = this.prMakeChannel;
				channels = channels.insert(i,nuchan);
				this.registerChannel(nuchan);
				adding = adding.add(nuchan);
				nuchan.pending = true;
			});
		});
	}
	
	insertChannel { arg index, objects;
		// make sure at least that many channels
		var chan,units;
		if( (channels[index].isNil or: {channels[index] === master}).not,{ // not creating a new channel
			this.extendChannels(index-1);
		});
		units = (objects ? []).collect({ arg obj; obj !? {MxUnit.make(obj)}});
		units.do { arg unit;
			if(unit.notNil,{
				this.registerUnit(unit)
			});
		};
		chan = this.prMakeChannel(units);
		this.registerChannel(chan);
		channels = channels.insert(index,chan);

		chan.pending = true;
		adding = adding.add(chan);
		if(autoCable,{
			this.updateAutoCables
		});
		this.changed('grid');
		^chan
	}
			
	putChannel { arg index,objects;
		var prev,chan;
		prev = channels[index];
		if(prev.notNil,{
			this.prRemoveChannel(index);
		});	
		^this.insertChannel(index,objects)
	}
	removeChannel { arg index;
		this.prRemoveChannel(index);
		this.changed('grid'); // this is why app should be separate
	}
	prRemoveChannel { arg index;
		var chan;
		chan = channels.removeAt(index);
		chan.pending = true;
		removing = removing.add( chan );
		// cut any cables going to/from any of those units
		chan.units.do { arg unit;
			cables.fromUnit.do(this.disconnectCable(_));
			cables.toUnit.do(this.disconnectCable(_));
		};
	}
	prMakeChannel { arg units;
		var chan;
		chan = MxChannel(units ? []);
		^chan
	}

	addOutput { arg rate='audio',numChannels=2;
		// not finished
		// just creates a default out for now, coming from the master

		// change this to keep the master as just a normal channel on grid
		// created by .mixer
		// the MxOutlet can specify how to find that
		// and master will be just for the player to use as its out
		// only trick then is to make addChannel insert before the output channels
		// add audio output
		var chan,out;

		// master output
		if(outlets.isNil or: {outlets.isEmpty},{
			// do not like. this is not in anything and should be created in the same way
			// as other outlets/inlets via make: mx.myUnit
			// otoh outlets is an array here and make would read that
			// and its variable
			chan = master;
			out = MxOutlet(\out,outlets.size,'audio'.asSpec,MxPlaysOnBus({chan.bus}));
			out.unit = this;
			outlets = outlets.add(out);
			this.register(out);
		});

		^chan
	}
	at { arg chan,index;
		^(channels.at(chan) ? []).at(index)
	}
	put { arg chan,index,object;
		var channel,unit,old;
		// TODO how do you insert into master then ??
		if(channels[chan].isNil or: {channels[chan] === master},{
			this.insertChannel(chan, Array.fill(index,nil) ++ [object]);
			^this
		});
		channel = channels[chan];
		unit = MxUnit.make(object);
		if(unit.notNil,{ // nil object is nil unit which is legal
			this.registerUnit(unit);
		});
		old = channel.at(index);
		if(old.notNil,{
			// cut or take any cables
			this.disconnectUnit(old);
			this.unregister(this.findID(old));
		});
		channel.put(index, unit);
		this.changed('grid');
		^unit
	}
	move { arg chan,index,toChan,toIndex;
		var moving,unit,unitg;
		moving = this.at(chan,index) ?? {^this};
		if(chan != toChan) {

			# unit,unitg = channels[chan].extractAt(index);
			
			// could keep them connected, depends on order of execution
			cables.fromUnit(moving).do { arg cable;
				if(cable.inlet.unit.source.isKindOf(MxChannel),{
					this.disconnectCable(cable);
				})
			};
			
			// make channel if needed
			this.extendChannels(toChan);
			
			channels[toChan].insertAt(toIndex,unit,unitg);
			
			if(autoCable,{
				this.updateAutoCables;
			})
		} {
			// not yet checking if some cables need to be cut
			channels[chan].move(index,toIndex);
		};
		this.changed('grid');
	}
	remove { arg chan,index;
		var del;
		del = this.at(chan,index) ?? {^this};
		this.put(chan,index,nil);
		removing = removing.add(del);
		cables.toUnit(del).do { arg cab;
			if(cab.inlet.unit === del or: {cab.outlet.unit === del},{
				this.disconnectCable(cab)
			})
		};
		/*autoCables.do { arg cab;
			if(cab.inlet.unit === del or: {cab.outlet.unit === del},{
				this.disconnectCable(cab)
			})
		};*/
	}
	removeUnit { arg unit;
		channels.do { arg ch,ci;
			ch.units.do { arg u,ri;
				if(unit === u,{
					^this.remove(ci,ri)
				})
			}
		}
	}
	addFrameRateDevice { arg func;
		frameRateDevices = frameRateDevices.add( MxFrameRateDevice(func) );
		if(this.isPlaying and: {ticker.isNil},{
			this.startTicker
		});
	}
	startTicker { arg bundle;
		ticker = Task({
					loop {
						frameRateDevices.do { arg frd;
							frd.tick(sched.beat);
						};
						position.value = sched.beat;
						frameRate.reciprocal.wait;
					}
				},sched.tempoClock);
		if(bundle.notNil,{
			bundle.addFunction({
				ticker.play
			})
		},{
			ticker.play
		});
	}
	stopTicker { arg bundle;
		if(bundle.notNil,{
			bundle.addFunction({
				ticker.stop;
				ticker = nil;
			});
		},{
			ticker.stop;
			ticker = nil;
		})
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

		cables.toUnit(inlet.unit).do { arg oldcable;
			if(oldcable.inlet === inlet
					and: {oldcable.inlet.unit.source.isKindOf(MxChannel).not}
					and: {oldcable.active},{
				this.disconnectCable(oldcable);
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
		// should be in API
		if(outlet.isKindOf(MxOutlet).not,{
			outlet = this.getOutlet(fromUnit,outlet);
		});
		if(inlet.isKindOf(MxInlet).not,{
			inlet = this.getInlet(toUnit,inlet);
		});
		cables.do { arg cab;
			if(cab.inlet === inlet and: {cab.outlet === outlet},{
				^this.disconnectCable(cab)
			})
		};
		^nil
	}
	disconnectUnit { arg unit;
		cables.copy.do { arg cable;
			if(cable.inlet.unit === unit or: {cable.outlet.unit === unit},{
				this.disconnectCable(cable);
			})
		};
	}
	disconnectInlet { arg inlet;
		cables.toInlet(inlet).do { arg cable;
			this.disconnectCable(cable)
		}
	}
	disconnectOutlet { arg outlet;
		cables.fromOutlet(outlet).do { arg cable;
			this.disconnectCable(cable)
		}
	}
	mute { arg channel,boo;
		var chan;
		chan = this.channels.at(channel);
		if(chan.notNil,{
			boo = boo ? chan.fader.mute.not;
			chan.fader.mute = boo;
		});
	}
	solo { arg channel,boo;
		var chan;
		chan = this.channels.at(channel);
		if(chan.notNil,{
			boo = boo ? chan.fader.solo.not;
			if(boo,{
				chan.fader.setSolo;
				this.channels.do { arg ch;
					if(ch !== chan and: {ch !== master},{
						ch.fader.muteForSoloist;
					})
				};
			},{
				chan.fader.unsetSolo;
				this.channels.do { arg ch;
					if(ch !== chan and: {ch !== master},{
						ch.fader.mute = false;
					})
				};
			});
		});
	}
	gotoBeat { arg beat,q=4,bundle;
		var b,beats,atBeat;
		beat = beat.trunc(q);
		atBeat = sched.beat.roundUp(q);
		
		b = bundle ?? {MixedBundle.new};
		b.addFunction({ 
			sched.beat = beat;
			position.value = beat;
		});
		channels.do { arg chan;
			chan.units.do { arg unit;
				if(unit.notNil,{
					unit.gotoBeat(beat,atBeat,b)
				})
			}
		};
		if(this.isPlaying,{
			sched.schedAbs(atBeat,this.server,b.messages,{
				b.doSendFunctions;// should be before !
				b.doFunctions;
			});
		},{
			b.doFunctions.doSendFunctions
		})
	}
	// enact all changes on the server after things have been added/removed dis/connected
	// syncChanges
	update { arg bundle=nil;
		var b;
		if(this.isPlaying,{
			b = bundle ?? { MixedBundle.new };
			removing.do { arg r;
				r.freeToBundle(b);
			};
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
				removing.do { arg r;
					this.unregister(r);
				};
				removing = adding = nil
			});
			if(bundle.isNil,{
				b.send(this.server)
			});
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
		// master is source : one of the channels
		^channels // ++ cables
	}
	allUnits {
		^Routine({
			channels.do({ arg c;
				c.myUnit.yield;
				c.units.do({ arg u;
					if(u.notNil,{
						u.yield
					})
				})
			})
		});
	}
	loadDefFileToBundle { arg b,server;
		this.children.do(_.loadDefFileToBundle(b,server))
	}
	prepareChildrenToBundle { arg bundle;
		channels.do { arg c;
			if(c !== master,{
				c.prepareToBundle(group,bundle,true)
			})
		};
		master.prepareToBundle(group,bundle,false,this.bus);
	}
	spawnToBundle { arg bundle;
		// never sure that all defs havent been added, children prepared
		// some could have been added while play is stopped
		// cant prepare since dont have group yet
		
		// this.prepareChildrenToBundle(bundle);
		channels.do({ arg chan;
			if(chan !== master,{
				chan.spawnToBundle(bundle);
			});
		});
		super.spawnToBundle(bundle);
		
		this.spawnCablesToBundle(bundle);
		bundle.addFunction({
			adding = removing = nil;
			// starting from the start
			sched.time = 0.0;
			sched.beat = 0.0;
		});
		this.startTicker(bundle);
		
		//if(frameRateDevices.notNil,{
			// need some way to send initial value
			// but the inlets are only connected by a changed notification
			//frameRateDevices.do { arg frd;
			//	frd.tick(0); // node don't exist yet
			//};
		//})
	}

	spawnCablesToBundle { arg bundle;
		cables.do(_.spawnToBundle(bundle));
	}
	updateAutoCables {

		var patched,autoCabled,ac,changed=false;
		var addingCables=[],removingCables;

		channels.do { arg chan;
			chan.units.do { arg unit;
				// should be auto cabled, and isn't already
				if(unit.notNil and: {unit.spec.isKindOf(AudioSpec)},{
					if(cables.fromUnit(unit).any({ arg cable; cable.inlet.unit === chan.myUnit }).not,{
						ac = MxCable( unit.outlets.first, chan.myUnit.inlets.first );
						cables.add(ac);									addingCables = addingCables.add(ac);
						changed = true;
					})
				})
			};
			if(chan !== master,{
				// if the channel is not patched to anything then patch it to the master
				if(cables.fromUnit(chan.myUnit).isEmpty,{
					ac = MxCable( chan.myUnit.outlets.first, master.myUnit.inlets.first );
					cables.add(ac);
					addingCables = addingCables.add(ac);
					changed = true;
				})
			})
		};
		// patch units in master
		if(addingCables.notEmpty,{
			adding = adding.addAll(addingCables);
		});
		^addingCables
	}
	stopToBundle { arg bundle;
		super.stopToBundle(bundle);
		if(ticker.notNil,{
			this.stopTicker(bundle)
		});
	}
	guiClass { ^MxGui }

	draw { arg pen,bounds,style;
		master.draw(pen,bounds,style)
	}
}


			

