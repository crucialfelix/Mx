

MxFrameRateDevice {
	
	var <>func,<>source;
	
	*new { arg func,source;
		^super.newCopyArgs(func,source)
	}
	
	tick { arg time;
		var v;
		v = func.value(time);
		func.changed(v);
		^v
	}
}

