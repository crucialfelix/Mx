
var cs;
cs = ControlSpec(0,127,\lin,1);

MxUnit.register('NumberEditor',
(
	make: { arg num;
		var inlets,outlets,sips;
		inlets  = [];
		outlets = [ MxOutlet(num.asString, 0, num.spec ,
						MxHasAction({arg action; num.action = action  }) ) 
				];
		MxUnit(num,inlets,outlets);
	},
	spec: {
		~source.spec
	},
	prepareToBundle:  { arg agroup, bundle, private, bus;
	},
	spawnToBundle: { arg bundle;
	},
	stopToBundle: { arg bundle; 
	},

	save: {
		~source
	},
	load: { arg data;
		data
	}
));