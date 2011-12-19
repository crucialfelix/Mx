

MxGui : AbstractPlayerGui {
	
	var boxes,drawerGui;

	writeName {}
	saveConsole { arg layout;
		super.saveConsole(layout);
		ActionButton(layout,"Timeline",{
			MxTimeGui(model).gui(nil,1000@800);
		});
		ActionButton(layout,"Mixer",{
			MxMixerGui(model).gui(nil,1000@500);
		});
		// this will move into an MxAction
		ActionButton(layout,"Insp selected",{
			var inspMe,in,out,cable;
			inspMe = boxes.selected;
			if(boxes.selected.size == 2,{
				out = boxes.selected.detect({ arg io; io.class === MxOutlet });
				in = boxes.selected.detect({ arg io; io.class === MxInlet });
				if(in.notNil and: out.notNil,{
					cable = model.cables.detect({ arg cable; cable.inlet === in and: cable.outlet === out });
					if(cable.notNil,{
						inspMe = inspMe.add( cable );
					});
				})
			});
			inspMe.do(_.insp);
		});
	}

	guiBody { arg layout,bounds;
		var bb;
		bounds = bounds ?? {layout.innerBounds.moveTo(0,0)};
		bb = bounds.resizeBy(-200,0);
		boxes = MxMatrixGui(model, layout, bb );
		boxes.transferFocus(0@0);
		this.drawer(layout,(bounds - bb).resizeTo(200,bounds.height));
		boxes.focus;
	}

	drawer { arg layout,bounds;
		var d,doIt;
		doIt = { arg obj;
			// which puts to master or channels
			boxes.put(boxes.focusedPoint.x,boxes.focusedPoint.y,obj);
			boxes.refresh;
			if(model.isPlaying,{
				model.update;
			},{
				model.updateAutoCables
			});
		};			
		d = MxDrawer({ arg obj;
			if(boxes.focusedPoint.notNil,{
				if(obj.isKindOf(MxDeferredDrawerAction),{
					obj.func = doIt
				},{
					doIt.value(obj)
				})
			})
		});
		drawerGui = d.gui(layout,bounds);
	}
	keyDownResponder {
		^boxes.keyDownResponder ++ drawerGui.keyDownResponder
	}
}


MxDeferredDrawerAction {
	
	var <>func;
	
	value { arg object;
		^func.value(object)
	}
}


