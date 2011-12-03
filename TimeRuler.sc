
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
		view = UserView(layout,bounds);
		gridLines = GridLines(view,bounds,nil,[0.0,maxTime].asSpec,true,false);
		view.drawFunc = {
			gridLines.draw;
			if((position ? -1).inclusivelyBetween(*zoomCalc.zoomedRange),{
				Pen.use {
					var x;
					Pen.width = 1;
					Pen.color = Color.blue;
					x = zoomCalc.modelToDisplay(position);
					Pen.moveTo( x@0 );
					Pen.lineTo( x@bounds.height );
					Pen.stroke;
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
		gridLines.domainSpec = [from,to].asSpec;
		view.refresh;
	}
	maxTime_ { arg mt;
		maxTime = mt;
		zoomCalc.modelRange = [0.0,maxTime];
		gridLines.domainSpec = [0.0,maxTime].asSpec;
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
