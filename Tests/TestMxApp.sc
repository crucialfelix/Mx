
MxAppTester : UnitTest {

	var x,instr;
	
	setUp {
		x = MxApp(Mx.new);
		instr = Instr("_test.SinOsc",{ arg freq=440; SinOsc.ar(freq) },[],\audio);
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
	outlet_to_fader {
		var f,u;
		u = x.channel(0).put(2,instr);
		f = x.channel(0).fader;
		u.o.first >> f;
	}
	test_unit_to_fader {
		var f,u;
		u = x.channel(0).put(2,instr);
		f = x.channel(0).fader;
		u >> f;
	}
	test_fader2 {
		var f,u;
		u = x.channel(0).put(2,instr);
		f = x.channel(0).fader;
		u >> f;
	}
}

TestMxUnitApp : MxAppTester {
	
	u {
		^x.channel(0).put(0, instr );
	}
	test_remove {
		this.u.remove
	}
	test_moveTo {
		var u;
		u = this.u;
		u.moveTo(1@1);
		u.moveTo(0@1);
	}
	test_inlets {
		this.u.inlets
	}
	test_outlets {
		this.u.outlets
	}
	test_i {
		var u;
		u = this.u;
		this.assertEquals( u.i.at(\freq).class, MxInletApp );
		this.assertEquals( u.i.at(0).class, MxInletApp );
		this.assertEquals( u.i.freq.class, MxInletApp );
	}
	test_o {
		var u;
		u = this.u;
		this.assertEquals( u.o.at('audio').class,MxOutletApp);
		this.assertEquals( u.o.at(0).class,MxOutletApp);
		this.assertEquals( u.o.out.class,MxOutletApp);
	}
	test_channel {
		var chan;
		chan = this.u.channel;
		this.assertEquals(chan.class,MxChannelApp);
		this.assertEquals(chan.channelNumber,0);
	}
	test_point {
		var p;
		p = this.u.point;
		this.assertEquals(p,0@0);
	}
	test_asString {
		this.u.asString
	}
	test_dup {
		var u,v;
		u = this.u;
		v = u.dup;
		this.assert(v.notNil);
	}
	test_dup_in_transaction {
		var u,v;
		u = this.u;
		x.transaction {
			v = u.dup;
			this.assert(v.notNil);
		};
		//x.source. removing adding should be empty
	}
	test_copy {
		var u,v;
		u = this.u;
		v = u.copy(u.point + Point(1,1));
		this.assert(v.notNil);
	}
}

TestMxInletApp : MxAppTester {

	u {
		^x.channel(2).put(2, Instr("_test.u",{ arg freq; Saw.kr(freq) },[\freq],['bipolar']) );
	}
	v {
		^x.channel(3).put(3, Instr("_test.v",{ arg freq,amp=1.0; Saw.ar(freq) * amp },[\freq],['audio']) );
	}
	test_connect_out {
		this.u.o.out >> this.v.i.freq
	}
	test_connect_in{		
		this.v.i.amp << this.u.o.out
	}
}


	