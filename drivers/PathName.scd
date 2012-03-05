
MxUnit.register('PathName',(

	make: { arg pathName;

		var inlets,outlets;
		
		~source = pathName;
		
		inlets = [];
		outlets = [ ];
		
		MxUnit(pathName,inlets,outlets);
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
	save: { 
		[~source]
	},
	load: { arg args;
		args.first
	},
	copy: { ~save.value },
*/
	name: { ~source.fileName ? "Document "},
	gui: { arg layout;
		Document.open(~source.fullPath);
	},
));
