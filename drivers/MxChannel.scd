
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

	inlets  = [ MxInlet(mx.nextID, "MxChannelIn", 0, AudioSpec(chan.numChannels), MxListensToBus({chan.source.bus},{chan.server},{chan.mixGroup})) ];
	outlets = [ MxOutlet(mx.nextID, "MxChannelOut", 0, AudioSpec(chan.numChannels),MxPlaysOnBus({chan.source.bus}) ) ];

	// doesn't play as a unit
	// we just wanted the inlets and outlets for patching
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