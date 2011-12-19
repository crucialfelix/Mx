
/*

inlets
	db
	mute
	solo

outlets
	you don't patch from it directly,
	you do so from the mx channel
*/

MxUnit.register('MxChannelInput',
(
	make: { arg input;
		var inlets,outlets;

		inlets  = [ 
				MxInlet("in", 0, AudioSpec(input.numChannels), MxListensToBus({input.bus},{input.server},{input.group}) )
			];
		
		outlets = [ 
				MxOutlet("out", 0, AudioSpec(input.numChannels), MxPlaysOnBus({input.bus},{input.server},{input.group}) )
			];

		MxUnit(input,inlets,outlets);
	},
	save: { [] },
	load: { MxChannelInput.new }
	
	
	 /*,
	draw: { arg pen,bounds,style;
		~source.draw(pen,bounds,style)
	} */
));
