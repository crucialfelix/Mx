
MxUnit.register('PathName',(

	make: { arg pathName;

		var inlets,outlets;
		
		~path = pathName;
		~source = ~path;

		// all inlets and outlets are managed from document
		// not by what the result is
		inlets = [];
		outlets = [];
		
		MxUnit(pathName,inlets,outlets);
	},
	didLoad: {
		~evalCode.value();
	},
	evalCode: {
		try {
			~result = ~path.fullPath.load;
		} { arg e;
			("ERROR WHILE LOADING:" + ~path.fullPath).inform;
			e.reportError;
			~result = nil;
		};
		// really needs to get handlers for whatever the object is
		// and proxy most further handlers to that
		// could simply overwrite my own variables.
		/*if(~result.notNil,{
			~delegate = MxUnit.handlersForClass(~result.class);
			if(~delegate.isNil,{
				("No handlers found for result of document:" + ~result).warn;
			})
		});*/
	},
	
/*
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
	moveToHead: { arg aGroup,bundle,currentGroup; 
		bundle.add( ~source.spawnGroup.moveToHeadMsg(aGroup) );
	},
		
	numChannels: { 
		~source.numChannels
		},
	spec: {
		~source.spec
	},
	play: { arg group, atTime, bus;
		~source.play(group,atTime,bus) 
		},
	stop: { arg atTime,andFreeResources = true;
		~source.stop(atTime,andFreeResources)
		},
	respawn: {
		var group,bus;
		group = ~source.group;
		bus = ~source.bus;
		~source.stop;
		~source.play(group,1,bus)
	},
	gotoBeat: { arg beat,atBeat,bundle;
		~source.gotoBeat(beat,atBeat,bundle)
	},
		
	isPlaying: {
		~source.isPlaying
	},
*/
	save: { 
		[~path]
	},
	load: { arg args;
		args.first
	},
	// this might work: you would have two identical code, but running separately
	// with own environment
	copy: { ~save.value },

	name: { ~source.fileName ? "Document "},
	gui: { arg layout;
		Document.open(~source.fullPath,envir:currentEnvironment);
	},
));
