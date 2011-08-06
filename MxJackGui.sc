

MxKrJackGui : ObjectGui {
	
	var ne;

	writeName {}
	guiBody { arg layout;
		ne = NumberEditor(model.value,model.spec);
		ne.gui(layout);
		ne.action = {
			model.value = ne.value
		}
	}
	update {
		if(ne.value != model.value,{
			ne.value = model.value
		})
	}
}


