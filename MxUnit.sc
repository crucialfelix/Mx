

MxUnit  { 
	
	classvar registery,<protoHandler;

	var <>source,<inlets,<outlets,<>handlers;
	var <>group,<>point;
	
	*make { arg source,mx,ids,class;
		var handlers;
		if(source.isKindOf(MxUnit),{
			^source
		});
		handlers = this.handlersFor(class ? source.class);
		ids = ids ? [[],[]];
		^handlers.use {
			var unit;
			unit = ~make.value(source);
			if(~source.isNil,{
				~source = source;
			});
			unit.handlers = handlers;
			unit.inlets.do { arg in,i;
				mx.register(in,ids[0][i]);
				in.unit = unit;
			};
			unit.outlets.do { arg out,i;
				mx.register(out,ids[1][i]);
				out.unit = unit;
			};
			// unit.registerWithMx(mx);
			unit
		}
	}
	*loadData { arg data,mx;
		var h,source,class,ids;
		# class, data, ids = data;
		class = class.asClass;
		h = this.handlersFor(class);
		source = h.use { ~load.value(data) };
		^this.make(source,mx,ids,class)
	}
	*new { arg source,inlets,outlets;
		^super.newCopyArgs(source,inlets,outlets)
	}
	saveData {
		var data,ids;
		data = handlers.use { ~save.value(source) };
		ids = [ inlets.collect(_.uid), outlets.collect(_.uid) ];
		^[source.class.name,data,ids]
	}
	*handlersFor { arg class;
		var h;
		h = protoHandler.copy;
		class.superclassesDo({ arg class;
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
				h.putAll(match);
				^h
			})
		});
		Error("No MxUnit driver found for " + class).throw;
	}
	getInlet { arg index;
		if(index.isNil,{
			^inlets.first
		});
		if(index.isInteger,{
			^inlets[index]
		},{
			inlets.do { arg in;
				if(in.name == index,{
					^in
				})
			}
		});
		Error("Inlet not found:" + index).throw
	}
	getOutlet { arg index;
		if(index.isNil,{
			^outlets.first
		});
		if(index.isInteger,{
			^outlets[index]
		},{
			outlets.do { arg in;
				if(in.name == index,{
					^in
				})
			}
		});
		Error("Outlet not found:" + index).throw
	}
	*register { arg classname,handlers;
		registery.put(classname.asSymbol, handlers)
	}
	// change name, confusing
	registerWithMx { arg mx;
		inlets.do { arg in; mx.register(in,in.uid) };
		outlets.do { arg in; mx.register(in,in.uid) };
	}

	// methods delegated to the handlers
	prepareToBundle { arg agroup, bundle, private, bus;
		^handlers.use { ~prepareToBundle.value(agroup,bundle,true,bus) }
	}
	spawnToBundle { arg bundle;
		^handlers.use { ~spawnToBundle.value(bundle) }
	}
	stopToBundle { arg bundle;
		^handlers.use { ~stopToBundle.value(bundle) }
	}
	freeToBundle { arg bundle;
		^handlers.use { ~freeToBundle.value(bundle) }
	}		
	play { arg group,atTime,bus;
		^handlers.use { ~play.value(group,atTime,bus) }
	}
	stop { arg atTime,andFreeResources=true;
		^handlers.use { ~stop.value(atTime,andFreeResources) }
	}
	numChannels {
		^handlers.use { ~numChannels.value }
	}
	copySource {
		^handlers.use { ~copy.value }
	}
	// relocate  toBeat, atTime
	name {
		^handlers.use { ~name.value }
	}
	gui { arg layout,bounds;
		^handlers.use { ~gui.value(layout,bounds) }
	}	
	// timeGui


	*initClass {
		registery = IdentityDictionary.new;
		
		protoHandler = (
			make: { arg object; MxUnit(object) },
			save: { ~source.asCompileString },
			load: { arg string; string.compile.value() },
			copy: { ~source.deepCopy },
			
			prepareToBundle:  { arg agroup, bundle, private, bus; },
			spawnToBundle: { arg bundle; },
			stopToBundle: { arg bundle; },
			freeToBundle: { arg bundle; },

			play: { arg group, atTime, bus;},
			stop: { arg atTime,andFreeResources = true;},
			numChannels: { 2 },
			gui: { arg layout,bounds; 
				~source.gui(layout ?? {Window(~name.value,bounds).front},bounds) 
			},
			name: { ~source.asString }

			// crop
			// relocate: { arg toBeat, atTime; }
			// timeGui
			// zoomTimeGui
			// asCompileString
		);
	}
}


MxInlet {
	
	var <>name,<>index,<>spec,<>adapter;
	var <>uid,<>unit;
	
	*new { arg name,index,spec,adapter;
		^super.newCopyArgs(name.asSymbol,index,spec.asSpec,adapter)
	}
	storeArgs {
		^[name,index,spec,adapter]
	}
	printOn { arg stream;
		stream << name
	}
}


MxOutlet : MxInlet {
	
}

