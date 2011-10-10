
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
			gridLines.draw	
		}
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
	keyDownAction_ { arg f;
		view.keyDownAction = f;
	}
}
