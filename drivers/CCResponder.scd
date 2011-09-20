
var cs;
cs = ControlSpec(0,127,\lin,1);

MxUnit.register('CCResponder',
(
	make: { arg ccr;
		var inlets,outlets,sips;
		ccr.function = {  |src,chan,num,value|
			~value = value;
			~source.changed
		}.inEnvir;
				
		inlets  = [];
		outlets = [ MxOutlet("CC", 0, cs ,
						MxSendsValueOnChanged({ccr})
						// MxHasAction({arg action; ccr.function = action  }) 
						) 
				];
		MxUnit(ccr,inlets,outlets);
	},
	spec: {
		cs		
	},
	prepareToBundle:  { arg agroup, bundle, private, bus;
	},
	spawnToBundle: { arg bundle;
		/*var s;
		s = ~source;
		// midi responders could refactor in order to detect if installed or not
		bundle.addFunction({
			s.class.add(s)
		}) */
	},
	freeToBundle: { arg bundle; 
		var s;
		s = ~source;
		bundle.addFunction({
			s.class.remove(s)
		})
	},

	save: {
		[~source.src,~source.chan,~source.num]
	},
	load: { arg data;
		CCResponder(nil,data[0],data[1],data[2],true)
	},
	gui: { arg layout;
		var l,ne,tb,ve;
		var oneShot;
		var source = ~source;
		l = layout ?? {PageLayout("CC",Rect(0,0,500,600))};
		tb = ToggleButton(l,"Learn",{
			oneShot = CCResponder({ |src,chan,num,value|
						source.matchEvent = MIDIEvent(nil,src,chan,num,nil);
						oneShot.remove;
						{
							tb.toggle(false,false);
							ne.value = num;
						}.defer
					},nil,nil,nil,nil,true,true)
		},{
			oneShot.remove
		});
		Updater(~source,{
			ve.value = ~value
		}.inEnvir).removeOnClose(l);
		CXLabel(l,"CC");
		ne = NumberEditor((~source.matchEvent.b ? 1).asInteger,ControlSpec(1,128,\lin,1));
		ne.action = {
			source.matchEvent = MIDIEvent(nil,source.matchEvent.src,source.matchEvent.chan,ne.value.asInteger,nil);
		};
		ne.smallGui(l);

		CXLabel(l,"value");
		ve = NumberEditor(~value ? 0,cs);
		ve.gui(l);
		
		if(layout.isNil,{
			l.resizeToFit
		})
	}
));