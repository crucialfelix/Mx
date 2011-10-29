

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
		if(spec.isKindOf(NoLagControlSpec),{
			^MxKrJack(defArg ? spec.default,spec,nil)
		});
		if(spec.isKindOf(ControlSpec),{
			^MxKrJack(defArg ? spec.default,spec)
		});
		^defArg
	}
}


MxControlJack : MxJack { // abstract

	var <value,<>spec;
	var <patchOut;
	
	storeArgs {
		^[value,spec]
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
	
	getNodeControlIndex { arg patchIn;
		^patchIn.instVarAt('index')
	}
	readFromBusToBundle { arg bus, bundle;
		patchOut.connectedTo.do { arg patchIn;
			bundle.add( patchIn.nodeControl.node.mapMsg(this.getNodeControlIndex(patchIn.nodeControl),bus) );
		}
	}
	stopReadFromBusToBundle { arg bundle;
		patchOut.connectedTo.do { arg patchIn;
			bundle.add( patchIn.nodeControl.node.mapMsg(this.getNodeControlIndex(patchIn.nodeControl),-1) );
		}
	}
	stopToBundle { arg bundle;
		bundle.addFunction({ patchOut.free; patchOut = nil; })
	}

	synthArg {
		^value
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
	rate { ^\control }
}


MxKrJack : MxControlJack {
	
	var <>lag=0.1;
	
	*new { arg value,spec,lag=0.1;
		^super.newCopyArgs(value,spec,lag)
	}
	storeArgs {
		^[value,spec,lag]
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
	guiClass { ^MxKrJackGui }	
}


MxArJack : MxControlJack {
	
	/*
		value is the bus
	*/
	
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
	rate { ^\audio }
	
	guiClass { ^MxArJackGui }
}


MxIrJack : MxControlJack {

	addToSynthDef {  arg synthDef,name;
		synthDef.addIr(name,value);
	}
	makePatchOut {
		patchOut = ScalarPatchOut(this);
	}
	connectToPatchIn { } // nothing doing.  we are ir only
}


MxTrJack : MxControlJack {

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

