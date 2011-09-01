

MxJack {
	
	*forSpec { arg spec,defArg;
		if(defArg.isKindOf(MxJack),{
			^defArg
		});
		if(spec.isKindOf(AudioSpec),{
			^MxArJack.new.value_(defArg ? 127)
		});
		// EnvSpec
		// SampleSpec
		// BusSpec
		// ScaleSpec
		// ArraySpec
		
		if(spec.isKindOf(TrigSpec),{
			^MxTrJack.new
		});
		if(spec.isKindOf(StaticSpec),{
			^NumberEditor(defArg ? spec.default,spec)
		});
		if(spec.isKindOf(NamedIntegersSpec),{
			^spec.defaultControl(defArg)
		});
		if(spec.isKindOf(ControlSpec),{
			^MxKrJack.new.value_(defArg ? spec.default).spec_(spec)
		});
		^defArg
	}
}


MxKrJack : MxJack {

	var <value,<patchOut,<>spec,<>lag=0.1;
	
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
	
	getIndex { arg patchIn;
		^patchIn.instVarAt('index')
	}
	readFromBusToBundle { arg bus, bundle;
		patchOut.connectedTo.do { arg patchIn;
			bundle.add( patchIn.nodeControl.node.mapMsg(patchIn.nodeControl.name,bus) );
		}
	}
	stopReadFromBusToBundle { arg bundle;
		patchOut.connectedTo.do { arg patchIn;
			bundle.add( patchIn.nodeControl.node.mapMsg(patchIn.nodeControl.name,-1) );
		}
	}		
		
	addToSynthDef {  arg synthDef,name;
		synthDef.addKr(name,value);
	}
	instrArgFromControl { arg control;
		// actually if its patched up to a kr on the server
		// then you don't want Lag
		// this assumes you are sending values from client
		if(lag.notNil and: {spec.isKindOf(NoLagControlSpec).not},{
			^Lag.kr(control,lag)
		},{
			^control
		})
	}
	makePatchOut {
		patchOut = UpdatingScalarPatchOut(this,enabled: false);
	}
	connectToPatchIn { arg patchIn,needsValueSetNow = true;
		patchOut.connectTo(patchIn,needsValueSetNow);
	}
	guiClass { ^MxKrJackGui }
}


MxArJack : MxKrJack {
	
	var <>numChannels=2;
	
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
	guiClass { ^MxArJackGui }
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

	instrArgFromControl { arg control;
		^control
	}
	
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

