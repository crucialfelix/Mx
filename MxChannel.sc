

MxChannel : AbstractPlayerProxy {

	classvar cInstr;

	var <id, <>to=nil, <db=0, <mute=false, <solo=false;
	var <>limit=nil, <>breakOnBadValues=true, <>breakOnDbOver=12;
	var <>units;
	
	var <numChannels=2,<>pending=false;

	var <bus,busJack,dbJack,<>myUnit,unitGroups,<mixGroup;
	var adding,removing;

	*new { arg id, to, units, db=0.0, mute=false, solo=false,
			limit=nil, breakOnBadValues=true, breakOnDbOver=12.0,mx;
		^super.new.init(id,to,units ? [],db,mute,solo,limit,breakOnBadValues,breakOnDbOver,mx)
	}
	storeArgs { ^[id,to,units.collect(_.saveData),db,mute,solo,limit,breakOnBadValues,breakOnDbOver] }

	init { arg argid,argto,argunits,argdb,argmute,argsolo,
			arglimit,argbreakOnBadValues,argbreakOnDbOver,mx;
		id = argid;
		to = argto;
		units = argunits.collect { arg u;
					if(u.isKindOf(MxUnit),{
						u
					},{
						MxUnit.loadData(u.add(mx))
					});
				};
		db = argdb;
		mute = argmute;
		solo = argsolo;
		limit = arglimit;
		breakOnBadValues = argbreakOnBadValues;
		breakOnDbOver = argbreakOnDbOver;
		numChannels = units.maxValue(_.numChannels) ? 2;
		// change these to MxKrJack
		busJack = NumberEditor(126,StaticIntegerSpec(1, 128, 1, ""));
		dbJack = KrNumberEditor(db,\db);
		this.makeSource;
	}
	at { arg index;
		^units[index]
	}
	put { arg index,unit;
		this.removeAt(index);
		if(units.size < (index+1), {
			units = units.extend(index+1,nil)
		});
		units[index] = unit;
		adding = adding.add(unit);
	}
	removeAt { arg index;
		var old;
		old = units.at(index);
		if(old.notNil,{
			removing = removing.add(old)
		});
		units.put(index,nil);
		^old
	}
	makeSource {
		source = Patch(MxChannel.channelInstr,[
					numChannels,
					busJack,
					dbJack,
					limit ? 0,
					breakOnBadValues.binaryValue,
					breakOnDbOver
				]);
	}
	children {
		^super.children ++ units
	}
	prepareToBundle { arg parentGroup,bundle,private, bus,groupToUse;
		// if has a to-destination then play on a private bus
		// and the Mx will cable it to that destination
		// else play on public/main out => this is a master channel
		group = groupToUse ?? {
			group = Group.basicNew(parentGroup.server);
			bundle.add( group.addToTailMsg(parentGroup) );
			group
		};
		
		unitGroups = units.collect { arg u,i;
			var g;
			if(u.notNil,{
				g = Group.basicNew(this.server);
				bundle.add( g.addToTailMsg(group) );
			});
			g
		};
		mixGroup = Group.basicNew(this.server);
		bundle.add( mixGroup.addToTailMsg(group) );
		// prepares the children
		source.prepareToBundle(mixGroup,bundle,to.notNil,bus);
		units.do { arg unit,i;
			unit.prepareToBundle(unitGroups[i],bundle,true)
		};
	}
	prepareChildrenToBundle { arg bundle;}	
	loadDefFileToBundle { arg b,server;
		this.children.do(_.loadDefFileToBundle(b,server))
	}	
	spawnToBundle { arg bundle;
		units.do(_.spawnToBundle(bundle));
		super.spawnToBundle(bundle);
		adding = removing = nil;
	}
	freeToBundle { arg bundle;
		super.freeToBundle(bundle);
		if(mixGroup.notNil,{
			bundle.add( mixGroup.freeMsg );
		});
		unitGroups.do { arg u;
			bundle.add( u.freeMsg );
		};
		bundle.addFunction({
			mixGroup = unitGroups = nil;
		});
	}
	update { arg bundle;
		removing.do { arg unit;
			// leaves groups running
			// they are likely to get refilled
			unit.freeToBundle(bundle);
		};
		adding.do { arg unit;
			var ui,ug;
			ui = units.indexOf(unit);
			ug = this.groupForIndex(ui,bundle);
			unit.prepareToBundle(ug,bundle,true);
			unit.spawnToBundle(bundle);
		};
		bundle.addFunction({
			removing = adding = nil;
		})
	}
	groupForIndex { arg index,bundle;
		// make a group on demand if needed
		var g,prev;
		unitGroups.at(index) ?? {
			g = Group.basicNew(this.server);
			prev = unitGroups.copyRange(0,index).reverse.detect(_.notNil);
			if(prev.notNil,{
				bundle.add( g.addAfterMsg(prev) )
			},{
				bundle.add( g.addToHeadMsg(group) )
			});
			g
		}
	}
	db_ { arg d;
		db = d;
		dbJack.value = db;
		dbJack.changed;
	}
	mute_ { arg boo;
		mute = boo;
		if(mute,{
			dbJack.value = -180;
			dbJack.changed;
		},{
			dbJack.value = db;
			dbJack.changed;
		})
	}
	solo_ { arg boo;
		solo = boo;
		if(solo,{
			dbJack.value = db;
			dbJack.changed;
		},{
			dbJack.value = -180;
			dbJack.changed;
		})
	}						
	
	*channelInstr {
		// or create it on the fly so the on trigs can go
		// could also pass those in
		^cInstr ?? {
			cInstr = Instr("MxChannel.channelStrip",{ arg numChannels=2,inBus=126,
									db=0,limit=0.999,breakOnBadValues=1,breakOnDbOver=12;
						var ok,threshold,c,k,in;
						in = In.ar(inBus,numChannels);
						if(breakOnBadValues > 0,{
							ok = BinaryOpUGen('==', CheckBadValues.kr(Mono(in), 0, 2), 0);
							(1.0 - ok).onTrig({
								"bad value, killing".inform;
								//total.app.releaseAll;
							});
							in = in * ok;
						});
						if(breakOnDbOver > 0,{
							threshold = breakOnDbOver.dbamp;
							c = max(0.0,(Amplitude.ar(Mono(in),0.001,0.001) - 2.0));
							k = c > threshold;
							A2K.kr(k).onTrig({
								"amp > threshold, killing".inform;
								//total.app.releaseAll;
							});
							k = 1.0 - k;
							in = in * k; //Lag.kr(k,0.01);
						});
						if(limit > 0,{
							Limiter.ar(
								( in ) * db.dbamp,
								limit
							) // .clip2(1.0,-1.0)
						},{
							in = in * db.dbamp
						});
						NumChannels.ar(in,numChannels)
				    },[
						StaticIntegerSpec(1,128),
						StaticIntegerSpec(1,128),
						\db,
						StaticSpec(0,1.0),
						StaticSpec(0,1),
						StaticSpec(0,100)
					],AudioSpec(2))
		}
	}
}



