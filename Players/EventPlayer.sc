

// when playEvent is called it spawns the event
// otherwise silent
EventPlayer : AbstractPlayer {
	
	var <>postFilter,<>protoEvent,<>spec;
	var postStream,<>verbose=false;
	
	*new { arg postFilter,protoEvent,spec=\audio;
		^super.new.init(postFilter,protoEvent).spec_(spec.asSpec)
	}
	storeArgs { ^[postFilter.enpath,protoEvent.enpath,spec] }
	init { arg pf,pe;
		postFilter = pf;
		protoEvent = pe ? Event.default;
	}		
	playEvent { arg event;
		var e;
		e = protoEvent.copy.putAll(event);
		if(postStream.notNil,{
			e = postStream.next(e)
		});
		e.play;
		if(verbose,{ e.debug });
		^e
	}
	postFilterPut { arg k,v;
		var pf;
		pf = (postFilter ?? { postFilter = Event.new});
		if(v.notNil,{
			pf.put(k,v);
			if(this.isPlaying,{
				v.prepareForPlay(this.group,true)
			});
		},{
			pf.removeAt(k)
		});
		this.resetProtoStream;
	}
	loadDefFileToBundle {}
	spawnToBundle { arg b;
		this.resetProtoStream;
		b.addFunction({
			this.prSetStatus(\isPlaying)
		})
	}
	resetProtoStream {
		postStream = nil;
		if(postFilter.size > 0,{
			postStream = Pbind(*postFilter.getPairs).asStream
		});
		protoEvent['group'] = this.group;
		protoEvent['bus'] = this.bus;
	}
	stopToBundle { arg b;
		b.addFunction({
			this.prSetStatus(\isStopped)
		})
	}
	isPlaying { ^(status == \isPlaying) }
}


// plays the events at their \beat
// or when playEventAt(i) is called
EventListPlayer : EventPlayer {
	
	var <events;
	var sched;
	
	*new { arg events,spec=\audio,postFilter,protoEvent;
		^super.new(postFilter,protoEvent,spec).initElp.events_(events)
	}
	storeArgs { ^[events.enpath,spec,postFilter.enpath,protoEvent.enpath] }
	initElp {
		sched = BeatSched.new;
	}
	events_ { arg evs;
		events = evs ? [];
	}
	spawnToBundle { arg bundle;
		bundle.addFunction({
			sched.beat = 0.0;
			this.schedAll;
		});
		super.spawnToBundle(bundle);
		// TODO spawn event on 0.0 ?
	}
	stopToBundle { arg b;
		b.addFunction({
			sched.clear
		});
		super.stopToBundle(b)
	}
	schedAll {
		sched.clear;
		events.do { arg ev,i;
			if(ev[\beat].notNil,{
				sched.schedAbs(ev[\beat],{ this.playEvent(ev) })
			})
		};
	}
	addEvent { arg ev;
		events = events.add(ev);
		if(this.isPlaying,{
			if(ev['beat'].notNil,{
				sched.schedAbs(ev[\beat], { this.playEvent(ev) })
			})
		})
	}
	removeEvent { arg ev;
		events.remove(ev);
		if(ev['beat'].notNil and: {ev['beat'] >= sched.beat},{
			this.schedAll;
		})
	}
	playEventAt { arg i,inval;
		var te;
		te = events[i];
		if(te.notNil,{
			this.playEvent(te,inval)
		})
	}
	playEvent { arg event,inval;
		var e;
		e = protoEvent.copy.putAll(event);
		if(postStream.notNil,{
			e = postStream.next(e)
		});
		if(inval.notNil,{
			e.putAll(inval)
		});
		e.play;
		if(verbose,{ e.asCompileString.postln; "".postln; });
		^e
	}
	
	getEventBeat { arg i;
		^events[i][\beat]
	}
	setEventBeat { arg i,beat;
		events[i].put(\beat,beat);
		if(this.isPlaying,{
			this.schedAll;
		})
	}
	beatDuration {
		^events.maxValue({ arg e; e[\beat] ? 0 })
	}
	gotoBeat { arg beat,atBeat,bundle;
		bundle.addFunction({
			sched.beat = beat;
			this.schedAll
		})	
	}
	sorted {
		^events.sort({ arg a,b; (a[\beat]?inf) <= (b[\beat]?inf) })
	} 	
	guiClass { ^EventListPlayerGui }
}


// specialized for instr events only
InstrEventListPlayer : EventListPlayer {
	
	instrArgs {
		var an;
		an = IdentityDictionary.new;
		events.do { arg e;
			var instr;
			if(e['instr'].notNil,{
				instr = e['instr'].asInstr;
				instr.argNames.do { arg aname,i;
					an.put(aname, instr.specs.at(i) )
				}
			})
		};
		^an
	}
	guiClass { ^InstrEventListPlayerGui }
}


