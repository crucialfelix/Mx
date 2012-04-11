

AbsApp {

	var <model,mxapp;
	
	*new { arg model,mxapp;
		^super.newCopyArgs(model,mxapp).prInit
	}
	prInit {}
	mx { ^mxapp.model }
	printOn { arg stream;
		stream << model
	}
}


MxApp : AbsApp {
	
	var cache,convertor;

	mx { ^model }
	at { arg point;
		var u;
		u = model.at(point.x,point.y) ?? { ^nil };
		^this.prFind( u )
	}
	put { arg point,object;
		^this.prFind( model.put(point.x,point.y,object) )
	}
	channel { arg i;
		var c;
		c = model.channelAt(i) ?? {
				model.extendChannels(i);
				model.update;
				model.channelAt(i)
			};
		^this.prFind( c )
	}
	master {
		^this.prFind( model.master )
	}
	
	newChan {
		^this.channel( model.channels.size )
	}
	add { arg ... sources;
		// returns a channel filled with object(s)
		var chan;
		chan = this.prFind( model.add(*sources) );
		model.update;
		this.mx.changed('grid');
		^chan
	}
	
	play { arg then;
		if(model.isPlaying.not,{
			model.onPlay(then).play
		},then)
	}
	stop { arg then;
		if(model.isPlaying,{
			model.onFree(then).free
		},then)
	}

	relocate { arg beat,q=4;
		model.gotoBeat(beat,q);
	}
	
	save {
		model.save
	}
	gui { arg layout,bounds;
		// detect and front
		var open;
		open = model.dependants.detect(_.isKindOf(MxGui));
		if(open.notNil,{
			open.front
		},{
			model.gui(layout,bounds);
		})
	}
	
	//select
	
	//copy to an app buffer
	//paste
	prInit {
		cache = IdentityDictionary.new;
	}
	prFind { arg obj;
		var app;
		^cache[obj] ?? {
			app = (obj.class.name.asString ++ "App").asSymbol.asClass.new(obj,this);
			cache[obj] = app;
			app
		}
	}
	printOn { arg stream;
		if(model.name.notNil,{
			stream << model
		},{
			stream << "an MxApp"
		})
	}
}


MxChannelApp : AbsApp {
	
	at { arg i;
		var unit;
		unit = model.at(i) ?? {
			^nil
			// slot is still nil
			//model.extendUnits(i);
			//model.at(i)
		};
		^mxapp.prFind( unit )
	}
	put { arg i,source;
		this.mx.put( this.channelNumber, i, source );
		this.mx.update;
		this.mx.changed('grid');
		^this.at(i)
	}
	removeAt { arg i;
		this.mx.remove( this.channelNumber, i );
		this.mx.changed('grid');
		this.mx.update
	}
	insertAt { arg i,source;
		// source.asArray.do
		this.mx.insert( this.channelNumber, i, source );
		this.mx.update;
		this.mx.changed('grid');
		^this.at(i)
	}
	
	//unit
	fader {
		// the audio inlet to the fader
		^mxapp.prFind(model.myUnit.inlets.first)
	}
	
	//select
	add { arg ... sources; // add 1 or more to the end
		var start,apps,ci;
		start = model.units.size;
		ci = this.channelNumber;
		apps = sources.collect { arg source,i;
			this.mx.put( ci,start + i, source );
			this.at(i)
		};
		this.mx.update;
		this.mx.changed('grid');
		if(apps.size == 1,{
			^apps.first
		},{
			^apps
		})
	}
	dup { arg fromIndex,toIndex;
		// toIndex defaults to the next slot, pushing any others further down		
		var unit,ci;
		ci = this.channelNumber;
		unit = this.mx.copy( ci, fromIndex, ci, 	toIndex ?? {fromIndex + 1} );
		this.mx.update;
		this.mx.changed('grid');
		if(unit.isNil, { ^nil });
		^mxapp.prFind( unit )
	}
	mute {
		this.mx.mute(this.channelNumber,true);
		this.mx.changed('mixer');
	}
	unmute {
		this.mx.mute(this.channelNumber,false);
		this.mx.changed('mixer');
	}
	toggle {
		this.mx.mute(this.channelNumber,model.fader.mute.not);
		this.mx.changed('mixer');
	}
	solo {
		this.mx.solo(this.channelNumber,true);
		this.mx.changed('mixer');
	}
	unsolo {
		this.mx.solo(this.channelNumber,false);
		this.mx.changed('mixer');
	}
	db {
		^model.fader.db
	}
	db_ { arg db;
		model.fader.db = db;
		this.mx.changed('mixer');
	}
	//fade { arg db,seconds=5; // easing
		// will need a little engine
	//}
	
	channelNumber {
		^this.mx.indexOfChannel(model)
	}
	printOn { arg stream;
		stream << "Channel" << this.channelNumber
	}	
}


MxUnitApp : AbsApp {
	
	name {
		^model.name
	}
	source {
		^model.source
	}
	use { arg function;
		^model.use(function)
	}
	stop {
		model.stop;
		// unit should send state change notifications
		this.mx.changed('grid');
	}
	play {
		model.play;
		this.mx.changed('grid');
	}
	respawn {
		model.respawn
	}
	isPlaying {
		^model.isPlaying
	}
	spec {
		^model.spec
	}
	beatDuration {
		model.beatDuration
	}
	gui { arg layout,bounds;
		^model.gui(layout,bounds)
	}
	
	remove {
		this.mx.remove(*this.point.asArray).update;
		this.mx.changed('grid');
		// should mark self as dead
	}
	moveTo { arg point;
		var me;
		me = this.point;
		this.mx.move(me.x,me.y,point.x,point.y).update;
		this.mx.changed('grid');
	}
	//replaceWith { arg source; // or unit or point
	//}
	//replace(other)
	disconnect {
		model.inlets.do { arg io;
			this.mx.disconnectInlet(io);
		};
		model.outlets.do { arg io;
			this.mx.disconnectOutlet(io);
		};
		this.mx.update;
		this.mx.changed('grid');
	}
	
	i { ^this.inlets }
	o { ^this.outlets }
	inlets {
		^MxIOletsApp(model.inlets,mxapp,model)
	}
	outlets {
		^MxIOletsApp(model.outlets,mxapp,model)
	}
	channel {
		^mxapp.prFind( this.mx.channelAt( this.point.x ) )
	}
	>> { arg that;
		^(this.outlets >> that)
	}
	point { ^this.mx.pointForUnit(model) }
	printOn { arg stream;
		var p;
		p = this.point ?? { stream << model << "(" << this.name << ")"; ^this };
		stream << p.x << "@" << p.y << "(" << this.name << ")"
	}
}


MxIOletsApp : AbsApp {
	
	var <unit,desc;
	
	*new { arg model,mxapp,unit,desc;
		^super.newCopyArgs(model,mxapp,unit,desc).prInit
	}
	
	at { arg key;
		^this.prFindIOlet(key,true)
	}
	first {
		^this.prFindIOlet(0,true)
	}
	out {
		// shortcut to the first output
		^this.prFindIOlet('out') ?? {this.prFindIOlet(0,true)}
	}
	>> { arg inlet;
		^(this.out ?? { (this.asString ++ "has no out").error; ^this }) >> inlet
	}
	// finds iolet by name
	doesNotUnderstand { arg selector ... args;
		^this.prFindIOlet(selector) ?? {
			this.superPerformList(\doesNotUnderstand, selector, args);
		}
	}
	disconnect {
		model.do { arg io;
			if(io.class === MxOutlet,{
				this.mx.disconnectOutlet(io);
			},{
				this.mx.disconnectInlet(io);
			})
		};
		this.mx.update;
		this.mx.changed('grid');
	}
	prFindIOlet { arg i,warn=false;
		if(i.isNumber,{
			if(i >= model.size,{
				if(warn,{
					("In/Out index out of range:"+ i.asCompileString + this).warn;
				});
				^nil
			},{
				^mxapp.prFind(model.at(i))
			})
		},{
			i = i.asSymbol;
			model.do { arg inl;
				if(inl.name == i,{
					^mxapp.prFind(inl)
				})
			}
		});
		if(warn,{
			("In/Out index not found:"+ i.asCompileString + this).warn;
		});
		^nil
	}
	printOn { arg stream;
		stream << mxapp.prFind(unit) << ":" << (", ".join(model.collect(_.name)))
	}
}


MxInletApp : AbsApp {

	<< { arg outlet;
		this.mx.connect(outlet.model.unit,outlet.model,model.unit,model);
		this.mx.update;
		this.mx.changed('grid');
		^outlet
	}
	disconnect {
		this.mx.disconnectInlet(model);
		this.mx.update;
		this.mx.changed('grid');
	}
	spec {
		^model.spec
	}
	name {
		^model.name
	}
	index {
		^model.index
	}
	unit {
		^mxapp.prFind(model.unit)
	}
	from { // outlets that connect to me
		^this.mx.cables.toInlet(model).collect { arg cable;
			mxapp.prFind( cable.outlet )
		}
	}
	printOn { arg stream;
		stream << mxapp.prFind(model.unit) << "::";
		model.printOn(stream)
	}
	// cables
}


MxOutletApp : AbsApp {

	>> { arg inlet;
		/*
		could connect to fader of channel,
		or take connect to channel as meaning connect to top of channel strip
		if(inlet.isKindOf(MxChannelApp),{
			inlet = inlet.fader
		}); */
		this.mx.connect(model.unit,model,inlet.model.unit,inlet.model);
		this.mx.update;
		this.mx.changed('grid');
		^inlet // or magically find the outlet of that unit; or return that unit
	}
	disconnect {
		this.mx.disconnectOutlet(model);
		this.mx.update;
		this.mx.changed('grid');
	}
	
	spec {
		^model.spec
	}
	name {
		^model.name
	}
	index {
		^model.index
	}
	unit {
		^mxapp.prFind(model.unit)
	}
	to { // outlets that connect to me
		^this.mx.cables.fromInlet(model).collect { arg cable;
			mxapp.prFind( cable.inlet )
		}
	}
	printOn { arg stream;
		stream << mxapp.prFind(model.unit) << "::";
		model.printOn(stream)
	}
}


MxQs {
	
	
}


	