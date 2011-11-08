

MxGui : AbstractPlayerGui {
	
	var boxes,drawerGui;

	writeName {}
	saveConsole { arg layout;
		super.saveConsole(layout);
		ActionButton(layout,"Timeline",{
			MxTimeGui(model).gui(nil,1000@800);
		});
	}

	guiBody { arg layout,bounds;
		var bb;
		bounds = bounds ?? {layout.innerBounds.moveTo(0,0)};
		bb = bounds.resizeBy(-200,0);
		boxes = MxMatrixGui(model, layout, bb );
		boxes.transferFocus(0@0);
		this.drawer(layout,(bounds - bb).resizeTo(200,bounds.height));
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
		var kdr;
		kdr = UnicodeResponder.new;
		//  control s
		kdr.register(   19  ,   false, false, false, true, {
			drawerGui.focusSearch
		});
		^kdr	++ boxes.keyDownResponder
	}
}



