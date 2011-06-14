

MxCable {
	
	classvar strategies;
	
	var <>outlet,<>inlet,<>mapping,<>active=true,<>pending=false;
	
	*new { arg outlet,inlet,mapping;
		^super.newCopyArgs(outlet,inlet,mapping)
	}
	*register { arg outAdapterClassName,inAdapterClassName, strategy;
		strategies[ outAdapterClassName -> inAdapterClassName] = strategy
	}
	*initClass {
		strategies = Dictionary.new;

		this.register(\MxPlaysOnBus,\MxHasJack,
			MxCableStrategy({ arg cable,bundle;
				bundle.addFunction({
					var bus;
					bus = cable.outlet.adapter.value;
					cable.inlet.adapter.value.value = bus;
				})
			},{ arg cable,bundle;
				bundle.addFunction({
					// um, listen to a dead channel ?
					// what if a connection is in the same bundle ?
					cable.inlet.adapter.value = 127
				})
			}));
		
		
	}
	strategy {
		^strategies[outlet.adapter.class.name -> inlet.adapter.class.name] ?? {
			Error("No MXCable connection strategy found for" + outlet.adapter + "=>" + inlet.adapter ).throw
		}
	}
	spawnToBundle { arg bundle;
		this.strategy.connect(this,bundle)
	}
	stopToBundle { arg bundle;
		this.strategy.disconnect(this,bundle)
	}
	*spawnEndpointsToBundle { arg bundle,pool;
		// pool[ inletID ] = [outlet, outlet, ...]	
		
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


MxArCableEndpoint : AbstractPlayerProxy {

	// mixes one or more busses onto a target bus

	var <>cables;

//	*new { arg numChannels=2;
//		^super.new(\audio,numChannels).makeSource
//	}
//	prepareToBundle { arg agroup, bundle, private, bus;
//		
//	makeSource {
//		source = 
		
	*instr { arg numChannels=2;
			^Instr("MxArCableJackpoint" + numChannels,{ arg busses,bussesNumChannels;
						var in;
						in = Mix.ar( busses.collect({ arg b,i; NumChannels.ar(In.ar(b,numChannels),bussesNumChannels[i]) }) );
						NumChannels.ar(in,numChannels)
					},[
				    		StaticIntegerSpec(1,128),
						ArraySpec(ControlSpec(0,4096,'linear',1,0,"Bus"),nil),
						ArraySpec(StaticIntegerSpec(1,128),nil),
					],AudioSpec(numChannels))
	}
}
		

