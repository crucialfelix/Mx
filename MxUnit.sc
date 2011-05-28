

MxUnit  { 
	
	classvar registery;

	var <source,<inlets,<outlets,<relocator;
	var <>point,<>group;	
	
	*new { arg source,inlets,outlets,relocator;
		^super.newCopyArgs(source,inlets,outlets,relocator)
	}

	*make { arg object,mx;
		object.class.superclassesDo({ arg class;
			var match;
			match = registery[class.name];
			if(match.notNil,{
				^match.value(object,mx)
			})
		});
		Error("No MxUnit maker function registered for " + object.class).throw;
	}			
	*register { arg classname,unitMaker;
		registery.put(classname.asSymbol, unitMaker)
	}
	prepareToBundle { arg group,bundle;
		source.prepareToBundle(group,bundle)
	}
	spawnToBundle { arg bundle;
		source.spawnToBundle(bundle)
	}
	stopToBundle { arg bundle;
		source.stopToBundle(bundle)
	}
	*initClass {
		registery = IdentityDictionary.new;
		
		this.register(\Instr,{ arg p,mx;
			var inlets,outlets,patch,inps,connectors,conn,relocator;
			inps = p.specs.collect({ arg spec; MxJack.forSpec(spec) });
			patch = Patch(p,inps );
			connectors = patch.inputs.collect({ arg inp,i; 
				if(inp.isKindOf(MxJack),{
					MxConnector(inp)
				},{
					// many things it could be
					nil
				})
			});
			inlets = p.specs.collect({ arg spec,i; MxInlet(mx.nextID,p.argNameAt(i),i,spec,connectors[i] ) });
			if(p.outSpec.isKindOf(AudioSpec) or: {p.outSpec.isKindOf(ControlSpec)},{
				conn = MxPlaysOnBus({patch.bus});
			});
			outlets = [ MxOutlet(mx.nextID, (p.outSpec.findKey ? p.outSpec.class).asString, 0, p.outSpec, conn ) ];
			// todo relocator
			MxUnit(p,inlets,outlets,relocator);
		});
	}
}


MxInlet {
	
	var <>uid,<>name,<>index,<>spec,<>connector;
	
	*new { arg uid,name,index,spec,connector;
		^super.newCopyArgs(uid,name,index,spec,connector)
	}
		
	printOn { arg stream;
		stream << name
	}
}


MxOutlet : MxInlet {
	
}

