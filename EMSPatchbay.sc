
/*
	an EMS Synthi style patchbay for connecting things in Mx

	aka "I sunk your battleship"

	http://en.wikipedia.org/wiki/EMS_Synthi_A
	http://www.thesynthi.de/data/SYNTHIA/A2011_1.jpg

	synthi pegs are red yellow white : varying resistance

	outlets and inlets should be iterables, preferrably MxQuery
	this will be especially interesting when MxQuery can do lazy evaluation and thus the patchbay will update dynamically.

	click to toggle patched
	color code if its possible to connect or not
	should sort by execution order
	thus for synths everything above the diagonal
	is a precendence error
*/


EMSPatchbay {

	var <>outlets,<>inlets;
	var uv,pen;
	var <>labelSize = 50,<>on,<>off,<>cant,<>font;
	var gridRect,width,height;
	var ins,outs;

	*new { arg outlets,inlets;
		^super.newCopyArgs(outlets,inlets)
	}
	gui { arg layout,bounds;
		if(layout.notNil,{
			layout = layout.asFlowView(bounds);
			uv = UserView(layout,bounds ?? {layout.indentedRemaining});
		},{
			bounds = bounds ?? {Rect(0,0,500,500)};
			layout = Window("EMS",bounds).front;
			uv = UserView(layout,layout.bounds.moveTo(0,0));
			uv.resize = 5;
		});
		uv.drawFunc = {this.draw};
		uv.mouseDownAction = Message(this,\mouseDownAction,[]);
		pen = GUI.pen;
		on = Color.yellow;
		off = Color.black;
		cant = Color.grey;
		font = Font.sansSerif(9);
	}
	draw {
		var b;
		ins = inlets.asArray;
		outs = outlets.asArray;
		b = uv.bounds.moveTo(0,0);
		pen.color = Color.grey(181/255.0);
		pen.fillRect(b);
		gridRect = Rect(labelSize,labelSize,b.width - labelSize,b.height - labelSize);

		height = max(gridRect.height / outs.size.asFloat,0);
		width = max(gridRect.width / ins.size.asFloat,0);

		pen.color = Color.black;
		pen.font = font;
		if(width > labelSize,{
			pen.use {
				//pen.translate(labelSize,0);
				//pen.rotateDeg(90);
				ins.do { arg in,ii;
					pen.stringAtPoint(in.name,
						Point(width*ii+labelSize,0)
						);
				};
			};
		});
		pen.use {
			pen.translate(0,labelSize);
			outs.do { arg out,oi;
				var to;
				pen.use {
					pen.string(out.name);
					pen.translate(labelSize,0);
					to = out.to.asArray;
					ins.do { arg in,ii;
						var can,r;
						r = Rect(0,0,width,height).insetAll(0,0,1,1);
						can = out.unit !== in.unit;
						if(can,{
							pen.color = off;
							pen.fillRect(r);
							if(to.includes(in),{
								pen.color = on;
								pen.fillOval(r);
							})
						},{
							pen.color = cant;
							pen.fillRect(r)
						});
						pen.translate(width,0)
					};
				};
				pen.translate(0,height)
			}
		};
	}
	mouseDownAction { arg view, x, y, modifiers, buttonNumber, clickCount;
		var p,col,row;
		var out,in;
		p = x@y;
		if(gridRect.contains(p),{
			p = p - gridRect.origin;
			col = (p.x / width).asInteger;
			row = (p.y / height).asInteger;
			out = outs.at(row);
			in = ins.at(col);
			if(out.unit !== in.unit,{
				if(out.to.includes(in),{
					in.disconnect
				},{
					out >> in
				});
				this.refresh;
			})
		})
	}
	refresh { uv.refresh }
}

