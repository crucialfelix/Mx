/*
	for the moment make this a 0..1 generic slider
	and let the mapping on the target take care of range
*/

MxUnit.register('NumberEditor',
(
	make: { arg num;
		var inlets,outlets,sips,ne;
		ne = NumberEditor(num,\unipolar);
		inlets  = [];
		outlets = [ MxOutlet(ne.asString, 0, ne.spec ,
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
		data
	}
));