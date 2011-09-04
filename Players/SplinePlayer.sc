
/*
	the only thing this adds is a spec and player compatibility.
	but player compat is only adding a few methods.
	this can add a gui that is full-service while leaving basic spline classes alone
*/

KrSplinePlayer : AbstractPlayer {
	
	var <>gen,>spec;
	
	*new { arg spline, dimension=0,loop=false;
		^super.new.gen_(SplineGen(spline ?? {
			BezierSpline(
				0@0,
				  [ 0.2@0.4, 0.5@0.9  ],
				120@1,
				  [],
				false
			)},dimension,loop))
	}
	storeArgs { ^gen.storeArgs }
	kr {
		// embeds the interpolated spline in the synth def
		^gen.kr
	}
	defName {
		^"ksp"++gen.spline.hash
	}
	rate { ^\control }
	spec { ^(spec ?? { \unipolar.asSpec }) }
}


SplineFr { 
	
	var <>spline,<>dimension,<>loop=false,<>spec;
	var table;
	
	*new { arg spline, dimension=0,loop=false,spec;
		^super.newCopyArgs(spline ?? {
			spec = spec ?? {'unipolar'.asSpec};
			BezierSpline(
				0@0,
				  [ 0.2@0.4, 0.5@0.9  ],
				120@1,
				  [],
				false
			)},dimension,loop,spec).init
	}
	storeArgs { ^[spline,dimension,loop] }

	init {
		this.initTable;
		spline.addDependant(this);
	}
	initTable {
		table = spline.bilinearInterpolate(Mx.defaultFrameRate * spline.points.last[dimension],dimension,true);
	}
	update {
		this.initTable
	}
	free {
		spline.removeDependant(this)
	}
	value { arg time;
		^table.intAt(time)
	}
}	


