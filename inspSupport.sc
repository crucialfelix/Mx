

+ Mx {
	
	*initClass {
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
	