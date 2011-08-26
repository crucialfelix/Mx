

MxChannelFader : AbstractPlayerProxy {

	classvar cInstr;
	
	var <numChannels,<db=0, <mute=false, <solo=false;
	var <>limit=nil, <>breakOnBadValues=true, <>breakOnDbOver=12;
	
	var busJack,dbJack;
	
	*new { arg db=0.0, mute=false, solo=false,
			limit=nil, breakOnBadValues=true, breakOnDbOver=12.0,numChannels=2;
		^super.new.init(db,mute,solo,limit,breakOnBadValues,breakOnDbOver,numChannels)
	}
	storeArgs {
		^[db,mute,solo,limit,breakOnBadValues,breakOnDbOver,numChannels]
	}
				
	init { arg argdb,argmute,argsolo,
			arglimit,argbreakOnBadValues,argbreakOnDbOver,argnumChannels;
		db = argdb;
		mute = argmute;
		solo = argsolo;
		limit = arglimit;
		breakOnBadValues = argbreakOnBadValues;
		breakOnDbOver = argbreakOnDbOver;
		numChannels = argnumChannels ? 2;
		// change these to MxKrJack
		busJack = NumberEditor(126,StaticIntegerSpec(1, 128, 1, ""));
		dbJack = KrNumberEditor(db,\db);

 		source = Patch(MxChannelFader.channelInstr,[
					numChannels,
					busJack,
					dbJack,
					limit ? 0,
					breakOnBadValues.binaryValue,
					breakOnDbOver
				]);
	}
	db_ { arg d;
		db = d;
		dbJack.value = db;
		dbJack.changed;
	}
	mute_ { arg boo;
		mute = boo;
		if(mute,{
			dbJack.value = -300.0;
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
			dbJack.value = -300.0;
			dbJack.changed;
		})
	}
	
	draw { arg pen,bounds,style;
		pen.color = style['fontColor'];
		pen.font = style['font'];
		if(mute,{
			^pen.stringCenteredIn("muted",bounds);
		},{
			if(solo,{
				^pen.stringCenteredIn("solo",bounds)
			})
		});
		pen.stringCenteredIn(db.round(0.1).asString ++ "dB",bounds);
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

	