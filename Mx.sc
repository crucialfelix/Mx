

Mx : AbstractPlayerProxy {
	
	var <channels,<cables;
	var myUnit,<inlets,<outlets;
	
	var <>autoCable=true;
	
	var allocator,register,unitGroups,busses;
	var <master;
	var removing,adding, cableEndpoints;
	
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

		cables = MxCableCollection.newFrom((cabs ? []).collect(MxCable.loadData(_,this)));
		//this.updateAutoCables;
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
		units = (objects ? []).collect({ arg obj; obj !? {MxUnit.make(obj,this)}});
		chan = this.prMakeChannel(units);
		if(channels.size < (index+1),{
			channels = channels.extend(index,this.prMakeChannel);
			channels = channels.add(chan)
		},{
			channels.put(index,chan);
		});
		if(autoCable,{
			this.updateAutoCables
		});
		this.changed('grid');
		chan.pending = true;
		adding = adding.add(chan);
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
		var chan;
		chan = MxChannel(units ? [],cableTo:master.uid);
		chan.makeUnit(this).registerWithMx(this);
		^chan
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
		var channel,unit,old;
		channel = channels[chan] ?? {
			this.insertChannel(chan, Array.fill(index,nil) ++ [object]);
			^this
		};
		unit = MxUnit.make(object,this);
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
		var moving,unit;
		moving = this.at(chan,index) ?? {^this};
		if(chan != toChan) {
			unit = channels[chan].removeAt(index);
			cables.fromUnit(moving).do { arg cable;
				if(cable.inlet.unit.source.isKindOf(MxChannel),{
					this.disconnectCable(cable);
				})
			};
			this.put(toChan,toIndex, moving);
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
	/* playAt { arg chan,index;
		var unit;
		unit = this.at(chan,index);
		if(unit.isPrepared.not,{
			unit.play( MxChannel )
		})
	} */
		
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
			// this.autoCablesToBundle(b);
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
		//this.autoCablesToBundle(bundle);
	}
//	autoCablesToBundle { arg bundle;
//		/* updates autoCables, adding and removing to get to current patch state */
//		var adds,rms;
//		adds = this.updateAutoCables;
//		adds.do { arg a;
//			a.spawnToBundle(bundle)
//		};
//		
//		/*rms.do { arg r;
//			r.stopToBundle(bundle)
//			//r.freeToBundle(bundle)
//		}*/
//	}
	updateAutoCables {
		// the expensive way: scan the whole thing
		// and check for integrity

		var patched,autoCabled,ac,changed=false;
		var addingCables,removingCables;
		
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
		
		^addingCables
	}
	
	guiClass { ^MxGui }
}



