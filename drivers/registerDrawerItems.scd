/*
	onMake is a callback that is given the object to insert onto the grid
	
	

*/

MxDrawer.addGroup("Instr",{
	Instr.leaves.collect { arg instr; [instr.dotNotation ,instr] }
},{ arg instr,i,instrname,onMake;
	onMake.value(instr)
});

MxDrawer.add("Slider",{ arg i,onMake; onMake.value(NumberEditor(1.0)) });

MxDrawer.add("CC",{ arg i,onMake; onMake.value(CCResponder.new) });

MxDrawer.add("SplineKr",{ arg i,onMake; onMake.value(KrSplinePlayer.new) });

MxDrawer.add("SplineFr",{ arg i,onMake; onMake.value(SplineFr.new) });

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
	},{ arg path,i,filename,onMake;
		onMake.value(SFP(path))
	});
};

