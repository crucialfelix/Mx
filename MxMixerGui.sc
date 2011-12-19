

MxMixerGui : ObjectGui {

	var scope,lastScope,freqScope,meters;
	var solos,mutes;

	writeName {}
	guiBody { arg layout;
		var scopeSize = 275,faderHeight,chans;
		solos = Array.newClear(model.channels.size);
		mutes = Array.newClear(model.channels.size + 1);
		faderHeight = layout.bounds.height - scopeSize - 4;
		layout.startRow;
		if(model.isPlaying and: {model.server.inProcess},{
			layout.flow({ arg layout;
				scope = Stethoscope(model.server,2,model.master.fader.bus.index,bufsize: 4096 * 4 ,view:layout);
				scope.xZoom = 16;
			},scopeSize@scopeSize);
			if(SCFreqScope.notNil,{
				freqScope = SCFreqScope(layout,scopeSize@scopeSize);
				freqScope.inBus = model.master.bus.index;
				freqScope.dbRange_(24);
				freqScope.freqMode = 1;
				freqScope.active = true;
			})
		});
		layout.startRow;
		chans = (model.channels ++ [model.master]);
		if(model.isPlaying,{
			meters = BusMeters(model.server,chans.collect({ arg chan; chan.fader.bus}));
		});
		chans.do { arg chan,i;
			var f,ab;
			f = NumberEditor(chan.fader.db,ControlSpec(-80,12,default:0,units:"dB"));
			f.action = {
				chan.fader.db = f.value
			};
			f.gui(layout,40@faderHeight);
			// mute, solo
			
			// meter
			if(meters.notNil,{
				layout.comp({ arg layout;
					meters.makePeak(i,layout,Rect(0,0,28, GUI.skin.buttonHeight));
					meters.makeBusMeter(i,layout,Rect(0,GUI.skin.buttonHeight+1,30,faderHeight - GUI.skin.buttonHeight - 1));
				},Rect(0,0,30,faderHeight))
			});
			layout.flow({ arg layout;
				if(scope.notNil,{
					ab = ActionButton(layout,"¤",{
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
					layout.startRow;
					solos.put(i, ToggleButton(layout,"S",{ arg button,bool; model.solo(i,bool); this.updateButtons }) );
				});
				layout.startRow;
				mutes.put(i, ToggleButton(layout,"M",{ arg button,bool; 
					if(chan === model.master,{ chan.fader.mute = bool }, {model.mute(i,bool); }); 
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
	remove {
		meters.remove;
		if(freqScope.notNil,{
			freqScope.kill
		});
		super.remove;
	}
}