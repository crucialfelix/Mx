
MxUnit.register('Instr',{ arg instr,mx;

	var inlets,outlets,patch,inps,connectors,conn,handlers;

	inps = instr.specs.collect({ arg spec,i; MxJack.forSpec(spec,instr.defArgAt(i) ) });
	patch = Patch(instr,inps );
	connectors = patch.inputs.collect({ arg inp,i; 
		if(inp.isKindOf(MxJack),{
			MxHasJack({inp})
		},{
			// many things it could be
			nil
		})
	});
	inlets = instr.specs.collect({ arg spec,i; MxInlet(mx.nextID,instr.argNameAt(i),i,spec,connectors[i] ) });
	if(instr.outSpec.isKindOf(AudioSpec) or: {instr.outSpec.isKindOf(ControlSpec)},{
		conn = MxPlaysOnBus({patch.bus});
	});
	outlets = [ MxOutlet(mx.nextID, (instr.outSpec.findKey ? instr.outSpec.class).asString, 0, instr.outSpec, conn ) ];

	handlers = (
		init: {
			~source = patch
		},
		prepareToBundle:  { arg agroup, bundle, private, bus;
			// happening twicer
			"prepare".postln;
			~source.prepareToBundle(agroup,bundle,private,bus)
			},
		spawnToBundle: { arg bundle; 
			~source.spawnToBundle(bundle)
			},
		stopToBundle: { arg bundle; 
			~source.stopToBundle(bundle)
			},

		play: { arg group, atTime, bus;
			~source.play(group,atTime,bus) 
			},
		stop: { arg atTime,andFreeResources = true;
			~source.stop(atTime,andFreeResources)
			}//,
	);	
	
	MxUnit(handlers,inlets,outlets);

});