

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
			if(inp.isKindOf(MxJack),{
				MxHasJack({inp})
			},{
				// many things it could be
				nil
			})
		});
		inlets = instr.specs
			.collect({ arg spec,i; MxInlet(instr.argNameAt(i),i,spec,connectors[i] ) });
		if(instr.outSpec.isKindOf(AudioSpec) or: {instr.outSpec.isKindOf(ControlSpec)},{
			conn = MxPlaysOnBus({patch.bus});
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
	save: { [~source.instr.dotNotation, ~source.inputs] },
	load: { arg nameinp;
		[Instr(nameinp[0]),nameinp[1]]
	},
	copy: { ~source.instr },
	name: { ~source.instr.dotNotation }

));

