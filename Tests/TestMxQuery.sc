

TestMxQuery : MxAppTester {
	
	u {
		^x.channel(2).put(2, Instr("_test.u",{ arg freq; Saw.kr(freq) },[\freq],['bipolar']) );
	}
	v {
		^x.channel(3).put(3, Instr("_test.v",{ arg freq,amp=1.0; Saw.ar(freq) * amp },[\freq],['audio']) );
	}
	q {
		this.u;
		^x.units
	}
	test_get_query {
		this.assert(this.q.isKindOf(MxQuery))
	}
	test_size {
		this.assertEquals(this.q.size,1)
	}
	
    test_select {
		var q,instrs,nils;
		q = this.q;
		
		instrs = q.select({ arg obj; obj.isKindOf(Instr) });
		nils = q.select({ arg obj; obj.isNil });

		this.assertEquals(instrs.size,1);
		this.assertEquals(nils.size,0);
		this.assert( q.at(0).isKindOf(MxUnitApp) );
		this.assert( q.at(0).source.isKindOf(Instr) );
	}
	test_reject {
		var q,instrs,nils;
		q = this.q;
		
		instrs = q.reject({ arg obj; obj.isKindOf(Instr) });
		nils = q.reject({ arg obj; obj.isKindOf(Symbol) });

		this.assertEquals(instrs.size,0);
		this.assertEquals(nils.size,1);
	}
	test_mx_channels {
		var q;
		q = x.channels;
		this.assert(q.class == MxQuery);
	}
	test_units {
		var q,units;
		this.u;
		q = x.channels;
		units = q.units;
		this.assertEquals(units.size,1);
	}
	test_units_of_mx {
		var units;
		units = this.q.units;
		this.assertEquals(units.size,1);
	}
	test_units_of_iolets {
		var inlets,units;
		inlets = this.q.inlets;
		units = inlets.units;
		this.assertEquals(units.size,1);
	}
	test_units_of_inlet {
		var inlets,units,q;
		inlets = this.q.inlets;
		inlets = inlets.where('name','freq');
		units = inlets.units;
		this.assertEquals(units.size,1);
	}
	test_inlets {
		var inlets,units;
		inlets = this.q.inlets;
		this.assertEquals(inlets.size,1);
	}
	test_outlets {
		var inlets,units;
		inlets = this.q.outlets;
		this.assertEquals(inlets.size,1);
	}
	test_channels {
		var chans,units;
		chans = this.q.channels;
		this.assertEquals(chans.size,1);
	}
	test_where {
		var inlets,units,q;
		inlets = this.q.inlets;
		inlets = inlets.where('name','freq');
		this.assertEquals(inlets.size,1);
	}
	test_whereIsA {
		var q;
		q = this.q.whereIsA(Instr);
		this.assertEquals(q.size,1);
		
		q = this.q.whereIsA(Symbol);
		this.assertEquals(q.size,0);
	}
	/*
    whereIsKindOf { arg class;
		^MxQuery(this.select({ arg obj,i,app; obj.isKindOf(class) }),mxapp)
	}
	whereAppClassIs { arg class;
		^MxQuery(this.select({ arg obj,i,app; app.class === class }),mxapp)
	}*/

	test_connect_to_inlet {
		var q,qv,v;
		q = this.q.units.select({ arg instr; instr.dotNotation == "_test.u" }); // just u
		// add v to mx
		v = this.v;
		q >> v.i.amp
	}

	test_disconnect_unit {
		var q;
		this.u >> this.v.i.amp;
		
		q = this.q.select({ arg instr; instr.dotNotation == "_test.u" }); // just u
		q.disconnect
	}
	test_disconnect_outlets {
		var q;
		this.u >> this.v.i.amp;
		
		q = this.q.select({ arg instr; instr.dotNotation == "_test.u" }); // just u
		q = q.outlets;
		q.disconnect
	}

	test_remove {
		var q;
		this.u;
		this.v;
		q = this.q.select({ arg instr; instr.dotNotation == "_test.u" }); // just u
		q.remove
	}

	test_get {
		var results;
		results = this.q.get('name'); // gets from source
		this.assertEquals(results.size,1);
		this.assertEquals(results[0],this.u.source.name);
	}
	/*set { arg selector,value;
		// set a single value to all source objects
		this.do({ arg obj; obj.perform(selector,value) })
	}*/

	// units
	test_dup {
		this.q.dup
	}
	test_moveBy {
		var u,uu,moved,p,newPoint,v;
		u = this.u;
		uu = x.at(u.point);
		//uu.insp("uu");
		p = u.point;
		v = 1@1;
		//uu.insp("uu");

		//"MOVING".debug;
		this.q.moveBy(v);
		//uu.insp("uu after move");
		// it is not found in channels ?
		x.source.gui;
		
		newPoint = p + v;
		moved = x.at(newPoint);
		//[moved,uu,moved === uu,moved.model === uu.model].insp;
		this.assertEquals(moved,uu);
		this.assertEquals( x.at(p), nil , "unit should not be in old position")
	}
	test_copyBy { arg vector;
		var u,moved,p,newPoint,v,old;
		u = this.u;
		p = u.point;
		v = 1@1;
		this.q.copyBy(v);
		newPoint = p + v;
		moved = x.at(newPoint);
		old = x.at(p);
		this.assertEquals(old,u,"original should still be there");
		this.assert(moved.notNil,"should be a new unit in copied position")
	}
	
	// channels
	test_mute {
		var c,u;
		u = this.u;
		c = x.channel(u.point.x).insp;
		this.q.mute;
		this.assertEquals( c.muted, true );
		this.q.mute(false);
		this.assertEquals( c.muted, false );
	}
	test_solo {
		var c,u;
		u = this.u;
		c = x.channel(u.point.x);
		this.q.solo;
		this.assertEquals( c.soloed, true );
		this.q.solo(false);
		this.assertEquals( c.soloed, false );
	}
	test_db {
		var c,u;
		u = this.u;
		c = x.channel(u.point.x);
		this.assertEquals( c.db, 0 );
		this.q.db = -4;
		this.assertEquals( c.db, -4 );
	}
}
