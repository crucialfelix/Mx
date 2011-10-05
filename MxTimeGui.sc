

MxTimeGui : ObjectGui {
	
	var <from,<to,maxTime,zoomCalc,playZoomCalc;
	var xScale;
	var <>laneHeight=100.0;
	var zoom,playHead,updater;
	
	writeName {}
	guiBody { arg layout;
		var i = 0,width;
		layout.startRow;
		width = layout.indentedRemaining.width;

		maxTime = 60 * 5;
		zoomCalc = ZoomCalc([0,maxTime],[0,width]);
		playZoomCalc = ZoomCalc([0,maxTime],[0,1.0]);
		
		this.prSetFromTo(0.0,maxTime);
		
		// zoom controls
		zoom = SCRangeSlider(layout,width@20);
		zoom.lo = 0.0;
		zoom.hi = 1.0;
		zoom.action = {this.zoom((zoom.lo * maxTime).round, (zoom.hi * maxTime).round)};
		zoom.knobColor = Color.black;
		zoom.keyDownAction_({nil});

		playHead = SCSlider(layout,width@10);
		playHead.background = Color(0.36241707801819, 0.55301971435547, 0.60233759880066, 1);
		playHead.knobColor = Color.black;
		playHead.thumbSize = 18;
		playHead.action = { arg mg;
			model.gotoBeat( playZoomCalc.displayToModel(mg.value).debug("goto beat") )
		};
		playHead.keyDownAction = {nil};
		//playHead.focusColor = focusColor;
		updater = Updater(model.position,{ arg pos;
			{
				playHead.value = playZoomCalc.modelToDisplay(pos.current);
			}.defer
		}).removeOnClose(layout);
		
		// Updater on model position
		
		model.channels.do { arg chan,ci;
			chan.units.do { arg unit;
				var bounds;
				if(unit.notNil and: {unit.handlers.at('timeGui').notNil},{
					layout.startRow;
					bounds = layout.indentedRemaining;
					bounds.width = width;
					bounds.height = laneHeight;
					unit.timeGui(layout,bounds,maxTime);
					// interface to move unit
					i = i + 1;
				})
			}
		};
	}
	prSetFromTo { arg argFrom,argTo;
		from = argFrom;
		to = argTo;
		zoomCalc.setZoom([from,to]);
		playZoomCalc.setZoom([from,to]);
	}
	zoom { arg argFrom,argTo;
		this.prSetFromTo(argFrom,argTo);
		model.channels.do { arg chan,ci;
			chan.units.do { arg unit;
				if(unit.notNil and: {unit.handlers.at('zoomTime').notNil},{
					unit.zoomTime(from,to);
				})
			}
		};
	}
	didClose {
		"didClose".debug;
		updater.remove;
	}
}

