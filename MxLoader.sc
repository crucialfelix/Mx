
MxLoader {
	
	var f,registerData;
	var <registery,<channels,<master,<cables,<inlets,<outlets;
	
	/*  SAVE   */
	*saveData { arg mx,register;
		
		var f,registerData,cables;
		f = IdentityDictionary.new;
		
		f[MxUnit] = { arg unit;
			[	MxUnit,
				unit.saveData,
				unit.inlets.collect(mx.findID(_)),
				unit.outlets.collect(mx.findID(_))]
		};
		f[MxChannel] = { arg channel;
			[	MxChannel,
				channel.units.collect({ arg unit; unit !? {mx.findID(unit)}}),
				channel.inlets.collect(mx.findID(_)),
				channel.outlets.collect(mx.findID(_)),
				channel.fader.storeArgs
			]
		};
		// not really needed to save these
		/*f[MxInlet] = { arg inlet;
			[ 	MxInlet, inlet.name ]
		};
		f[MxOutlet] = { arg outlet;
			[ 	MxOutlet, outlet.name ]
		};*/
		f[Mx] = { arg mx;
			[ Mx ,
				mx.inlets.collect({ arg i; [mx.findID(i),i.name,i.spec] }),
				mx.outlets.collect({ arg o; [mx.findID(o),o.name,o.spec] }),
				mx.channels.collect(mx.findID(_)),
				mx.findID(mx.master)
			]
		};
		registerData = Array.new(register.size);
		register.keysValuesDo({ arg id,object;
			var data;
			data = f[object.class].value(object);
			if(data.notNil,{
				registerData.add( id -> data )
			})
		});
		// should just register cables
		cables = mx.cables.collect { arg cable;
				[mx.findID(cable.outlet),mx.findID(cable.inlet),cable.mapping,cable.active]
		};
		^[registerData,cables]
	}

	/*  LOAD   */
	*new { arg register;
		^super.new.init(register)
	}
	loadData { arg data;
		this.initForLoad(data[0]);
		// better: just find the Mx and load that
		// the rest comes with it
		registerData.keysValuesDo { arg id,data;
			var object;
			if(this.registery[id].isNil,{
				object = this.get(id);
				this.register(object,id);
			})
		};
		cables = data[1].collect { arg data;
			var oid,iid, mapping,active;
			# oid,iid, mapping,active = data;
			MxCable( this.get(oid), this.get(iid),mapping,active)
		}
	}

	init { arg mxRegister;
		registery = mxRegister
	}
	initForLoad { arg rd;
		// associations to dict
		registerData = IdentityDictionary.new;
		rd.do(registerData.add(_));

		// load functions
		f = IdentityDictionary.new;
		f[Mx] = { arg uid,data;
			var ins,outs,chans,mast;
			# ins,outs,chans,mast = data;
			channels = chans.collect({ arg cid; this.get(cid) });
			master = this.get(mast);
			inlets = ins.collect({ arg d,i;
						var id,name,spec,io;
						# id, name, spec = d;
						io = MxInlet(name,i,spec);
						this.register(io,id);
						io
					});
			outlets = outs.collect({ arg d,i;
						var id,name,spec,io;
						# id, name, spec = d;
						io = MxOutlet(name,i,spec);
						this.register(io,id);
						io
					});
			nil
		};
		f[MxChannel] = { arg uid,data;
			var channel,units,unitIDs,inletIDs,outletIDs,faderArgs;
			# unitIDs,inletIDs,outletIDs,faderArgs = data;
			units = unitIDs.collect({ arg id; id !? {this.get(id)}});
			channel = MxChannel(units,faderArgs);
			channel.inlets.do { arg in,i;
				this.register(in,inletIDs[i])
			};
			channel.outlets.do { arg in,i;
				this.register(in,outletIDs[i])
			};
			this.register(channel,uid);
			channel			
		};
		f[MxUnit] = { arg uid,data;
			var unit, saveData,inletIDs,outletIDs;
			# saveData,inletIDs,outletIDs = data;
			unit = MxUnit.loadData(saveData);
			unit.inlets.do { arg in,i;
				this.register(in,inletIDs[i])
			};
			unit.outlets.do { arg out,i;
				this.register(out,outletIDs[i])
			};
			this.register(unit,uid);
			unit
		};
	}
	get { arg id;
		var klass,data,obj;
		^registery[id] ?? {
			klass = registerData[id][0];
			data = registerData[id].copyToEnd(1);
			obj = f[klass].value(id,data);
			if(obj.notNil,{
				registery[id] = obj;
			});
			obj
		}
	}
	register { arg object,id;
		registery[id] = object
	}

	maxID {
		var max=0;
		registery.keysDo { arg id;
			max = max(max,id?0)
		}
		^max
	}
}
