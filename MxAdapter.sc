
/*
for use in MxInlet and MxOutlet objects

the adapters describe the functionality and capability of the object that they adapt for.

many objects may play audio on a Bus. an MxHasBus adapter expresses those types of objects and can be used to get that Bus

the strategy objects contain the connection implementations that connect one adapted object to another adapted object, where the Adapter is used to obtain the resource from the adapted objects.

eg. to fetch the Bus, get/set value, set action etc.

*/


AbsMxAdapter {}

AbsMxFuncAdapter : AbsMxAdapter {
	
	var <>func;

	*new { arg thingGetter;
		^super.new.func_(thingGetter)
	}	
	value { ^func.value }	
}

MxHasBus : AbsMxFuncAdapter {}

MxHasJack : AbsMxFuncAdapter {}

MxPlaysOnBus : AbsMxFuncAdapter {}

