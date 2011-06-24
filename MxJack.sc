

MxJack {
	
	*forSpec { arg spec,defArg;
		if(spec.isKindOf(AudioSpec),{
			^MxArJack.new.value_(defArg ? 127)
		});
		// StaticSpec
		// StaticIntegerSpec
		// EnvSpec
		// SampleSpec
		// BusSpec
		// ScaleSpec
		// ArraySpec
		
		if(spec.isKindOf(TrigSpec),{
			^MxTrJack.new
		});
		if(spec.isKindOf(ControlSpec),{
			^MxKrJack.new.value_(defArg ? spec.default)
			//^KrNumberEditor(defArg ? spec.default,spec)
		});
		
	}
}


MxKrJack : MxJack {

	var <value,<patchOut;
	
	*new { arg v;
		^super.new.value_(v)
	}
	storeArgs {
		^[value]
	}
	value_ { arg v;
		value = v;
		this.changed;
	}
	setValueToBundle { arg v,bundle;
		bundle.addFunction({
			value = v;
		});
		patchOut.connectedTo.do { arg patchIn;
			bundle.add( patchIn.nodeControl.setMsg(v) );
		}
	}
		
	addToSynthDef {  arg synthDef,name;
		synthDef.addKr(name,value);
	}
	instrArgFromControl { arg control;
		^control
	}
	makePatchOut {
		patchOut = UpdatingScalarPatchOut(this,enabled: false);
	}
	connectToPatchIn { arg patchIn,needsValueSetNow = true;
		patchOut.connectTo(patchIn,needsValueSetNow);
	}	
}


MxArJack : MxKrJack {
	
	var <value, <>numChannels=2;
	var patchOut;
	
	*new { arg numChannels=2,bus=126;
		^super.new.numChannels_(numChannels).value_(bus)
	}
	storeArgs {
		^[numChannels]
	}
	bus_ { arg v;
		if(v.isSimpleNumber,{
			value = v;
		},{
			value = v.index
		});
		this.changed;
	}
	addToSynthDef {  arg synthDef,name;
		synthDef.addKr(name,value);
	}
	instrArgFromControl { arg control;
		^In.ar(control,numChannels)
	}
}


MxIrJack : MxKrJack {

	addToSynthDef {  arg synthDef,name;
		synthDef.addIr(name,value);
	}
	instrArgFromControl { arg control;
		^control
	}
	makePatchOut {
		patchOut = ScalarPatchOut(this);
	}
	connectToPatchIn { } // nothing doing.  we are ir only
}


MxTrJack : MxKrJack {
	
}


// not yet
/*
MxBufferJack : MxJack {
	
}


MxFFTJack : MxBufferJack {
	
}


MxArrayJack : MxJack {
	
}


MxEnvJack : MxArrayJack {
	
}

*/

