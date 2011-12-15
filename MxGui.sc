

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
		var d;
		d = MxDrawer({ arg obj;
			if(boxes.focusedPoint.notNil,{
				// which puts to master or channels
				boxes.put(boxes.focusedPoint.x,boxes.focusedPoint.y,obj);
				boxes.refresh;
				if(model.isPlaying,{
					model.update;
				},{
					model.updateAutoCables
				});
			})
		});
		drawerGui = d.gui(layout,bounds);
	}
	keyDownResponder {
		^boxes.keyDownResponder ++ drawerGui.keyDownResponder
	}
}



