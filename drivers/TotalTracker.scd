
/*

setSongControl
	MxSetter
	
setPatternControl

gotoPatternID

*/

MxUnit.register('TotalTracker',
(
	make: { arg player;
		var inlets,outlets,sips;
		~loadDefs.value;
		
		sips = [1,2,3,4].collect { arg i; 
				var ii;
				ii = i + 1;
				MxInlet(ii.asString,i,\unipolar, 
					MxSetter({ arg v; player.app.setSongControl(ii,v)},{player.server},{player.group}) 
				)
			};
		inlets  = sips;
		outlets = [ MxOutlet(player.name, 0, player.spec ,MxPlaysOnBus({player.bus}) ) ];
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
		~loadDefs.value;
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
	gui: {
		InfiniteGesture(gui:false).total_(~source).gui
	},
	
	// private
	loadDefs: {
		if(TrEventDef.eventDefs.size == 0,{
			TrEventDef.loadAll;
		})
	}

));