/*
	for the moment make this a 0..1 generic slider
	and let the mapping on the target take care of range
*/

MxUnit.register('NumberEditor',
(
	make: { arg num;
		var inlets,outlets,sips,ne;
		ne = NumberEditor(num.value,\unipolar);
		~source = ne;
		inlets  = [ MxInlet("in",0,ne.spec,MxSetter({ arg v; ne.valueAction = v })) ];
		outlets = [ MxOutlet("action", 0, ne.spec ,
						MxHasAction({arg action; ne.action = action  }) ) 
				];
		MxUnit(ne,inlets,outlets);
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
		// this just saves the value
		~source.value
	},
	load: { arg data;
		// return args for make
		data // NumberEditor(data,\unipolar)
	}
));