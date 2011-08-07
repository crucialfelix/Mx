

MxMatrixGui : SCViewHolder {

	var <numRows,<numCols;
	var <mx;
	var bounds,<focusedPoint;
	var <>currentDragPoint,draggingXY,draggingOutlet,mouseDownPoint,isDown=false;

	var selected,hovering,dragging;

	var <>ioHeight=10,<>faderHeight=100.0;

	var <>dragOn=4; // pixel distance to initiate drag or a symbol like \isCntrl
	var <>background,<styles,<defaultStyle;
	var pen, boxWidth,boxHeight,boxBounds;

	*new { arg mx, w, bounds;
		^super.new.init(mx,w, bounds);
	}

	init { arg argmx, w,argbounds;

		var skin;
		mx = argmx;
		skin = GUI.skin;
		pen = GUI.pen;
		defaultStyle = (
			font: GUI.font.new(*skin.fontSpecs),
			fontColor: skin.fontColor,
			boxColor: skin.offColor,
			borderColor: skin.foreground,
			center: false
			);
		this.makeDefaultStyles(skin);

		this.calcNumRows;
		boxWidth = 100.0;//bounds.width.asFloat / numCols;
		boxHeight = 80.0;//min( (bounds.height.asFloat - faderHeight) / numRows, 80 );
	   
		// will leave room for mixer control at bottom
		bounds = argbounds ?? {Rect(20, 20, min(numCols * boxWidth,1000), numRows * boxHeight + faderHeight + ioHeight + ioHeight)};
		bounds = bounds.asRect;
		bounds = Rect(bounds.left + 0.5, bounds.top + 0.5, bounds.width, bounds.height);

		if(w.isNil, {
			w = Window(mx.asString, bounds.resizeBy(40,40).moveTo(10,250) );
			w.front
		});

		bounds = Rect(bounds.left+1, bounds.top+1, bounds.width, bounds.height);
		view = UserView(w, bounds);
		//view.resize = 5;
		bounds = bounds.moveTo(0,0); // my reference
	   this.calcBoxBounds;

		view.focusColor = defaultStyle.borderColor;

		pen = GUI.pen;
		view.drawFunc = { this.drawGrid };

		// mouses
		view.mouseOverAction = { arg me,x,y,modifiers;
			this.mouseOver(x,y,modifiers)
		};
		view.mouseDownAction = { arg me, x, y, modifiers, buttonNumber, clickCount;
			mouseDownPoint = x@y;
			if(this.mouseDownIsDragStart(modifiers,x,y),{
				this.startDrag(x,y,modifiers,buttonNumber);
			},{
				isDown = true;
				// select whatever is hit
				this.mouseDown(x,y,modifiers,buttonNumber,clickCount);
				this.view.refresh;
			});
		};
		view.mouseMoveAction = { arg me,x,y,modifiers;
			var outlet;
			if(this.isDragging(modifiers,x,y),{
				if(dragging.isNil,{ // initiating drag now because it moved far enough
					dragging = this.getByCoord(mouseDownPoint.x,mouseDownPoint.y);
					if(dragging.class === MxInlet,{ // not these
						dragging = nil
					});
				});
				draggingXY = x@y;
				this.view.refresh;
			});
		};
		view.mouseUpAction = { arg me, x, y, modifiers;
			isDown = false;
			if(this.isDragging(modifiers,x,y),{
				this.endDrag(x,y,modifiers);
				this.view.refresh;
			});
		};

		// dragging on and off the matrix
		view.beginDragAction = { arg me;
			//this.handleByFocused('beginDragAction',[]) ?? {this.focusedUnit}
		};
		view.canReceiveDragHandler = { arg me;
			focusedPoint.notNil
			// View.currentDrag
		};
		view.receiveDragHandler = { arg me;
			mx.put( focusedPoint.x,focusedPoint.y,View.currentDrag );
			this.view.refresh;
		};


		// keys
		view.keyDownAction_({ arg me,char,modifiers,unicode,keycode;
			//this.handleByFocused('keyDownAction',[char,modifiers,unicode,keycode])
		});
		view.keyUpAction = { arg me,char,modifiers,unicode,keycode;
			//this.handleByFocused('keyUpAction',[char,modifiers,unicode,keycode])
		};
		view.keyModifiersChangedAction = { arg me,char,modifiers,unicode,keycode;
			//this.handleByFocused('keyModifiersChangedAction',[char,modifiers,unicode,keycode])
		};
	}

	mouseDown { arg x, y, modifiers, buttonNumber, clickCount;
		var obj;
		obj = this.getByCoord(x,y);
		// move fader, mute/solo button

		if(clickCount == 2,{
			if(obj.isKindOf(MxUnit),{
				obj.gui
			});
		},{
			// click to select
			if(modifiers.isShift,{
				selected = selected.add(obj)
			},{
				selected = [obj]
			});
			focusedPoint = this.boxPoint(x,y)
		});
	}
	mouseOver { arg x,y,modifiers;
		var obj,unit;
		hovering = this.getByCoord(x,y);
	}

	// internal dragging
	endDrag { arg x,y,modifiers;
		// patch it
		var target,targetPoint;
		targetPoint = this.boxPoint(x,y);
		target = this.getByCoord(x,y);
		if(target.notNil,{
			if(dragging.isKindOf(MxOutlet) and: {target.isKindOf(MxInlet)},{
				mx.connect(nil,dragging,nil,target);
				mx.update
			})
		});
		// to a unit
		if(dragging.isKindOf(MxUnit),{
			// move it, copy it, replace it
			if(modifiers.isAlt,{
				mx.put(targetPoint.x,targetPoint.y, dragging.copySource );
			},{
				mx.move(dragging.point.x,dragging.point.y,targetPoint.x,targetPoint.y)
			});
			mx.update;
		});
		currentDragPoint = nil;
		this.transferFocus(targetPoint);
		dragging = nil;
	}
	startDrag { arg x,y;
		dragging = this.getByCoord(x,y);
		if(dragging.notNil,{
			this.transferFocus(this.boxPoint(x,y));
		});
		draggingXY = x@y;
		this.view.refresh;
	}

	focusedUnit {
		^focusedPoint !? {
			this.getUnit(focusedPoint)
		}
	}
	transferFocus { arg toBoxPoint;
		focusedPoint = toBoxPoint
	}

	refresh {
		view.refresh
	}
	calcNumRows {
		numCols = mx.channels.size + 1;
		numRows = mx.channels.maxValue({ arg ch; ch.units.size }) ? 0;
		numRows = max(numRows,mx.master.units.size) + 1;
	}
	calcBoxBounds {
		boxBounds = Rect(bounds.left,
						bounds.top + ioHeight,
						view.bounds.width,
						view.bounds.height - faderHeight - ioHeight - ioHeight);
	}		
	boxPoint { arg x,y;// view coords
		var col,row,p;
		p = x@y;
		if(boxBounds.containsPoint(p).not,{
			^nil
		});
		p = p - boxBounds.origin;
		col = this.intFloor( p.x.asFloat / boxWidth );
		row = this.intFloor( p.y.asFloat / boxHeight );
		^col@row
	}
	
	intFloor { arg i; // a bandaid
		if(i.frac.equalWithPrecision(1.0),{
			^i.round.asInteger
		},{
			^i.floor.asInteger
		})
	}

	getUnit { arg boxPoint;
		if(boxPoint.x == mx.channels.size,{
			^mx.master.units[boxPoint.y]
		},{
			^mx.at(boxPoint.x,boxPoint.y)
		});
	}
	getUnitFromCoords { arg x,y; // view coords
		^this.getUnit(this.boxPoint(x,y) ?? {^nil});
	}
	getByCoord { arg x,y;
		// outlet, inlet, box, fader
		var unit,bp,oi,ioArea,b,iolets,p;
		p = x@y;
		bp = this.boxPoint(x,y);
		if(bp.notNil,{
			unit = this.getUnit(bp);
			if(unit.isNil,{
				^nil
			});
			b = this.getBounds(bp);
			//outlet hit
			if(unit.outlets.size > 0,{
				ioArea = this.outletsArea(b);
				if(ioArea.containsPoint(p),{
					^this.findIOlet(unit.outlets,ioArea,p)
				})
			});
			//inlet hit
			if(unit.inlets.size > 0,{
				ioArea = this.inletsArea(b);
				if(ioArea.containsPoint(p),{
					^this.findIOlet(unit.inlets,ioArea,p)
				})
			});
			^unit
		});
		if(mx.inlets.size > 0,{
			if(p.y < ioHeight) {
				^this.findIOlet( mx.inlets, Rect(0,0,bounds.width,ioHeight), p )
			}
		});
		if(mx.outlets.size > 0,{
			if(p.y >= (bounds.bottom - ioHeight),{
				^this.findIOlet( mx.outlets, Rect(0,bounds.bottom - ioHeight,bounds.width,ioHeight), p )
			});
		});
		^nil
	}
	findIOlet { arg iolets,ioArea,point;
		// inside an iolet area find which one the point is on
		var oi;
		oi = ((point.x - ioArea.left).asFloat / (ioArea.width.asFloat / iolets.size)).floor.asInteger;
		^iolets[oi]
	}

	getBounds { arg boxPoint;
		// x is col
		// y is row
		^Rect(boxPoint.x * boxWidth, boxPoint.y * boxHeight + boxBounds.top, boxWidth, boxHeight)
	}
	mouseDownIsDragStart { arg modifiers,x,y;
		var boo;
		if(dragOn.isNumber,{
			// wait for mouse move to confirm
			^false
		},{
			// immediate pick up
			boo = modifiers.perform(dragOn);
			^boo
		})
	}
	isDragging { arg modifiers,x,y;
		 if(dragOn.isNumber,{
			 if(mouseDownPoint.notNil,{
				 if((x@y).dist(mouseDownPoint) > dragOn,{
					 //mouseDownPoint = nil;
					 ^true
				},{
					^false
				})
			},{
				^currentDragPoint.notNil
			})
		},{
			^modifiers.perform(dragOn)
		})
	}
	outletsArea { arg rect;
		^Rect.newSides( rect.left, rect.bottom - ioHeight, rect.right,rect.bottom)
	}
	inletsArea { arg rect;
		^Rect( rect.left, rect.top, rect.width,ioHeight)
	}
	ioArea { arg iosArea,i, iowidth;
		^Rect(iosArea.left + (iowidth * i), iosArea.top,iowidth,ioHeight)
	}
	inletArea { arg inlet;
		var p,b,r;
		p = inlet.unit.point;
		b = this.getBounds(p);
		r = this.inletsArea(b);
		^this.ioArea( r , inlet.index, r.width.asFloat / inlet.unit.inlets.size )
	}
	outletArea { arg outlet;
		var p,b,r;
		p = outlet.unit.point;
		b = this.getBounds(p);
		r = this.outletsArea(b);
		^this.ioArea( r , outlet.index, r.width.asFloat / outlet.unit.outlets.size )
	}
	drawIOlets { arg ioarea,lets;
		var iowidth;
		iowidth = ioarea.width.asFloat / lets.size;
		lets.do { arg outlet,i;
			var or;
			or = this.ioArea(ioarea,i,iowidth);
			pen.color = outlet.spec.color;
			pen.fillRect(or);
			pen.color = Color.grey(alpha: 0.4);
			pen.strokeRect(or);
			pen.stringLeftJustIn(outlet.name.asString,or.insetBy(1,1))
		}
	}
	drawGrid {
		var d,box,style,r;
		d = { arg rect,unit,styleName,boxPoint;
			var style,styleNames,name,ioarea,iowidth;
			// cascade styles: defaultStyle + box style + box's set styles (playing, selected) + temp style (down, focused)
			style = defaultStyle.copy;
			if(unit.notNil,{
				styleNames = ['unit'];
				name = unit.name
			},{
				styleNames = [];
			});
			if(styleName.notNil,{
				styleNames = styleNames.add(styleName)
			});
			styleNames.do { arg sn;
				styles[sn].keysValuesDo { arg k,v;
					style[k] = v.value(style[k],unit)
				}
			};

			pen.color = style['boxColor'];
			pen.fillRect( rect );
			pen.color = style['borderColor'];
			pen.strokeRect( rect );
			if(unit.notNil,{
				pen.color = style['fontColor'];
				pen.font = style['font'];
				if(style['center'],{
					pen.stringCenteredIn(name,rect)
				},{
					pen.stringLeftJustIn(name, rect.insetBy(2,2) )
				});

				// outlets
				if(unit.outlets.size > 0,{
					this.drawIOlets(this.outletsArea(rect),unit.outlets);
				});
				if(unit.inlets.size > 0,{
					this.drawIOlets(this.inletsArea(rect),unit.inlets);
				});
			});
		};

		pen.width = 1;
		pen.color = background;
		pen.fillRect(bounds); // background fill

		// each time ?
		this.calcNumRows;
		this.calcBoxBounds;
		
		numCols.do({ arg ci;
			numRows.do({ arg ri;
				var p,unit;
				p = ci@ri;
				if(ci == mx.channels.size,{ // master
					unit = mx.master.units.at(ri)
				},{
				   unit = mx.at(p.x,p.y);
				});
				d.value(this.getBounds(p),unit, nil, p );
			})
		});
		// main inlets / outlets
		this.drawIOlets(Rect(bounds.left,bounds.top,bounds.width,ioHeight),mx.inlets);
		this.drawIOlets(Rect(bounds.left,bounds.top,bounds.width,ioHeight).bottom_(bounds.bottom),mx.outlets);

		// draw focused on top so border style wins out against neighbors
		if(focusedPoint.notNil,{
			if(isDown) {
				style = 'down'
			} {
				style = 'focused'
			};
			d.value(this.getBounds(this.focusedPoint),mx.at(this.focusedPoint.x,this.focusedPoint.y),style, this.focusedPoint );
		});

		if(dragging.notNil,{
			if(dragging.isKindOf(MxUnit),{
				d.value(
					Rect(draggingXY.x,draggingXY.y + boxBounds.top,boxWidth,boxHeight)
						.moveBy((boxWidth / 2).neg,(boxHeight / 2).neg),
				  dragging,
				  'dragging',
				  draggingXY
				  )
			},{
				// outlet
				pen.color = dragging.spec.color;
				pen.width = 2;
				r = Rect(draggingXY.x,draggingXY.y,ioHeight,ioHeight).moveBy(ioHeight.neg / 2,ioHeight.neg / 2);
				pen.fillOval( r );
				pen.color = Color.blue;
				pen.strokeOval( r );
			});
		});

		// inlets that can accept
		// hovering

		mx.cables.do { arg cable,i;
			var f,t,c;
			f = this.outletArea(cable.outlet);
			t = this.inletArea(cable.inlet);
			c = cable.outlet.spec.color;
			if(cable.active.not,{
				c = Color(c.red,c.green,c.blue,0.3)
			});
			pen.color = Color.black;
			pen.width = 3;
			pen.moveTo(f.center);
			pen.lineTo(t.center);
			pen.stroke;
			pen.color = c;
			pen.width = 1;
			pen.moveTo(f.center);
			pen.lineTo(t.center);
			pen.stroke;
		};
		mx.autoCables.do { arg cable,i;
			var f,t,c;
			f = this.outletArea(cable.outlet);
			t = this.inletArea(cable.inlet);
			c = cable.outlet.spec.color;
			c = c.lighten(Color.grey,0.6);
			if(cable.active.not,{
				c = Color(c.red,c.green,c.blue,0.3)
			});
			pen.color = Color.grey;
			pen.width = 3;
			pen.moveTo(f.center);
			pen.lineTo(t.center);
			pen.stroke;
			pen.color = c;
			pen.width = 1;
			pen.moveTo(f.center);
			pen.lineTo(t.center);
			pen.stroke;
		};
	}
	makeDefaultStyles { arg skin;
		background = skin.background;
		/*
			fontSpecs:		["Helvetica", 10],
			fontColor:		Color.black,
			background:		Color(0.8, 0.85, 0.7, 0.5),
			foreground: 	Color.grey(0.95),
			onColor:		Color(0.5, 1, 0.5),
			offColor:		Color.clear,
			gap:			0 @ 0,
			margin:			2@2,
			boxHeight:		16
			*/

		styles = IdentityDictionary.new;
		styles['focused'] = (
			borderColor: Color(0.2499443083092, 0.55516802266236, 0.76119402985075)
			);
		styles['over'] = (
			boxColor: { |c| c.saturationBlend(Color.black,0.3) }
			);
		styles['down'] = (
			boxColor: Color(0.093116507017153, 0.25799716055499, 0.28358208955224, 0.86567164179104),
			borderColor: skin.foreground
			);
		styles['dragging'] = (
			boxColor: { arg c; c.blend(Color.blue(alpha:0.2),0.8) },
			borderColor: Color.blue(alpha:0.5)
			);

		styles['deactivated'] = (
			fontColor: { |c| c.alpha_(0.2) },
			boxColor: { |c| c.alpha_(0.2) },
			borderColor: { |c| c.alpha_(0.2) }
			);
		styles['selected'] = (
			borderColor: Color.blue
			);
		styles['unit'] = (
			boxColor: { arg c; c.darken(Color(0.2202380952381, 0.40008503401361, 0.5)) },
			borderColor: Color(0.2202380952381, 0.40008503401361, 0.5)
			);
	}

	// rearranging
//	  copy { arg fromBoxPoint,toBoxPoint;
////		var box;
////		box = this.getBox(fromBoxPoint).copy;
////		box.point = toBoxPoint;
////		boxes.put(toBoxPoint,box);
//	  }
//	  clear { arg point;
////		boxes.removeAt(point);
//	  }
//	  clearAll {
//		  boxes = Dictionary.new;
//	  }
//	  move { arg fromBoxPoint,toBoxPoint;
////		if(fromBoxPoint != toBoxPoint,{
////			this.copy(fromBoxPoint,toBoxPoint);
////			this.clear(fromBoxPoint);
////		});
//	  }
//	  swap { arg fromBoxPoint,toBoxPoint;
////		var tmp;
////		tmp = this.getBox(toBoxPoint).copy;
////		this.copy(fromBoxPoint,toBoxPoint);
////		tmp.point = toBoxPoint;
////		boxes[toBoxPoint] = tmp;
//	  }


	// the key responders are passed to the FOCUSED box
	// box, char,modifiers,unicode,keycode
	keyUpAction_ { arg func;
		//this.setHandler('keyUpAction',func)
	}
	// box, char,modifiers,unicode,keycode
	keyDownAction_ { arg func;
		//this.setHandler('keyDownAction',func)
	}
	// box,modifiers
	keyModifiersChangedAction_ { arg func;
		//this.setHandler('keyModifiersChangedAction',func)
	}

}

/*
		addKeyHandler(function,keycode,shift,cntl,alt,cmd,caps,numPad,fun)
		addUnicodeHandler(function,keycode,shift,cntl,alt,cmd,caps,numPad,fun)
		navByArrows
*/
