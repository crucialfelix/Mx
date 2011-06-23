
/*

no inlets assumed, write a drive for specific subclasses
	
*/

MxUnit.register('AbstractPlayer',{ arg player,mx;

	var inlets,outlets,patch,inps,connectors,conn,handlers;

	inlets  = [];
	outlets = [ MxOutlet(mx.nextID, player.name, 0, player.spec ,MxPlaysOnBus({player.bus}) ) ];

	handlers = (
		init: {
			~source = player
		},
		prepareToBundle:  { arg agroup, bundle, private, bus;
			~source.prepareToBundle(agroup,bundle,private,bus)
			},
		spawnToBundle: { arg bundle; 
			~source.spawnToBundle(bundle)
			},
		stopToBundle: { arg bundle; 
			~source.stopToBundle(bundle)
			},
		
		play: { arg group, atTime, bus;
			~source.play(group,atTime,bus)
			},
		stop: { arg atTime,andFreeResources = true;
			~source.stop(atTime,andFreeResources)
			}//,
	);
	
	MxUnit(handlers,inlets,outlets);

});