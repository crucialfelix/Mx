

MxTimeGui : ObjectGui {
	
	var <from,<to,maxTime,zoomCalc,playZoomCalc;
	var xScale;
	var <>laneHeight=300.0;
	var zoom,playHead,timeRuler,updater,units;
	
	writeName {}
	guiBody { arg layout;
		var i = 0,width, focusColor,kdr;
		layout.startRow;
		width = layout.indentedRemaining.width;
		focusColor = Color.blue(alpha:0.4);
		
		kdr = this.keyDownResponder;

		maxTime = 60 * 5;
		CXLabel(layout,"MaxBeat:");
		NumberEditor(maxTime,[0,10000].asSpec).action_({ arg num;
			this.maxTime = num.value;
			timeRuler.refresh;
			// units
			// zoom is off. but it will be changed to TimeRuler anyway
			// playHead will update itself
		}).smallGui(layout);

		zoomCalc = ZoomCalc([0,maxTime],[0,width]);
		playZoomCalc = ZoomCalc([0,maxTime],[0,1.0]);
		timeRuler = TimeRuler(layout,Rect(0,0,width,25),maxTime);
		timeRuler.keyDownAction = kdr;
		
		this.prSetFromTo(0.0,maxTime);
		
		// zoom controls
		zoom = SCRangeSlider(layout,width@20);
		zoom.lo = 0.0;
		zoom.hi = 1.0;
		zoom.action = {this.zoom((zoom.lo * maxTime).round, (zoom.hi * maxTime).round,false)};
		zoom.knobColor = Color.black;
		zoom.keyDownAction = kdr;
		zoom.focusColor = focusColor;

		playHead = SCSlider(layout,width@10);
		playHead.background = Color(0.36241707801819, 0.55301971435547, 0.60233759880066, 1);
		playHead.knobColor = Color.black;
		playHead.thumbSize = 18;
		playHead.action = { arg mg;
			model.gotoBeat( playZoomCalc.displayToModel(mg.value) )
		};
		playHead.keyDownAction = kdr;
		playHead.focusColor = focusColor;
		updater = Updater(model.position,{ arg pos;
			{
				playHead.value = playZoomCalc.modelToDisplay(pos.current) ? 0;
			}.defer
		}).removeOnClose(layout);
		
		units = [];
		model.channels.do { arg chan,ci;
			chan.units.do { arg unit;
				var v,b;
				if(unit.notNil and: {unit.handlers.at('timeGui').notNil},{
					b = Rect(0,0,width,laneHeight);
					v = FlowView(layout,b,0@0,0@0);
					unit.timeGui(v,b,maxTime);
					v.resizeToFit;
					units = units.add(v);
				})
			}
		};
	}
	prSetFromTo { arg argFrom,argTo;
		from = argFrom;
		to = argTo;
		zoomCalc.setZoom(from,to);
		playZoomCalc.setZoom(from,to);
		timeRuler.setZoom(from,to);
	}
	zoom { arg argFrom,argTo,updateZoomControl=true;
		this.prSetFromTo(argFrom,argTo);
		model.channels.do { arg chan,ci;
			chan.units.do { arg unit;
				if(unit.notNil and: {unit.handlers.at('zoomTime').notNil},{
					unit.zoomTime(from,to);
				})
			}
		};
		if(updateZoomControl,{
			zoom.setSpan( from / maxTime, to / maxTime )
		}) 
	}
	zoomBy { arg percentage,round=4.0; // 0 .. 2
		// zoom up or down, centered on the current middle
		var middle, newrange,newfrom,newto;
		middle = (to - from) / 2.0;
		newto = to - middle * percentage + middle;
		newfrom = middle - (middle - from * percentage);
		//[newfrom,newto].debug("zooming to");
		newfrom = newfrom.clip(0,maxTime - round);
		newto = newto.clip(newfrom + round,maxTime);
		//[newfrom,newto].debug("zooming to");
		newfrom = newfrom.round(round);
		newto = newto.round(round);
		//[newfrom,newto].debug("zooming to");
		
		this.zoom(newfrom,newto);
	}
	moveBy { arg percentage,round=4.0; // -1 .. 1
		// move by a percentage of current shown range, rounded to nearest bar or beat
		var newfrom,newto,range,stepBy;
		range = (to - from);
		stepBy = range * percentage;
		stepBy = stepBy.round(round);
		newfrom = from + stepBy;
		newfrom = newfrom.clip(0.0,maxTime - range).round(round);
		newto = newfrom + range;
		
//		//[newfrom,newto].debug("zooming to");
//		newfrom = newfrom.clip(0,maxTime - round);
//		newto = newto.clip(newfrom + round,maxTime);
//		//[newfrom,newto].debug("zooming to");
//		newfrom = newfrom.round(round);
//		newto = newto.round(round);
//		//[newfrom,newto].debug("zooming to");		
//		newfrom = max(from + stepBy,0.0).round(round);
//		newto = min(newfrom + range,maxTime);
		//[newfrom,newto].debug("moveBy to");
		this.zoom(newfrom,newto);
	}
	maxTime_ { arg mt;
		maxTime = mt;
		zoomCalc.modelRange = [0,maxTime];
		playZoomCalc.modelRange = [0,maxTime];
		timeRuler.maxTime = maxTime;
	}
	keyDownResponder {
		var k,default;
		default = 0@0;
		k = UnicodeResponder.new;
		//  63232
		k.register(   63232  ,   false, false, false, false, {
			this.zoomBy(1.1)
		});
		//  63233
		k.register(   63233  ,   false, false, false, false, {
			this.zoomBy(0.9)
		});
		//  shift control 63232
		k.register(   63232  ,   true, false, false, true, {
			this.zoomBy(1.01,1)
		});
		//  shift control 63233
		k.register(   63233  ,   true, false, false, true, {
			this.zoomBy(0.99,1)
		});
		//  63234
		k.register(   63234  ,   false, false, false, false, {
			this.moveBy(-0.1)
		});
		//  63235
		k.register(   63235  ,   false, false, false, false, {
			this.moveBy(0.1)		
		});
		//  shift control 63234
		k.register(   63234  ,   true, false, false, true, {
			this.moveBy(-0.01,1)		
		});
		//  shift control 63235
		k.register(   63235  ,   true, false, false, true, {
			this.moveBy(0.01,1)
		});
		^k		
	}
}

