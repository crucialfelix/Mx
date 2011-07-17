

MxGui : AbstractPlayerGui {
	
	var boxes;
	
	guiBody { arg layout;
		boxes = MxMatrixGui(model, layout );
	}
	
}

