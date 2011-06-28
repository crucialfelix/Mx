
var cs;
cs = ControlSpec(0,127,\lin,1);

MxUnit.register('CCResponder',
(
	make: { arg ccr;
		var inlets,outlets,sips;
		inlets  = [];
		outlets = [ MxOutlet("CC"+ccr.num, 0, cs ,
						MxHasAction({arg action; ccr.function = action  }) ) 
				];
		MxUnit(ccr,inlets,outlets);
	},
	spec: {
		cs		
	},
	prepareToBundle:  { arg agroup, bundle, private, bus;
	},
	spawnToBundle: { arg bundle;
		/*var s;
		s = ~source;
		// midi responders could refactor in order to detect if installed or not
		bundle.addFunction({
			s.class.add(s)
		}) */
	},
	freeToBundle: { arg bundle; 
		var s;
		s = ~source;
		bundle.addFunction({
			s.class.remove(s)
		})
	},

	save: {
		[~source.src,~source.chan,~source.num]
	},
	load: { arg data;
		CCResponder(nil,data[0],data[1],data[2],true)
	}
));