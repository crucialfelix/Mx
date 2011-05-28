

MxCable {
	
	classvar strategies;
	
	var <>outlet,<>inlet,<>mapping,<>active=true,<>pending=false;
	

	*register { arg outAdapterClassName,inAdapterClassName, function;
		strategies[ outAdapterClassName -> inAdapterClassName] = function
	}
	*initClass {
		strategies = Dictionary.new;
		
	}	
}


MxAutoCable : MxCable {}


MxCableMapping {
	
	var <>mapToSpec,<>mapCurve,<>enabled=false;
	
}

