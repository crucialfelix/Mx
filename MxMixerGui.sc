

MxMixerGui : ObjectGui {

	var scope,lastScope,freqScope,meters;

	writeName {}
	guiBody { arg layout;
		var scopeSize = 275,faderHeight,chans;
		faderHeight = layout.bounds.height - scopeSize;
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
			})
		};
		if(meters.notNil,{
			meters.start
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