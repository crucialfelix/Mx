

MxChannel : AbstractPlayerProxy {

	var <>cableTo=nil;
	var <>units,<fader;
	
	var <numChannels=2,<>pending=false;

	var <myUnit,unitGroups,<mixGroup;
	var adding,removing;

	*new { arg units, faderArgs, cableTo;
		var fader;
		units = units ? [];
		if(faderArgs.isNil,{
			fader = MxChannelFader(numChannels:units.maxValue(_.numChannels) ? 2)
		},{
			fader = MxChannelFader(*faderArgs)
		});
		^super.new.init(cableTo,units,fader)
	}
	storeArgs { 
		^[units.collect({|u| u !? {u.saveData}}),fader.storeArgs, cableTo] 
	}

	init { arg argto,argunits,argfader;
		cableTo = argto;
		units = argunits;
		fader = argfader;
		source = fader; // 2 proxy layers
		myUnit = MxUnit.make(this);
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
		if(units.size > index,{
			old = units.at(index);
			if(old.notNil,{
				removing = removing.add(old)
			});
			units.put(index,nil);
		})
		^old
	}
	move { arg fromIndex,toIndex;
		var old;
		// on the update this removes then adds
		old = this.removeAt(fromIndex);
		if(old.notNil,{
			this.put(toIndex,old)
		})
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
		source.prepareToBundle(mixGroup,bundle,cableTo.notNil,bus);
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
			if(u.notNil,{
				bundle.add( u.freeMsg );
			});
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
	bus {
		^source.bus
	}
	groupForIndex { arg index,bundle;
		// make a group on demand if needed
		var g,prev;
		^unitGroups.at(index) ?? {
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
		fader.db = d;
	}
	mute_ { arg boo;
		fader.mute = boo
	}
	solo_ { arg boo;
		fader.solo = boo
	}
}



