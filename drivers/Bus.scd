

MxUnit.register('Bus',(

	make: { arg bus;
		
		var inlets,outlets;
		
		~source = bus;
		
		inlets = [ MxInlet("in",0,AudioSpec(bus.numChannels), MxListensToBus({bus}) ) ];
		outlets = [ MxOutlet( "out", 0, AudioSpec(bus.numChannels), MxPlaysOnBus({bus}) ) ];
		
		MxUnit(bus,inlets,outlets);
	}
));
