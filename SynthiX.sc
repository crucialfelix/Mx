
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

	x = Mx.new.app;
x.add( MultiSplineFr.new );
x.add( TotalTracker.new );
SynthiX(x.outlets,x.inlets).gui;

*/


SynthiX {

	var <>outlets,<>inlets;
	var uv,pen;
	var <>labelSize = 50,<>on,<>off,<>cant,<>font;
	var gridRect,width,height;
	var ins,outs;
	var <updateRate=1.0;

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
		this.update;
		ins[0].mx.addDependant(this);
		if(updateRate.notNil,{


		})
	}
	update {
		// if what is 'grid'
		ins = inlets.asArray.sort({ arg a,b; a.unit.point <= b.unit.point });
		outs = outlets.asArray.sort({ arg a,b; a.unit.point <= b.unit.point });
	}
	draw {
		var b,black,blue;
		black = Color.black;
		blue = Color.yellow;
		b = uv.bounds.moveTo(0,0);
		pen.color = Color.grey(181/255.0);
		pen.fillRect(b);
		gridRect = Rect(labelSize,17,b.width - labelSize,b.height - 17);

		height = max(gridRect.height / outs.size.asFloat,0);
		width = max(gridRect.width / ins.size.asFloat,0);

		pen.color = black;
		pen.font = font;
		pen.use {
			ins.do { arg in,ii;
				var r,v;
				r = Rect(width*ii+labelSize,0,width,17);
				pen.color = in.spec.color;
				pen.fillRect(r);
				pen.stringInRect(in.name,r.moveBy(1,1),font,black);

				// mark value of inlet value
				if(in.canGet,{
					v = in.spec.unmap(in.get);
					pen.color = blue;
					r = r.moveBy( v * width,0 );
					r.width = 2;
					pen.fillRect( r );
				});
			};
		};
		pen.use {
			pen.translate(0,17);
			outs.do { arg out,oi;
				var to;
				pen.use {
					var r,v;
					r = Rect(1,1,labelSize,height);
					pen.color = out.spec.color;
					pen.fillRect(r);
					pen.stringInRect(out.name,r,font,black);
					if(out.canGet,{
						v = out.spec.unmap(out.get);
						pen.color = blue;
						r = r.moveBy( v * width,0 );
						r.width = 2;
						pen.fillRect( r );
					});
					pen.translate(labelSize,0);
					to = out.to.asArray;
					ins.do { arg in,ii;
						var can,r;
						r = Rect(0,0,width,height).insetAll(0,0,1,1);
						can = out.unit !== in.unit;
						if(can,{
							pen.color = if(to.includes(in),on,off);
						},{
							pen.color = cant;
						});
						pen.fillRect(r);
						pen.translate(width,0)
					};
				};
				pen.translate(0,height)
			}
		};
	}
	mouseDownAction { arg view, x, y, modifiers, buttonNumber, clickCount;
		var p,col,row;
		var out,in,v;
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
		},{
			// mouse move actually

			// top or side ?
			if(p.x < gridRect.left,{
				row = ((p.y - gridRect.top) / height).asInteger;

				if(clickCount == 2,{
					outs.at(row).unit.gui
				},{
					this.refresh;
				})

			},{
				if(p.y < gridRect.top,{
					col = ((p.x - gridRect.left) / width).asInteger;
					in = ins.at(col);
					if(clickCount == 2,{
						in.unit.gui
					},{
						if(modifiers.isCtrl,{
							if(in.canSet,{
								v = (p.x - gridRect.left).excess(col * width) / width;
								in.set(v);
								{this.refresh}.defer(0.05);
							})
						})
					})
				},{
					// top corner
					// this.refresh.debug("corner");
					// could toggle auto-refresh
				})
			})

		})
	}
	refresh { uv.refresh }
}

