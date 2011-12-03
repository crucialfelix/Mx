

MxUnit.register('MultiSplineFr',
(
	make: { arg sfr;
		var inlets,outlets,rec;
		~source = sfr;
		~recording = false;
		rec = MxSetter({ arg v; 
			if(~recording,{ sfr.setValueOnNextTick(v) }) 
		}.inEnvir);
		inlets  = [ MxInlet("setValueNow",0,sfr.spec,rec) ];
		outlets = Array.fill(~source.spline.numDimensions-1,
				 { arg di;
					MxOutlet("DIM" ++ (di+1), di, sfr.spec, MxSendsValueOnChanged({outlets[di]}) ) 
				 });
		MxUnit(sfr,inlets,outlets);
	},
	frameRateDevices: {
		[{ arg time; 
			var values;
			values = ~source.value(time);
			values.do { arg v,i;
				if(i > 0,{
					// real messaging happens here
					~unit.outlets[i-1].changed(v)
				})
			};
			values // the function will get .changed(values) but nobody listens
		}.inEnvir]
	},
	play: { arg group, atTime, bus;
	},
	stop: { arg atTime,andFreeResources = true;
	},
	save: {
		~source.storeArgs
	},
	load: { arg data;
		MultiSplineFr(*data)
	},
	spec: {
		~source.spec
	},
	gui: { arg layout;
		~source.gui(layout,900@700)
	},
	timeGui: { arg layout, bounds, maxTime;
		~splineGui = MxMultiSplineTimeGui(~source).gui(layout,bounds,maxTime);
	},
	zoomTime: { arg from,to;
		~splineGui.setZoom(from,to);
	},
	setMaxTime: { arg maxTime;
		~splineGui.setMaxTime( maxTime );
	},
	beatDuration: {
		~source.spline.points.last[0]
	},
	record: { arg boo,atTime;
		~recording = boo;
		if(boo.not,{
			~splineGui.update
		})
	}
));