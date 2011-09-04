

MxUnit.register('SplineFr',
(
	make: { arg sfr;
		var inlets,outlets;
		inlets  = [];
		outlets = [ MxOutlet("out", 0, sfr.spec ,MxSendsValueOnChanged({sfr}) ) ];
		MxUnit(sfr,inlets,outlets);
	},
	frameRateDevices: {
		[~source]
	},
	play: { arg group, atTime, bus;
	},
	stop: { arg atTime,andFreeResources = true;
	},
	save: {
		~source.storeArgs
	},
	load: { arg data;
		SplineFr(*data)
	},
	
	gui: { arg layout;
		~source.spline.gui(layout,900@100,~source.spec)
	}
		
));