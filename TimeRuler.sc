
/*
time ruler
	uses a GridLines
	is zoomable like any other

	can move the range select and play head into here later
		would take:
			position,relocateFunction
		
MxTimeGui
*/


TimeRuler {

	var <maxTime;	
	var view,zoomCalc,gridLines;
	var <position;
	
	*new { arg layout,bounds,maxTime;
		^super.new.init(layout,bounds,maxTime)
	}
	
	init { arg layout,bounds,mt;
		maxTime = mt;
		zoomCalc = ZoomCalc([0.0,maxTime],[0.0, bounds.width]);
		this.gui(layout,bounds)
	}
		
	gui { arg layout,bounds;
		var pen,blue;
		view = UserView(layout,bounds);
		view.background = Color.white;
		gridLines = DrawGrid(bounds,GridLines([0.0,maxTime]),nil);
		pen = GUI.pen;
		blue = Color.blue;
		view.drawFunc = {
			gridLines.draw;
			if((position ? -1).inclusivelyBetween(*zoomCalc.zoomedRange),{
				pen.use {
					var x;
					pen.width = 1;
					pen.color = blue;
					x = zoomCalc.modelToDisplay(position);
					pen.moveTo( x@0 );
					pen.lineTo( x@bounds.height );
					pen.stroke;
				}
			})
		};
		view.focusColor = GUI.skin.focusColor ? Color.clear;
	}
	refresh {
		view.refresh
	}
	setZoom { arg from,to;
		zoomCalc.setZoom(from,to);
		gridLines.x.setZoom(from,to);
		view.refresh;
	}
	maxTime_ { arg mt;
		maxTime = mt;
		zoomCalc.modelRange = [0.0,maxTime];
		gridLines.x.setZoom(0.0,maxTime);
	}
	position_ { arg p;
		position = p;
		view.refresh;	
	}
	keyDownAction_ { arg f;
		view.keyDownAction = f;
	}
	mouseDownAction_ { arg f;
		view.mouseDownAction = { arg view,x,y,modifiers,buttonNumber,clickCount;
			f.value( zoomCalc.displayToModel(x), modifiers,buttonNumber,clickCount )
		}
	}
	isClosed {
		^view.isClosed
	}
}
