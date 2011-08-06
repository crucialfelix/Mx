

MxDrawer {
	
	classvar >registery,<>registrationFilePaths;
	var <>onSelect;
	
	*new { arg onSelect;
		^super.newCopyArgs(onSelect)
	}

	*add { arg title,  buildItemFunc;
		this.registery[title] = MxDrawerItem(title,buildItemFunc)
	}
	*addGroup { arg title,itemsFunc,buildItemFunc;
		this.registery[title] = MxDrawerItemGroup(title,itemsFunc,buildItemFunc)
	}


	*registery {
		^registery ?? {
			registery = Dictionary.new;
			registrationFilePaths.do { arg p;
				p.loadPaths
			};				
			registery
		}
	}
	*addRegistrationFile { arg path;
		// a file that will be loaded that will contain calls to .add / .addGroup
		registrationFilePaths = registrationFilePaths.add(path)
	}
	*initClass {
		var path;
		path = PathName(MxDrawer.class.filenameSymbol.asString).parentPath +/+ "drivers" +/+ "registerDrawerItems.scd";
		registrationFilePaths = [path];
	}	
	
	guiClass { ^MxDrawerGui }
}


MxDrawerItem {
	
	var <>title,<>buildItemFunc;
	
	*new { arg title,buildItemFunc;
		^super.newCopyArgs(title,buildItemFunc)
	}
	make { arg i,onMake;
		buildItemFunc.value(i,onMake)
	}
}


MxDrawerItemGroup {
	
	var <>title,<>itemsFunc,<>buildItemFunc;

	*new { arg title,itemsFunc,buildItemFunc;
		^super.newCopyArgs(title,itemsFunc,buildItemFunc)
	}
	drill {
		^itemsFunc.value.collect { arg it,i;
			MxDrawerSubItem(it[0],this,it[1])
		}
	}
}
	

MxDrawerSubItem {
	
	var <>title, <>drawerItem,<>data;
	
	*new { arg title,drawerItem,data;
		^super.newCopyArgs(title,drawerItem,data)
	}
	make { arg i,onMake;
		drawerItem.buildItemFunc.value(data,i,title,onMake)
	}
}


MxDrawerGui : ObjectGui {
	
	var lv,keys,items,drillDownItem;
	
	writeName {}

	guiBody { arg layout,bounds;
		var bg,fg;
        bg = Color(0.21652929382936, 0.23886961779588, 0.26865671641791);
        fg = Color(0.94029850746269, 0.96588486140725, 1.0);
		
		// view = userView ?? { UserView(layout,bounds ?? { Rect(0,0,100,800) }) };
		
		// using ListView, though it cannot drag directly into a unit yet
        lv = ListView(layout,min(layout.bounds.width,200)@(layout.bounds.height-17-17-4-20));

		// all top level items, nothing unfolded
		this.drillUp;
        
        lv.mouseDownAction = { arg view, x, y, modifiers, buttonNumber, clickCount;
	        // double click on a top level single item or unfolded sub-item => select
	   		var item;
	        if(clickCount == 2,{
	   			item = items[lv.value];
	   			if(item.isKindOf(MxDrawerItemGroup).not,{
		   			item.make(lv.value,model.onSelect)
	   			},{
		   			this.drillDown(item);
	   			})
	        })
        };
        lv.background = bg;
        lv.stringColor = fg;
        lv.focusColor = Color.clear;
        lv.font = GUI.font.new(GUI.skin.fontSpecs[0],9);
        lv.beginDragAction = {
	        var key,item;
	        item = items[lv.value];
	        if(item.isKindOf(MxDrawerItemGroup).not,{
		        item = item.make(lv.value);
	        },{
		        item = nil
	        });
	        // should use a memento so that it can load asynch but you can already start dragging
            item
        };
        lv.enterKeyAction = {
   			var item;
   			item = items[lv.value];
   			if(item.isKindOf(MxDrawerItemGroup),{
	   			this.drillDown(item);
   			},{
	   			item.make(lv.value,model.onSelect)
   			});
            this.update;
        };
	}
	drillDown { arg itemGroup;
		items = itemGroup.drill; // title, data
		keys = items.collect(_.title);
		lv.items = keys;
		lv.refresh
	}
	drillUp {
        keys = MxDrawer.registery.keys.as(Array).sort;
        items = keys.collect { arg k; MxDrawer.registery[k] };
        lv.items = keys;
	}		
}

				
