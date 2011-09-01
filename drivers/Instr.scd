

MxUnit.register('Instr',(

	make: { arg instr;

		var inlets,outlets,patch,inps,connectors,conn,handlers;
		// accept either an instr or [instr, inputs]
		if(instr.isSequenceableCollection,{
			# instr,inps = instr;
		},{
			inps = instr.specs.collect({ arg spec,i; MxJack.forSpec(spec,instr.defArgAt(i) ) });
		});
		patch = Patch(instr,inps );
		connectors = patch.inputs.collect({ arg inp,i; 
			if(inp.isKindOf(MxArJack),{
				MxHasJack({inp},{patch.server},{patch.group})
			},{
				if(inp.isKindOf(MxKrJack),{
					MxHasKrJack({inp},{patch.server},{patch.group})
				},{
					// many things it could be
					nil
				})
			})
		});
		inlets = instr.specs
			.collect({ arg spec,i; MxInlet(instr.argNameAt(i),i,spec,connectors[i] ) });

		if(instr.outSpec  == 'control',{
			conn = MxPlaysOnKrBus({patch.bus},{patch.server},{patch.group})
		},{
			conn = MxPlaysOnBus({patch.bus},{patch.server},{patch.group})
		});
		outlets = [ MxOutlet( (instr.outSpec.findKey ? instr.outSpec.class).asSymbol, 0, instr.outSpec, conn ) ];
		
		~source = patch;
		
		MxUnit(instr,inlets,outlets);
	},
	prepareToBundle: { arg agroup, bundle, private, bus;
		~source.prepareToBundle(agroup,bundle,private,bus);
		},
	spawnToBundle: { arg bundle; 
		~source.spawnToBundle(bundle)
		},
	stopToBundle: { arg bundle; 
		~source.stopToBundle(bundle)
		},
	freeToBundle: { arg bundle; 
		~source.freeToBundle(bundle)
		},
	numChannels: { 
		~source.numChannels
		},

	play: { arg group, atTime, bus;
		~source.play(group,atTime,bus) 
		},
	stop: { arg atTime,andFreeResources = true;
		~source.stop(atTime,andFreeResources)
		},
	save: { 
		var inps;
		inps = ~source.inputs.collect { arg inp;
			if(inp.isKindOf(MxKrJack) and: {inp.isKindOf(MxArJack).not},{
				inp.value
			},{
				inp
			})
		};
		[~source.instr.dotNotation, inps ] 
	},
	load: { arg nameinp;
		var inps,instr;
		inps = nameinp[1];
		instr = Instr(nameinp[0]);
		inps = instr.specs.collect({ arg spec,i; MxJack.forSpec(spec,inps[i]) });

		[instr,inps]
	},
	copy: { ~source.instr },
	name: { ~source.instr.dotNotation }

));

