

MxCable {
	
	classvar strategies;
	
	var <>outlet,<>inlet,<>mapping,<>active=true,<>pending=false;
	var <state;
	
	*new { arg outlet,inlet,mapping,active=true;
		^super.newCopyArgs(outlet,inlet,mapping,active).init
	}
		
	init {
		state = Environment.new;
	}
	asString {
		^format("MxCable: % [%] -> % [%]",this.outlet.unit.source.class,this.outlet.adapter.class,
								this.inlet.unit.source.class,this.inlet.adapter.class)
	}
	strategy {
		^strategies[ [outlet.adapter.class.name, inlet.adapter.class.name] ] ?? {
			Error("No MxCableStrategy found for" + outlet + outlet.adapter + "=>" + inlet + inlet.adapter ).throw
		}
	}
	spawnToBundle { arg bundle;
		this.strategy.connect(this,bundle)
	}
	stopToBundle { arg bundle;
		this.strategy.disconnect(this,bundle)
	}
	freeToBundle { arg bundle;
		^this.stopToBundle(bundle)
	}
	map { arg v;
		// map an input value coming from the outlet to the range needed for the inlet
		// or map a ugen
		if(mapping.notNil,{
			^mapping.value(v)
		},{
			if(outlet.spec.isKindOf(ControlSpec) and: {inlet.spec.isKindOf(ControlSpec)} and: {outlet.spec != inlet.spec},{
				^inlet.spec.map( outlet.spec.unmap(v) )
			},{
				^v
			})
		})
	}

	*register { arg outAdapterClassName,inAdapterClassName, strategy;
		strategies[ [outAdapterClassName, inAdapterClassName] ] = strategy;
	}
	*initClass {
		strategies = Dictionary.new;

		Instr("MxCable.cableAr",{ arg inBus=126,outBus=126,inNumChannels=2,outNumChannels=2;
			Out.ar(outBus,
				NumChannels.ar( In.ar(inBus,inNumChannels), outNumChannels )
			)
		},[
			ControlSpec(0,127),
			ControlSpec(0,127),
			StaticIntegerSpec(1,128),
			StaticIntegerSpec(1,128)
		],\audio);
		
		this.register(\MxPlaysOnBus,\MxHasJack,
			MxCableStrategy({ arg cable,bundle;
				var bus, jack;
				bus = cable.outlet.adapter.value;
				jack = cable.inlet.adapter.value;
				jack.setValueToBundle( bus.index, bundle )
			},{ arg cable,bundle;
				var bus, jack;
				bus = cable.outlet.adapter.value;
				jack = cable.inlet.adapter.value;
				// temp: set it to silence
				// what if a new connection is in the same bundle ?
				// then order is important
				// disconnects are first
				jack.setValueToBundle( cable.inlet.adapter.server.options.numAudioBusChannels-2, bundle )
			})
		);

		this.register(\MxPlaysOnKrBus,\MxHasKrJack,
			MxCableStrategy({ arg cable,bundle;
				var bus, jack;
				bus = cable.outlet.adapter.value;
				jack = cable.inlet.adapter.value;
				// launch synth wire with cable mapping
				
				~cableKr = Patch({ arg in;
							cable.map(in)
						},[
							bus
						]);
				~cableGroup = Group.basicNew(cable.inlet.adapter.server);
				AbstractPlayer.annotate(~cableGroup,cable.asString);
				bundle.add( ~cableGroup.addToHeadMsg(cable.inlet.adapter.group) );

				~cableKr.prepareToBundle(~cableGroup,bundle);
				~cableKr.spawnToBundle(bundle);
				
				jack.readFromBusToBundle(~cableKr.bus,bundle);
				
			},{ arg cable,bundle;
				var bus, jack;
				bus = cable.outlet.adapter.value;
				jack = cable.inlet.adapter.value;
				~cableKr.freeToBundle(bundle);
				bundle.add( ~cableGroup.freeMsg );
				bundle.addFunction({
					~cableKr = nil;
					~cableGroup = nil;
				});
				jack.stopReadFromBusToBundle(bundle);
			})
		);
						
		this.register(\MxPlaysOnBus,\MxListensToBus,
			// depends on being on the same server
			MxCableStrategy({ arg cable,bundle;
				cable.state.use {
					var inbus,outbus,def,group;
					outbus = cable.inlet.adapter.value;
					inbus = cable.outlet.adapter.value;

					def = Instr("MxCable.cableAr").asSynthDef([
								inbus.index,
								outbus.index,
								inbus.numChannels,
								outbus.numChannels
							 ]);
					// loads if needed
					InstrSynthDef.loadDefFileToBundle(def,bundle,inbus.server);
							
					group = cable.inlet.adapter.group;
					~synth = Synth.basicNew(def.name,group.server);
					AbstractPlayer.annotate(~synth,cable.asString);
					bundle.add( ~synth.addToHeadMsg(group,[inbus.index,outbus.index]) );
				}
			},{ arg cable,bundle;
				var synth;
				synth = cable.state.at('synth');
				if(synth.notNil,{
					bundle.add( synth.freeMsg );
					bundle.addFunction({
						cable.state.removeAt('synth')
					})
				})
			})
		);			

		this.register(\MxHasAction,\MxHasKrJack,
			// always active, doesn't wait for play
			MxCableStrategy({ arg cable,bundle;
				var jack,action;
				jack = cable.inlet.adapter.value;
				// listener
				~nr = NotificationCenter.register( cable.outlet, \didAction, cable.inlet, 
							{ arg value;
								jack.value = cable.map(value)
							});

				// sender
				// this always takes over the action, assuming that a has-action is there to be taken over
				// and all strategies will set up to listen for the same notification.
				// adding more out cables means reinstalling an identical action, no harm done
				action = { arg val;
					NotificationCenter.notify(cable.outlet, \didAction, [ val ])
				};
				cable.outlet.adapter.value(action);
			},{ arg cable,bundle;
				bundle.addFunction({
					~nr.remove;
				}.inEnvir);
			})
		);
		
		this.register(\MxHasAction,\MxSetter,
			// always active, doesn't wait for play
			MxCableStrategy({ arg cable,bundle;
				var setter,action;
				setter = cable.inlet.adapter;
				// listener
				~nr = NotificationCenter.register( cable.outlet, \didAction, cable.inlet, 
							{ arg value;
								setter.value( cable.map(value) )
							});
				
				action = { arg val;
					NotificationCenter.notify(cable.outlet, \didAction, [ val ])
				};
				cable.outlet.adapter.value(action);
			},{ arg cable,bundle;
				bundle.addFunction({
					~nr.remove;
				}.inEnvir);
			})
		);
		
		this.register(\MxSendsValueOnChanged,\MxHasKrJack,
			MxCableStrategy({ arg cable,bundle;
				var model,ina;
				model = cable.outlet.adapter.value();
				ina = cable.inlet.adapter.value();
				bundle.addFunction({
					~updater = Updater(model,{ arg sender,value;
						ina.value = cable.map(value);
					});
				}.inEnvir);
			},{ arg cable,bundle;
				bundle.addFunction({
					~updater.remove
				}.inEnvir)
			})
		);
		
		this.register(\MxSendsValueOnChanged,\MxSetter,
			MxCableStrategy({ arg cable,bundle;
				var model,ina;
				model = cable.outlet.adapter.value();
				ina = cable.inlet.adapter;
				bundle.addFunction({
					~updater = Updater(model,{ arg sender,value;
						ina.value(cable.map(value))
					});
				}.inEnvir);
			},{ arg cable,bundle;
				bundle.addFunction({
					~updater.remove
				}.inEnvir);
			})
		);		
	}
}


MxAutoCable : MxCable {}


MxCableStrategy {
	
	var <>connectf,<>disconnectf;
	
	*new { arg connectf,disconnectf;
		^super.newCopyArgs(connectf,disconnectf)
	}
	connect { arg cable,bundle;
		try({
			cable.state.use {
				connectf.value(cable,bundle)
			}
		},{ arg exc;
			"".postln;
			"MxCableStrategy failed".error;
			cable.asString.postln;
			cable.outlet.dump;
			cable.outlet.unit.source.dump;
			"===>".postln;
			cable.inlet.dump;
			cable.inlet.unit.source.dump;
			exc.reportError;
			this.halt;
		})
	}
	disconnect { arg cable,bundle;
		cable.state.use {
			disconnectf.value(cable,bundle)
		}
	}
}
	

MxCableMapping {
	
	var <>mapToSpec,<>mapCurve,<>enabled=false;
	
}


