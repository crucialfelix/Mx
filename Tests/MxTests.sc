
/*
	requires UnitTesting
*/

TestMxLoader : UnitTest {
	
	test_loadData {
		
		var mx,data,loader;
		
		mx = Mx.new;
		
		data = mx.storeArgs[0];
		mx = Mx(data);

	}
	
	test_doubleLoad {
		var mx,data,loader;
		
		mx = Mx.new;
		
		data = mx.storeArgs[0];
		mx = Mx(data);

		data = mx.storeArgs[0];
		mx = Mx(data);
	}
	
}


