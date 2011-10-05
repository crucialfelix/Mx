
/*

setSongControl
	MxSetter
	
setPatternControl

gotoPatternID

*/

MxUnit.register('TotalTracker',
(
	make: { arg player;
		var inlets,outlets,sips,patter,switcher;
		
		sips = [1,2,3,4].collect { arg i; 
				MxInlet(i.asString,i,\unipolar, 
					MxSetter({ arg v; player.app.setSongControl(i,v)},{player.server},{player.group}) 
				)
			};

		switcher = TrPatternSwitcher(player);
		~switcher = switcher;
		// ok for midi note where you can choose
		// but how to do a float that wanders over them ?
		patter = MxInlet("pattern",0,ControlSpec(0,127,step:1),
					MxSetter({ arg v;
						var p;
						switcher.wrapSelect(v.asInteger);
					}));
		sips = sips.insert(0,patter);
		inlets  = sips;
		outlets = [ MxOutlet(player.name, 0, player.spec, MxPlaysOnBus({player.bus}) ) ];
		MxUnit(player,inlets,outlets);
	},
	prepareToBundle:  { arg agroup, bundle, private, bus;
		~source.prepareToBundle(agroup,bundle,private,bus)
	},
	spawnToBundle: { arg bundle; 
		~source.spawnToBundle(bundle)
	},
	stopToBundle: { arg bundle; 
		~source.stopToBundle(bundle)
	},
	freeToBundle: { arg bundle;
		var sw;
		~source.freeToBundle(bundle);
		sw = ~switcher;
		bundle.addFunction({
			sw.free;
		});
	},	

	play: { arg group, atTime, bus;
		~source.play(group,atTime,bus)
	},
	stop: { arg atTime,andFreeResources = true;
		~source.stop(atTime,andFreeResources)
	},
	save: {
		var app;
		app = ~source.app;
		[~source.song.path,app.playMode,app.currentPatternID,[1,2,3,4].collect(app.getSongControl(_)) ]
	},
	load: { arg data;
		var t;
		t = TotalTracker(data[0],false);
		if(data[1] == "song",{
			t.app.setSongPlayMode
		},{
			t.app.setPatternPlayMode;
			t.app.gotoPatternID(data[2])
		});
		data[3].do { arg v,i;
			t.app.setSongControl(i+1,v)
		};
		t
	},
	name: { ~source.name },
	gui: { arg layout;
		var patternSelectGui,env;
		patternSelectGui = SongPatternSelectGui(~source);
		patternSelectGui.total_(~source);
		layout = layout ?? {PageLayout(~source.name,Rect(100,100,600,300))};
		env = currentEnvironment; 
		ActionButton(layout,"Full GUI ^",{
			env.use {
				~source.app.publish.mount;
				InfiniteGesture(gui:false).total_(~source).gui
			}
		});
		patternSelectGui.gui(layout);
	}
	
));
