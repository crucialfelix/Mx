

MxMixerGui : ObjectGui {

	var scope,lastScope,freqScope,meters;
	var faders,solos,mutes;

	writeName {}
	guiBody { arg parent,bounds,showScope=true;
		var scopeSize = 275,faderHeight,chans;
		solos = Array.newClear(model.channels.size);
		mutes = Array.newClear(model.channels.size + 1);
		faders = Array.newClear(model.channels.size + 1);
		bounds = bounds ?? {parent.bounds};
		showScope = showScope and: {model.isPlaying and: {model.server.inProcess}};
		if(showScope,{
			faderHeight = bounds.height - scopeSize - 4;
		},{
			faderHeight = bounds.height
		});

		if(showScope,{
			parent.startRow;
			parent.flow({ arg parent;
				scope = Stethoscope(model.server,2,model.master.fader.bus.index,bufsize: 4096 * 4 ,zoom:1.0,rate:\audio,view:parent);
				scope.xZoom = 16;
			},scopeSize@scopeSize);
			/*
			freqScope = PlusFreqScope(parent,Rect(0,0,scopeSize,scopeSize));
			freqScope.inBus = model.master.bus.index;
			freqScope.dbRange = 18;
			freqScope.freqMode = 1;
			freqScope.active = true;
			*/
		});
		parent.startRow;
		chans = (model.channels ++ [model.master]);
		if(model.isPlaying and: {\BusMeters.asClass.notNil},{
			// else it doesnt have busses yet
			// could allocate on demand
			meters = BusMeters(model.server,chans.collect({ arg chan; chan.fader.bus }));
		});
		chans.do { arg chan,i;
			var f,ab;
			f = NumberEditor(chan.fader.db,ControlSpec(-80,12,default:0,units:"dB"));
			f.action = {
				chan.fader.db = f.value;
				model.changed('mixer',this);
			};
			faders.put(i,f);
			f.gui(parent,40@faderHeight);

			// meter
			if(meters.notNil,{
				parent.comp({ arg parent;
					meters.makePeak(i,parent,Rect(0,0,28, GUI.skin.buttonHeight));
					meters.makeBusMeter(i,parent,Rect(0,GUI.skin.buttonHeight+1,30,faderHeight - GUI.skin.buttonHeight - 1));
				},Rect(0,0,30,faderHeight))
			});
			parent.flow({ arg parent;
				if(scope.notNil,{
					ab = ActionButton(parent,"¤",{
						scope.index = chan.fader.bus.index;
						if(freqScope.notNil,{
							freqScope.inBus = chan.fader.bus.index;
						});
						if(lastScope.notNil,{
							lastScope.labelColor = Color.yellow;
							lastScope.background = Color.black;
							lastScope.refresh;
						});
						ab.background = Color.red;
						ab.labelColor = Color.black;
						ab.refresh;
						lastScope = ab;
					});
					if(chan === model.master,{
						ab.background = Color.red;
						ab.labelColor = Color.black;
						lastScope = ab;
					},{
						ab.background = Color.black;
						ab.labelColor = Color.yellow;
					})
				});
				if(chan !== model.master,{
					parent.startRow;
					solos.put(i, ToggleButton(parent,"S",{ arg button,bool; 
									model.solo(i,bool); 
									model.changed('mixer',this);
									this.updateButtons 
								}) );
				});
				parent.startRow;
				mutes.put(i, ToggleButton(parent,"M",{ arg button,bool; 
					if(chan === model.master,{ chan.fader.mute = bool }, {model.mute(i,bool); }); 
					model.changed('mixer',this);
					this.updateButtons 
				}) );
					
			},Rect(0,0,24,faderHeight));
		};
		this.updateButtons;
		if(meters.notNil,{
			meters.start
		})
	}
	updateButtons {
		model.channels.do { arg chan,i;
			solos[i].value = chan.fader.solo;
			mutes[i].value = chan.fader.mute;
		};
		mutes.last.value = model.master.fader.mute;
	}
	updateFaders {
		// needs to add remove, reorder faders
		model.channels.do { arg chan,i;
			faders[i].value = chan.fader.db
		}
	}
	update { arg mx, what;
		if(what == 'mixer',{
			this.updateButtons;
			this.updateFaders
		})
	}
	remove {
		meters.remove;
		if(freqScope.notNil,{
			freqScope.kill
		});
		super.remove;
	}
}