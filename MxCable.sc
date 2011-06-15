

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
			Error("No MxCableStrategy found for" + outlet.adapter + "=>" + inlet.adapter ).throw
		}
	}
	spawnToBundle { arg bundle;
		this.strategy.connect(this,bundle)
	}
	stopToBundle { arg bundle;
		this.strategy.disconnect(this,bundle)
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


MxArCableEndpoint : PlayerSocket {

	// mixes one or more busses onto a target bus
	var patch,<>busses,<>bussesNumChannels;
	
	*new { arg numBusses,numChannels=2;
		^super.new(\audio,numChannels).maceinit(numBusses)
	}
	maceinit { arg numBusses;
		busses = Array.fill(numBusses,{128-numChannels}};
		bussesNumChannels Array.fill(numBusses,numChannels);
		this.makePatch;
	}
	makePatch {
		patch = Patch(MxArCableEndpoint.instr,[
						this.numChannels,
						busses,
						bussesNumChannels
					])
		})
	}						
		
	*instr { 
		^Instr("MxArCableJackpoint",{ arg numChannels, busses,bussesNumChannels;
					var in;
					if(busses.size,{
						in = Mix.ar( busses.collect { arg b,i; NumChannels.ar(In.ar(b,bussesNumChannels[i]),numChannels) } );
					},{
						in = Silent.ar(numChannels)
					});
					in
				},[
			    		StaticIntegerSpec(1,128),
					ArraySpec(ControlSpec(0,4096,'linear',1,0,"Bus"),nil),
					ArraySpec(StaticIntegerSpec(1,128),nil),
				],AudioSpec(nil))
	}
}
		

