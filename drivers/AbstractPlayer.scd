
/*

no inlets assumed, write a driver for specific subclasses
	
*/

MxUnit.register('AbstractPlayer',
(
	make: { arg player;
		var inlets,outlets,pob;
		inlets  = [];
		if(player.rate  == 'control',{
			pob = MxPlaysOnKrBus({player.bus})
		},{
			pob = MxPlaysOnBus({player.bus})
		});			
		outlets = [ MxOutlet(player.name, 0, player.spec ,pob ) ];
		MxUnit(player,inlets,outlets);
	},

	play: { arg group, atTime, bus;
		~source.play(group,atTime,bus)
	},
	stop: { arg atTime,andFreeResources = true;
		~source.stop(atTime,andFreeResources)
	},
	save: {
		if(~source.path.notNil,{
			~source.enpath
		},{
			~source
		})
	},
	load: { arg data;
		if(data.isString,{
			data.loadPath
		},{
			data
		})
	}
));