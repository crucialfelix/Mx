/*
	onMake is a handler that is given the object to insert onto the grid
	
	

*/

MxDrawer.addGroup("Instr",{
	Instr.leaves.collect { arg instr; [instr.dotNotation ,instr] }
},{ arg instr,i,instrname,onMake;
	onMake.value(instr)
});

MxDrawer.add("Slider",{ arg i,onMake; onMake.value(NumberEditor(1.0)) });

MxDrawer.add("CC",{ arg i,onMake; onMake.value(CCResponder.new) });

MxDrawer.add("Spline.kr",{ arg i,onMake; onMake.value(KrSplinePlayer.new) });
