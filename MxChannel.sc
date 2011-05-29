

MxChannel : AbstractPlayerProxy {
	
	classvar cInstr;
	
	var <id, <>to=nil, <>db=0, <>mute=false, <>solo=false;
	var <>limit=nil, <>breakOnBadValues=true, <>breakOnDbOver=12;
	var <>units;
	
	var  <>pending=false;
	
	var group,bus;
	
	*new { arg id, to, units, db=0.0, mute=false, solo=false,
			limit=nil, breakOnBadValues=true, breakOnDbOver=12.0;
		^super.new.init(id,to,units,db,mute,solo,limit,breakOnBadValues,breakOnDbOver)
	}
	init { arg argid,argto,argunits,argdb,argmute,argsolo,arglimit,argbreakOnBadValues,argbreakOnDbOver;
		id = argid;
		to = argto;
		units = argunits;
		db = argdb;
		mute = argmute;
		solo = argsolo;
		limit = arglimit;
		breakOnBadValues = argbreakOnBadValues;
		breakOnDbOver = argbreakOnDbOver;
		source = Patch(MxChannel.channelInstr)
		// input is tricky
		// need a mixer in the channelInstr that can adapt to listen to multiple busses
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
	*channelInstr {
		// but PlayerSocket already has these traps in place
		^cInstr ?? {
			cInstr = Instr("MxChannel.channelInstr",{ arg audio,db=0,limit=0.999,breakOnBadValues=1,breakOnDbOver=12;
						var in,ok,threshold,c,k;
						in = audio;
						if(breakOnBadValues > 0,{
							ok = BinaryOpUGen('==', CheckBadValues.kr(in, 0, 2), 0);
							(1.0 - ok[0]).onTrig({
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
						NumChannels.ar(in,2)
				    },[
						AudioSpec(2),
						\db,
						StaticSpec(0,1.0),
						StaticSpec(0,1),
						StaticSpec(0,100)
					],AudioSpec(2))
		}
	}					
}



