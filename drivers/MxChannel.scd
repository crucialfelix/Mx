
/*

inlets
	audio input
	mute
	solo
	level
	fade to,time
	
*/

MxUnit.register('MxChannel',{ arg chan,mx;

	var inlets,outlets,patch,inps,connectors,conn,handlers;

	inps = [MxArJack(chan.numChannels)];
	inlets = [ MxInlet(mx.nextID, "MxChannelIn", 0, MxHasJack({inps[0]})) ];

	conn = MxPlaysOnBus({chan.source.bus});
	outlets = [ MxOutlet(mx.nextID, "MxChannelOut", 0, AudioSpec(chan.numChannels), conn ) ];

	// doesn't play as a unit
	// just does its patching as a unit
	handlers = (
		init: {
			~source = chan
		},
		prepareToBundle:  { arg agroup, bundle, private, bus;
			},
		spawnToBundle: { arg bundle; 
			},
		stopToBundle: { arg bundle; 
			},

		play: { arg group, atTime, bus;
			},
		stop: { arg atTime,andFreeResources = true;
			}//,
	);	
	
	MxUnit(handlers,inlets,outlets);

});