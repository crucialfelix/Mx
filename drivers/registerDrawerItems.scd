

MxDrawer.add("Slider",{ NumberEditor(1.0) });

MxDrawer.add("CC",{ CCResponder.new });

MxDrawer.add("SplineKr",{ KrSplinePlayer.new });

MxDrawer.add("SplineFr",{ SplineFr.new });

MxDrawer.add("MultiSplineFr",{ 
	var b;
	b = BezierSpline.fromPoints(
						[[0,0,0,0,0],
						[30,0,0,0,0],
						 [60,0,0,0,0]]);
	MultiSplineFr(b,spec:\unipolar.asSpec);
});


MxDrawer.addGroup("Instr",{
	Instr.leaves.collect { arg instr; [instr.dotNotation ,instr] }
},{ arg instr,instrname;
	instr
});

MxDrawer.addGroup("InstrGateSpawner",{
	Instr.leaves.collect { arg instr; [instr.dotNotation ,instr] }
},{ arg instr,instrname;
	InstrGateSpawner(instr)
});

MxDrawer.add("Patterns from Instr...",{
	var defAction;
	defAction = MxDeferredDrawerAction.new;
	InstrBrowser({ arg layout,instr;
		ActionButton(layout,"Make pattern...",{
			var min,max;
			Sheet({ arg layout;
				var p;
				p = Patch(instr);
				p.gui(layout);
				ActionButton(layout,"RND",{
					p.rand;
				});
				ActionButton(layout,"Test",{
					var vals;
					vals = p.value.asStream.nextN(200);
					min = vals.minItem;
					max = vals.maxItem;
					vals.plot2
				});
				ActionButton(layout,"MAKE AND INSERT TO MX",{
					var pattern,spec;
					pattern = p.value;
					if(min.notNil and: max.notNil,{
						spec = StreamSpec( ScalarSpec(min,max) );
					},{
						spec = instr.outSpec
					});
					defAction.value(MxUnit.make([pattern,spec],Pattern))
				});
			},"Generate pattern from a function")
		})
	}).rate_('stream').init.gui;
	defAction
});




[	[ "~/SoundFiles", "~/SoundFiles/*"], //personal
	[ "Recordings",   thisProcess.platform.recordingsDir +/+ "*"]
].do { arg nameSearchPath;
	
	MxDrawer.addGroup(nameSearchPath[0],{ 
		var all,formats,search;
		all = List.new;
		formats = [\aif,\wav,\WAV,\AIF,\AIFF,\aiff];
		search = { arg searchPath;
			searchPath.pathMatch.do { arg path;
				var pn = PathName(path);
				if(formats.includes( (pn.extension ? \none).asSymbol ),{
					all.add([pn.fileNameWithoutExtension,path])
				},{
					if(pn.isFolder,{
						search.value(pn +/+ "*")
					})
				})
			}
		};
		search.value(nameSearchPath[1]);
		all
	},{ arg path,filename;
		SFP(path)
	});
};

