
/*
	requires UnitTesting
*/

TestMxLoader : UnitTest {
	
	test_loadData {
		
		var mx,data,loader;
		
		mx = Mx.new;
		
		data = mx.storeArgs[0];
		mx = Mx(data);

	}
	
	test_doubleLoad {
		var mx,data,loader;
		
		mx = Mx.new;
		
		data = mx.storeArgs[0];
		mx = Mx(data);

		data = mx.storeArgs[0];
		mx = Mx(data);
	}
	
}


TestMxUnit : UnitTest {
	
	test_handlersForPlayer {
		var p,h;
		p = MxChannelInput.new;
		h = MxUnit.handlersFor(p.class);
		// know is set
		h.name;
	}
	test_handlersForPlayerInherited {
		var p,h;
		p = InstrEventListPlayer.new;
		h = MxUnit.handlersFor(p.class);
	}
	test_handlersForInstr {
		var h,p;
		h = MxUnit.handlersFor(Instr);
		this.assert( h['name'].isKindOf(Function), "'name' should be implemented in Instr handlers");
		// unless Instr did implement it later
		this.assert( h['beatDuration'].isKindOf(Function), "'beatDuration' from protoHandler should be found in Instr handlers");

		p = Instr("_test_.Sin",{ SinOsc.ar });
		h.use {
			~make.value(p);
			this.assert( ~patch.notNil,"The correct ~make from Instr.scd should run, and ~patch should be set");
		}
	}
}

TestMx : UnitTest {
	
	test_put {
		var x,p;
		x = Mx.new;
		p = Instr("_test_.Sin",{ SinOsc.ar });
		x.put(0,0,p);
	}
	test_mx_is_set {
		var x,p,unit;
		x = Mx.new;
		p = Instr("_test_.Sin",{ SinOsc.ar });
		x.put(0,0,p);
		unit = x.at(0,0);
		unit.use {
			this.assert( ~mx === x,"Unit's ~mx should be set to the mx that it has been added to");
		}
	}
	
}