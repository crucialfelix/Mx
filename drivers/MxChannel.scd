
/*

inlets
	audio input
	mute
	solo
	level
	fade to,time
	
*/

MxUnit.register('MxChannel',
(
	make: { arg chan;
		var inlets,outlets,patch,inps,connectors,conn,handlers;
		// isnt it a MxHasJack ??
		inlets  = [ MxInlet("MxChannelIn", 0, AudioSpec(chan.numChannels), MxListensToBus({chan.source.bus},{chan.server},{chan.mixGroup})) ];
		outlets = [ MxOutlet("MxChannelOut", 0, AudioSpec(chan.numChannels),MxPlaysOnBus({chan.source.bus}) ) ];

		MxUnit(chan,inlets,outlets);
	}
));
