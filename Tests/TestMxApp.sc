
MxAppTester : UnitTest {

	var x,instr;
	
	setUp {
		x = MxApp(Mx.new);
		instr = Instr("_test.SinOsc",{ arg freq=440; SinOsc.ar(freq) });
		super.setUp()
	}
}

TestMxChannelApp : MxAppTester {
	
	test_at_empty {
		var u;
		u = x.channel(0).at(0);
		this.assertEquals( u.class,Nil );
	}
	test_put {
		var u,v;
		u = x.channel(0).put(0, instr );
		this.assertEquals( u.class,MxUnitApp );
		v = x.channel(0).at(0);
		this.assert( u === v );
	}
	test_add {
		var u;
		u = x.channel(0).add( instr );
		this.assertEquals( u.class,MxUnitApp );
	}
	test_add_several {
		var u;
		u = x.channel(0).add( instr, instr, instr );
		this.assert( u.isSequenceableCollection );
		this.assert( u.every({ arg u; u.class == MxUnitApp }) );
	}
	test_removeAt {
		var u;
		u = x.channel(0).put(2, instr );
		x.channel(0).removeAt(2)
	}
	test_dup {
		var u,v;
		x.channel(0).put(2, instr );
		u = x.channel(0).dup(2);
		v = x.channel(0).at(3);
		this.assert( u === v, "dupped unit 2 should be in 3 now")
	}
	test_channelNumber {
		this.assertEquals( x.channel(0).channelNumber, 0 );
		this.assertEquals( x.channel(3).channelNumber, 3 );
		this.assertEquals( x.master.channelNumber, inf );
	}
}
