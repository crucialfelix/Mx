/*
	
	

*/

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

MxDrawer.add("InstrBuilder Effect 4",{  InstrBuilder([\audio,\one,\two,\three,\four]) });

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


//MxDrawer.add("InstrBuilder 4",{ InstrBuilder(\one,\two,\three,\four) });
