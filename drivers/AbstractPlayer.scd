
/*

no inlets assumed, write a drive for specific subclasses
	
*/

MxUnit.register('AbstractPlayer',
(
	make: { arg player;
		var inlets,outlets;
		inlets  = [];
		outlets = [ MxOutlet(player.name, 0, player.spec ,MxPlaysOnBus({player.bus}) ) ];
		MxUnit(player,inlets,outlets);
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
	}
));