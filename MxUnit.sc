

MxUnit  { 
	
	classvar registery,<protoHandler;

	var <handlers,<inlets,<outlets;
	var <>point,<>group;	
	
	*make { arg object,mx;
		object.class.superclassesDo({ arg class;
			var match,path;
			match = registery[class.name];
			if(match.isNil,{
				path = PathName(MxUnit.class.filenameSymbol.asString).parentPath +/+ "drivers" +/+ class.name.asString ++ ".scd";
				if(File.exists(path),{
					path.load;
					match = registery[class.name]
				});
			});				
			if(match.notNil,{
				^match.value(object,mx)
			})
		});
		Error("No MxUnit maker function registered for " + object.class).throw;
	}
	*new { arg handlers,inlets,outlets;
		var h;
		h = protoHandler.copy;
		h.putAll(handlers);
		^super.newCopyArgs(h,inlets,outlets).init
	}
	init {
		handlers.use { ~init.value() }
	}

	*register { arg classname,unitMaker;
		registery.put(classname.asSymbol, unitMaker)
	}

	// delegate to the handler
	prepareToBundle { arg agroup, bundle, private, bus;
		// create own bus and group here if it is audio/control
		// but the source object may already create a suitable group.
		// group is needed to make sure patch points can be reliably inserted
		^handlers.use { ~prepareToBundle.value(agroup,bundle,true,bus) }
	}
	spawnToBundle { arg bundle;
		^handlers.use { ~spawnToBundle.value(bundle) }
	}
	stopToBundle { arg bundle;
		^handlers.use { ~stopToBundle.value(bundle) }
	}
	play { arg group,atTime,bus;
		^handlers.use { ~play.value(group,atTime,bus) }
	}
	stop { arg atTime,andFreeResources=true;
		^handlers.use { ~stop.value(atTime,andFreeResources) }
	}
	// relocate  toBeat, atTime
	// gui
	// timeGui

	*initClass {
		registery = IdentityDictionary.new;
		
		protoHandler = (
			init: {}, // create any resources you need

			prepareToBundle:  { arg agroup, bundle, private, bus; },
			spawnToBundle: { arg bundle; },
			stopToBundle: { arg bundle; },

			play: { arg group, atTime, bus;},
			stop: { arg atTime,andFreeResources = true;}//,

			// crop
			// relocate: { arg toBeat, atTime; }
			// gui
			// timeGui
			// zoomTimeGui
		);
	}
}


MxInlet {
	
	var <>uid,<>name,<>index,<>spec,<>adapter;
	
	*new { arg uid,name,index,spec,adapter;
		^super.newCopyArgs(uid,name,index,spec,adapter)
	}
		
	printOn { arg stream;
		stream << name
	}
}


MxOutlet : MxInlet {
	
}

