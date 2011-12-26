

EventListPlayerGui : AbstractPlayerGui {
	
	var tg,zoomCalc,rs,hitAreas,selected,mouseDownPoint;
	
	guiBody { arg layout,bounds;
		// zoom control if top
		// test buttons to click each one
		this.timeGui(layout,bounds ?? {Rect(0,0,layout.bounds.width,100)})
	}
	timeGui { arg layout,bounds,maxTime;
		tg = UserView(layout,bounds);
		if(maxTime.isNil,{
			maxTime = model.beatDuration;
			if(maxTime.isNil,{
				maxTime = 128
			},{
				maxTime = (model.beatDuration * 1.1 + 0.1).ceil;
			})
		});
		zoomCalc = ZoomCalc([0,maxTime],[0,bounds.width]);
		rs = PenCommandList.new;
		selected = List.new;
		this.updateTimeGui;
		tg.drawFunc = rs;

		tg.mouseDownAction = { |uvw, x, y,modifiers, buttonNumber, clickCount|
			var was;
			mouseDownPoint = x@y;
			hitAreas.any { arg ass;
				var hit;
				hit = ass.key.containsPoint(mouseDownPoint);
				if(hit,{
					was = selected.remove(ass.value);
					if(was.isNil,{
						selected.add(ass.value)
					})
				});
				hit
			};
			// if none then deselect all
		};
			
		tg.mouseMoveAction = { |uvw, x,y| 
			if( selected.notEmpty ) { 
				// how much in x direction have we moved ?
				// add selected events in that position as ghosts
				
			}; 
		};
		tg.mouseUpAction = { |uvw,x,y|
			if(selected.notEmpty) {
				// how much in x direction have we moved ?
				// move selected events to those positions and updateTimeGui
				
			}
		}
	}
	updateTimeGui {
		var h,r,black;
		rs.clear;
		hitAreas = List.new;
		h = tg.bounds.height;
		rs.add(\addRect,tg.bounds.moveTo(0,0));
		rs.add('fillColor_',Color.white);
		rs.add('fill');
		black = Color.black;
		model.events.do { arg ev,i;
			var x;
			if(ev['beat'].notNil,{
				x = zoomCalc.modelToDisplay(ev['beat']);
				if(x.notNil,{ // on screen
					r = Rect(x,0,10,h);
					rs.add(\color_,Color.green);
					rs.add(\addRect,r);
					hitAreas.add(r -> i);
					rs.add(\draw,3);
					rs.add(\color_,black);
					rs.add(\stringCenteredIn, i.asString,r);
				})
			})
		};
	}
	setZoom { arg from,to;
		zoomCalc.setZoom(from,to);
	}
	update {
		this.updateTimeGui;
		tg.refresh
	}
}


InstrEventListPlayerGui : EventListPlayerGui {
	
	writeName { arg layout;
		super.writeName(layout);
		this.addEventButton(layout)
	}
	addEventButton { arg layout;
		ActionButton(layout,"+",{
			this.addEventDialog(blend(zoomCalc.zoomedRange[0],zoomCalc.zoomedRange[1],0.5).round(1))
		});
	}		
	addEventDialog { arg beat;
		InstrBrowser({ arg layout,instr;
			var patch,beatEditor,playingPatch,up;
			patch = Patch(instr);
			patch.gui(layout);
			layout.startRow;
			ToggleButton(layout,"test",{
				playingPatch = model.playEvent(patch.asEvent);
				// Updater(playingPatch
				// watch for it to die
				// should just have an onFree binding
			},{
				playingPatch.stop.free
			});
			ActionButton(layout,"RND",{
				patch.rand
			});
			beatEditor = NumberEditor(beat,[0, min(model.beatDuration ? 128,128) + 512 ]);
			CXLabel(layout,"At beat");
			beatEditor.gui(layout);
			ActionButton(layout,"Insert event",{
				var e;
				e = patch.asEvent;
				e[\beat] = beatEditor.value;
				model.addEvent(e);
				model.changed(\didAddEvent);
			});
			layout.hr;
		}).gui
	}
}


