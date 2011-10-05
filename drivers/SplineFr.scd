

MxUnit.register('SplineFr',
(
	make: { arg sfr;
		var inlets,outlets;
		~source = sfr;
		inlets  = [];
		outlets = [ MxOutlet("out", 0, sfr.spec, MxSendsValueOnChanged({sfr}) ) ];
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
	spec: {
		~source.spec
	},
	gui: { arg layout;
		~source.spline.gui(layout,900@100,~source.spec)
	},
	timeGui: { arg layout, bounds, maxTime;
		~splineGui = ~source.gui(layout,bounds,maxTime);
	},
	zoomTime: { arg from,to;
		~splineGui.setZoom(from,to).update;
	}
));
