

MxKrJackGui : ObjectGui {
	
	var ne;

	writeName {}
	guiBody { arg layout;
		ne = NumberEditor(model.value,model.spec);
		ne.gui(layout);
		ne.action = {
			// as long as you didn't get jacked into from something else
			// then you may move the fader
			if(model.isReadingFromBus.not,{
				model.value = ne.value
			})
		}
	}
	update {
		if(ne.value != model.value,{
			ne.value = model.value
		})
	}
}

MxArJackGui : ObjectGui {
	
}


