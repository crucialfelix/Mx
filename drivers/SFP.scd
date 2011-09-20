

MxUnit.register('SFP',(

	make: { arg sfp;

		var inlets,outlets;
		
		inlets = [];

		outlets = [ MxOutlet( "out", 0, AudioSpec(sfp.numChannels), MxPlaysOnBus({sfp.bus}) ) ];
		
		~source = sfp;
		
		MxUnit(sfp,inlets,outlets);
	},
	// should be able to inherit
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
		bundle.add( ~source.synth.moveToHeadMsg(aGroup) );
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
	isPlaying: {
		~source.isPlaying
	},
	save: { 
		[~source.soundFilePath]
	},
	load: { arg path;
		SFP(path)
	},
	copy: { ~source.soundFilePath },
	name: { PathName(~source.soundFilePath).fileNameWithoutExtension },
	
	gui: { arg layout;
		var l;
		l = layout ?? {PageLayout(~name.value,Rect(0,0,500,600))};
		~source.gui(l);
		if(layout.isNil,{
			l.resizeToFit
		})
	}

));

