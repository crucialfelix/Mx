

Mx : AbstractPlayerProxy {

	classvar <>defaultFrameRate=24;

	var <channels, <cables;
	var <myUnit, <inlets, <outlets;

	var <>autoCable=true;

	var allocator, register, unitGroups, busses;
	var <master;
	var removing, adding, cableEndpoints;
	var <>frameRate=24, ticker, frameRateDevices, preFrameRateDevices;

	*new { arg channels, cables, inlets, outlets;
		^super.new.init(channels,cables,inlets,outlets)
	}
	storeArgs {
		^[
			channels.collect({ arg channel;
				this.findID(channel) -> channel.units
					.collect({ arg unit; unit !? {this.findID(unit) -> unit.saveData} })
			}),
			channels.collect({ arg channel;
				this.findID(channel) -> channel.fader.storeArgs
			}),
			MxIDDataSource.saveIOlets(register,this),
			cables.collect({ arg cable;
					[this.findID(cable.outlet),this.findID(cable.inlet),cable.mapping,cable.active]
				}),
			inlets.collect({ arg out;
					[this.findID(out), out.name,out.spec ] // , spec, name,
				}),
			outlets.collect({ arg out;
					[this.findID(out), out.name,out.spec ] // , spec, name,
				})
		]
	}
	init { arg argChannels,faderData,ioids,argCables,ins,outs;
		var faderMap,ds,uid;

		register = IdentityDictionary.new;
		allocator = NodeIDAllocator(0,0);

		ds = MxIDDataSource(ioids);

		uid = this.register(this);

		faderMap = Dictionary.new;
		faderData.do { arg ass;
			faderMap[ass.key] = ass.value
		};
		channels = [];
		argChannels.do { arg ass;
			var cuid,unitsData,chan,units;
			cuid = ass.key;
			unitsData = ass.value;
			units = unitsData.collect { arg uass;
				var unitid,unitData, unit;
				if(uass.isNil,{
					nil
				},{
					unitid = uass.key;
					unitData = uass.value;
					unit = MxUnit.loadData(unitData);
					this.registerUnit(unit,unitid,ds);
					unit
				});
			};

			chan = MxChannel(units,faderMap[cuid]);
			this.registerChannel(chan,cuid,ds);

			channels = channels.add(chan)
		};
		master = channels.last ?? {
			master = this.prMakeChannel;
			this.registerChannel(master);
			channels = channels.add(master);
			master
		};
		source = master;

		inlets = [];
		(ins ? []).do { arg inletData,i;
			var in;
			// no adapter yet
			in =  MxInlet(inletData[1],i,inletData[2],nil);
			inlets = inlets.add(in);
			this.register(in,ds.getInletID(uid,in.name) ?? {this.nextID});
		};
		outlets = [];
		if(outs.notNil,{
			outs.do { arg outletData,i;
				var in;
				// no adapter yet
				in =  MxOutlet(outletData[1],i,outletData[2],nil);
				outlets = outlets.add(in);
				this.register(in,ds.getOutletID(uid,in.name) ?? {this.nextID});
			}
		},{
			this.addOutput;
		});

		cables = MxCableCollection.new;
		argCables.do { arg cableData;
			var oid,iid,mapping,active;
			# oid,iid,mapping,active = cableData;
			// iid is nil
			cables.add( MxCable(this.atID(oid),this.atID(iid),mapping,active) );
		};
	}
	nextID {
		^allocator.alloc
	}
	// registerIOlet
	register { arg object,uid;
		uid = uid ?? { this.nextID };
		register[uid] = object;
		^uid
	}
	atID { arg uid;
		^register[uid]
	}
	findID { arg object;
		^register.findKeyForValue(object)
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
	registerUnit { arg unit,uid,ds;
		uid = this.register(unit,uid);
		unit.inlets.do { arg inlet;
			var iid;
			if(ds.notNil,{
				iid = ds.getInletID(uid,inlet.name) ?? {this.nextID};
			},{
				iid = this.nextID
			});
			this.register(inlet,iid);
		};
		unit.outlets.do { arg outlet;
			var iid;
			if(ds.notNil,{
				iid = ds.getOutletID(uid,outlet.name) ?? {this.nextID};
			},{
				iid  = this.nextID
			});
			this.register(outlet,iid);
		};
		unit.handlers.use {
			~frameRateDevices.value.do { arg dev;
				this.addFrameRateDevice(dev)
			}
		};
	}
	registerChannel { arg chan,uid,ds;
		uid = this.register(chan,uid);
		chan.myUnit.inlets.do { arg inlet;
			var iid;
			if(ds.isNil,{
				iid = this.nextID
			},{
				iid = ds.getInletID(uid,inlet.name) ?? {this.nextID};
			});
			this.register(inlet,iid);
		};
		chan.myUnit.outlets.do { arg outlet;
			var iid;
			if(ds.isNil,{
				iid = this.nextID
			},{
				iid = ds.getOutletID(uid,outlet.name) ?? {this.nextID};
			});
			this.register(outlet,iid);
		};
	}

	add { arg ... objects;
		^this.insertChannel(channels.size,objects)
	}
	extendChannels { arg forIndex;
		// create more channels if needed
		// such that there is a channel at forIndex
		// and there is still the master channel after that
		var prior,nuchan,start,stop;
		start = channels.size - 1;
		stop = forIndex;
		if(stop >= start,{
			for(stop,start,{ arg i;
				nuchan = this.prMakeChannel;
				channels = channels.insert(i,nuchan);
				this.registerChannel(nuchan);
				adding = adding.add(nuchan);
				nuchan.pending = true;
			});
		});
	}
	insertChannel { arg index, objects;
		var chan,units,nuchan,prior;
		units = (objects ? []).collect({ arg obj; obj !? {MxUnit.make(obj)}});
		units.do { arg unit;
			if(unit.notNil,{
				this.registerUnit(unit)
			});
		};
		this.extendChannels(index);

		// now inserting it
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
	removeChannel { arg index;
		var chan;
		chan = channels.removeAt(index);
		chan.pending = true;
		removing = removing.add( chan );
		// cut any cables going to/from any of those units
		chan.units.do { arg unit;
			cables.fromUnit.do(this.disconnectCable(_));
			cables.toUnit.do(this.disconnectCable(_));
		};
		//if(autoCable,{
		//	this.updateAutoCables
		//});
		this.changed('grid');
	}
	prMakeChannel { arg units;
		var chan,cableTo;
		if(master.notNil,{
			cableTo = this.findID(master.myUnit)
		});
		chan = MxChannel(units ? [],cableTo:cableTo);
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
		});
		channel.put(index,unit);
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
		var clock;
		clock = BeatSched.new;
		ticker = Task({
					loop {
						frameRateDevices.do { arg frd;
							frd.tick(clock.beat);
						};
						frameRate.reciprocal.wait;
					}
				},clock.tempoClock);
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
	mute { arg channel,boo=true;
		channels[channel].mute = boo
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
		channels.do(_.spawnToBundle(bundle));
		super.spawnToBundle(bundle);
		this.spawnCablesToBundle(bundle);
		bundle.addFunction({
			adding = removing = nil;
		});
		if(frameRateDevices.notNil,{
			frameRateDevices.do { arg frd;
				frd.tick(0); // node don't exist yet
			};
			this.startTicker(bundle)
		})
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
		})
	}
	guiClass { ^MxGui }

	draw { arg pen,bounds,style;
		master.draw(pen,bounds,style)
	}
}


MxIDDataSource {

	// utility class for use during Mx load
	var inlets,outlets;

	*new { arg ioids;
		^super.new.init(ioids)
	}

	getInletID { arg parentID,name;
		inlets[parentID].do { arg keyuid;
			if(keyuid.key == name,{
				^keyuid.value
			})
		};
		^nil
	}
	getOutletID { arg parentID,name;
		outlets[parentID].do { arg keyuid;
			if(keyuid.key == name,{
				^keyuid.value
			})
		};
		^nil
	}

	init { arg ioids;
		inlets = Dictionary.new;
		outlets = Dictionary.new;
		ioids.do { arg uidio;
			inlets.put(uidio.key,uidio.value[0]);
			outlets.put(uidio.key,uidio.value[1]);
		};
	}
	*saveIOlets { arg register,mx;
		/* [
			unitid -> [ [ \freq -> inletid, ...], [ \out -> inletid, ... ] ],
			unitid -> [ [ \amp -> outletid, ...], [ \out -> outletid, ... ] ],
			..
		]
		the constructor loads from this data
		*/
		var data,ret;
		data = Dictionary.new;
		register.keysValuesDo { arg uid,object;
			var slot,unitid;
			if(object.isKindOf(MxInlet),{
				// some have nils
				if(object.unit.source.isKindOf(MxChannel),{
					unitid = mx.findID(object.unit.source)
				},{
					unitid = mx.findID(object.unit);
				});
				data[unitid] ?? { data[unitid] = [ [],[] ] };
				slot = if(object.isKindOf(MxOutlet),1,0);
				data[unitid][slot] = data[unitid][slot].add( object.name -> uid );
			//},{
				// only the Mx itself does not save its id, its always 1
				// channels are known by the fader strip data array
				// units are known by the channel unit array

			})
		};
		ret = Array(data.size);
		data.keysValuesDo { arg uid,dd;
			ret.add( uid -> dd )
		};
		^ret
	}
}


