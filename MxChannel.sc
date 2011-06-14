

MxChannel : AbstractPlayerProxy {

	classvar cInstr;

	var <id, <>to=nil, <db=0, <mute=false, <solo=false;
	var <>limit=nil, <>breakOnBadValues=true, <>breakOnDbOver=12;
	var <>units;

	var  <>pending=false;

	var <numChannels=2,busJack, <>inlet,dbJack;

	*new { arg id, to, units, db=0.0, mute=false, solo=false,
			limit=nil, breakOnBadValues=true, breakOnDbOver=12.0;
		^super.new.init(id,to,units ? [],db,mute,solo,limit,breakOnBadValues,breakOnDbOver)
	}
	init { arg argid,argto,argunits,argdb,argmute,argsolo,
			arglimit,argbreakOnBadValues,argbreakOnDbOver;
		id = argid;
		to = argto;
		units = argunits;
		db = argdb;
		mute = argmute;
		solo = argsolo;
		limit = arglimit;
		breakOnBadValues = argbreakOnBadValues;
		breakOnDbOver = argbreakOnDbOver;
		numChannels = units.maxValue(_.numChannels) ? 2;
		busJack = NumberEditor(126,StaticIntegerSpec(1, 128, 1, ""));
		dbJack = KrNumberEditor(db,\db);
		
		this.makeSource;
	}
	at { arg index;
		^units[index]
	}
	put { arg index,unit;
		units[index] = unit;
	}
	removeAt { arg index;
		^units.removeAt(index)
	}
	makeSource {
		source = Patch(MxChannel.channelInstr,[
					numChannels,
					busJack,
					dbJack,
					limit ? 0,
					breakOnBadValues.binaryValue,
					breakOnDbOver
				])
	}
	children {
		^super.children ++ units
	}
	prepareToBundle { arg agroup,bundle,private, bus;
		// if has a to-destination then play on a private bus
		// and the Mx will cable it to that destination
		// else play on public/main out => this is a master channel
		group = Group(agroup,\addToTail);
		units.do { arg unit;
			unit.prepareToBundle(group,bundle,true)
		};
		^super.prepareToBundle(group,bundle,to.notNil,bus)
	}
	
	loadDefFileToBundle { arg b,server;
		this.children.do(_.loadDefFileToBundle(b,server))
	}		
	spawnToBundle { arg bundle;
		units.do(_.spawnToBundle(bundle));
		super.spawnToBundle(bundle);
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
			cInstr = Instr("MxChannel.channelInstr",{ arg numChannels=2,inBus=126,
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



