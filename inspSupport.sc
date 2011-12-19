

+ Mx {
	
	*initClass {
		var busf;
		if(Insp.notNil,{
			Class.initClassTree(CXObjectInspector);
			CXObjectInspector.registerHook(MxCable,{ arg cable,layout;
				

				InspectorLink.captioned("Out",cable.outlet.unit.source,layout.startRow);
				InspectorLink.captioned("In",cable.inlet.unit.source,layout.startRow);

				InspectorLink.captioned("In adapter",cable.outlet.adapter,layout.startRow);
				InspectorLink.captioned("Out adapter",cable.inlet.adapter,layout.startRow);

				layout.startRow;
				try {
					InspectorLink.captioned("Strategy",cable.strategy,layout);
				} {
					CXLabel(layout,"NO STRATEGY FOR CABLE");
				}
			});
			
			busf = { arg pob,layout;
					
					var bus,listen;
					try {
						bus = pob.value;
					};
					if(bus.notNil,{
						if(bus.rate == 'audio',{
							listen = Patch({ In.ar( bus.index, bus.numChannels ) });
							layout.startRow;
							CXLabel( layout, bus.asString );
							ToggleButton( layout,"listen",{
								listen.play
							},{
								listen.stop
							});
						});	
						layout.startRow.flow({ |f|
							var ann;
							CXLabel(f,"Annotations:");
							ann = BusPool.getAnnotations(bus);
	
							if(ann.notNil,{
								ann.keysValuesDo({ |client,name|
									f.startRow;
									Tile(client,f);
									CXLabel(f,":"++name);
								});
							});
						})
					});
				};
			[MxPlaysOnBus,MxListensToBus,MxHasBus,MxPlaysOnKrBus].do { arg klass;
				CXObjectInspector.registerHook(klass,busf);
			};
				
						
			/*
			CXObjectInspector.registerHook(MxCableStrategy,{ arg strategy,layout;
				
				CXObjectInspector.sourceCodeGui
				InspectorLink.captioned("connectf",cable.outlet.unit.source,layout.startRow);
				InspectorLink.captioned("adapter",cable.outlet.adapter,layout.startRow);

				InspectorLink.captioned("In",cable.inlet.unit.source,layout.startRow);
				InspectorLink.captioned("adapter",cable.inlet.adapter,layout.startRow);

				layout.startRow;
				try {
					InspectorLink.captioned("Strategy",cable.strategy,layout);
				} {
					CXLabel(layout,"NO STRATEGY FOR CABLE");
				}
			})
			*/
			
		})
	}
	
}
	