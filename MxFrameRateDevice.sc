

MxFrameRateDevice {
	
	var <>func;
	
	*new { arg func;
		^super.newCopyArgs(func)
	}
	
	tick { arg time;
		var v;
		v = func.value(time);
		func.changed(v);
		^v
	}
}

