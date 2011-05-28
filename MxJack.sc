

MxJack {
	
	*forSpec { arg spec;
		if(spec.isKindOf(AudioSpec),{
			^MxArJack.new
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
			^MxKrJack.new
		});
		
		\trigger.asSpec.rate
	}
}


MxArJack : MxJack {
	
	
}


MxKrJack : MxArJack {
	
}


MxTrJack : MxKrJack {
	
}


MxBufferJack : MxJack {
	
}


MxFFTJack : MxBufferJack {
	
}


MxArrayJack : MxJack {
	
}


MxEnvJack : MxArrayJack {
	
}

