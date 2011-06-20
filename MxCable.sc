

MxCable {
	
	classvar strategies;
	
	var <>outlet,<>inlet,<>mapping,<>active=true,<>pending=false;
	var <state;
	
	*new { arg outlet,inlet,mapping;
		^super.newCopyArgs(outlet,inlet,mapping).init
	}
	init {
		state = ()
	}
	strategy {
		^strategies[ [outlet.adapter.class.name, inlet.adapter.class.name] ] ?? {
			Error("No MxCableStrategy found for" + outlet.adapter + "=>" + inlet.adapter ).throw
		}
	}
	spawnToBundle { arg bundle;
		this.strategy.connect(this,bundle)
	}
	stopToBundle { arg bundle;
		this.strategy.disconnect(this,bundle)
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
				// need it as a message in same bundle
				
				bundle.addFunction({
					var bus, jack;
					bus = cable.outlet.adapter.value;
					jack = cable.inlet.adapter.value;
					MxArJack
					cable.inlet.adapter.value.value = bus;
				})
			},{ arg cable,bundle;
				bundle.addFunction({
					// um, listen to a dead channel ?
					// what if a connection is in the same bundle ?
					cable.inlet.adapter.value.value = 127
				})
			}));
			
		this.register(\MxPlaysOnBus,\MxListensToBus,
			// depends on being on the same server
			MxCableStrategy({ arg cable,bundle;
				cable.state.use {
					var inbus,outbus,def,group;
					outbus = cable.inlet.adapter.value;
					inbus = cable.outlet.adapter.value;

					// cache these
					def = Instr("MxCable.cableAr").asSynthDef([
								inbus.index,
								outbus.index,
								inbus.numChannels,
								outbus.numChannels
							 ]);
					InstrSynthDef.loadDefFileToBundle(def,bundle,inbus.server);
							
					group = cable.inlet.adapter.getGroup.value;
					~synth = Synth.basicNew(def.name,group.server);
					bundle.add( ~synth.addToHeadMsg(group,[inbus.index,outbus.index]) );
				}
			},{ arg cable,bundle;
				var synth;
				synth = cable.state.removeAt('synth');
				bundle.add( synth.freeMsg )
			}));		
	}
}


MxAutoCable : MxCable {}


MxCableStrategy {
	
	var <>connectf,<>disconnectf;
	
	*new { arg connectf,disconnectf;
		^super.newCopyArgs(connectf,disconnectf)
	}
	connect { arg cable,bundle;
		connectf.value(cable,bundle)
	}
	disconnect { arg cable,bundle;
		disconnectf.value(cable,bundle)
	}
}
	

MxCableMapping {
	
	var <>mapToSpec,<>mapCurve,<>enabled=false;
	
}


