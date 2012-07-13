
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
					MxSetter({ arg v; player.app.setSongControl(i,v) },
						{ player.server },
						{ player.group },
						{ player.app.getSongControl(i) ? 0 },
						{ arg v; player.app.setSongControl(i,v)}
						)
				)
			};
		player.numOuts = TotalTracker.numSongOuts;
		switcher = TrPatternSwitcher(player);
		~switcher = switcher;
		// ok for midi note where you can choose
		// but how to do a float that wanders over them ?
		patter = MxInlet("pattern",0,ControlSpec(0,player.app.numPatterns,step:1),
					MxSetter({ arg v;
						var p;
						switcher.wrapSelect(v.asInteger);
					}));
		sips = sips.insert(0,patter);
		sips = sips.add( MxInlet("audio",5,AudioSpec(2),MxListensToBus({ player.song.efxBus },{player.server},{player.group}) ) );
		inlets  = sips;
		outlets = [ MxOutlet("out", 0, AudioSpec(2), MxPlaysOnBus({player.bus}) ) ];
		player.numOuts.do { arg i;
			outlets = outlets.add( MxOutlet((i+1).asString,i+1,AudioSpec(2),MxPlaysOnBus({ player.songBusses.at(i) }) ) );
		};
		outlets = outlets.add( MxOutlet("fx",player.numOuts + 2, AudioSpec(2), MxPlaysOnBus({ player.song.efxBus }) ) );
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
		// will load afterwards, will be blank song until loaded
		t = TotalTracker(data[0],false,{ arg t;
			if(data[1] == "song",{
				t.app.setSongPlayMode
			},{
				t.app.setPatternPlayMode;
				t.app.gotoPatternID(data[2])
			});
			data[3].do { arg v,i;
				t.app.setSongControl(i+1,v)
			};
		});
		t
	},
	name: { ~source.name },
	gui: { arg layout;
		var l;
		l = layout ?? {PageLayout(~source.name,Rect(100,100,800,600))};
		TrMxSongController(~source).gui(l);
		if(layout.isNil,{
			l.resizeToFit
		})
	},

	timeGui: { arg layout, bounds, maxTime;
		~tg = TrMxSongTimeGui(~source);
		~tg.gui(layout,bounds,maxTime);
	},
	zoomTime: { arg from,to;
		~tg.setZoom(from,to).update;
	},
	beatDuration: {
		max(~source.app.endsAtBeat,300)
	},
	gotoBeat: { arg beat,atBeat,bundle;
		~source.app.gotoBeatAtBeat(beat,atBeat)
	},
	record: { arg boo,atTime;
		~source.app.recordSong(boo)	
	}
	
));
