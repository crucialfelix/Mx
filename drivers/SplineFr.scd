

MxUnit.register('SplineFr',
(
	make: { arg sfr;
		var inlets,outlets,rec;
		~source = sfr;
		~recording = false;
		rec = MxSetter({ arg v; 
			if(~recording,{ sfr.setValueOnNextTick(v) }) 
		}.inEnvir);
		inlets  = [ MxInlet("setValueNow",0,sfr.spec,rec) ];
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
	},
	record: { arg boo,atTime;
		~recording = boo;
		if(boo.not,{
			~splineGui.update
		})
	}
));
