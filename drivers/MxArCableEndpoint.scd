
/*

inlets
	audio input
	mute
	solo
	level
	fade to,time
	
*/

MxUnit.register('MxArCableEndpoint',{ arg endpoint,mx;

	// at creation time, do I know how many ins it needs ?
	// yes
	
	// at spawn time
	
	var inlets,outlets,patch,inps,connectors,conn,handlers;

	inps = [MxArJack(endpoint.numChannels)];
	inlets = [ MxInlet(mx.nextID, "jackpoint", 0, MxMethodCall({  })) ];

	conn = MxPlaysOnBus({endpoint.bus});
	outlets = [ MxOutlet(mx.nextID, "MxArCableEndpoint", 0, AudioSpec(endpoint.numChannels), conn ) ];

	// doesn't play as a unit
	// just does its patching as a unit
	handlers = (
		init: {
			~source = endpoint
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