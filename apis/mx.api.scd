
var get = { arg uuid;
  API('player').call('_get', uuid);
};


(
  new: { arg reply, uuid;
    var x = Mx.new.app;
    uuid = API('player').call('_set', uuid, x);
    reply.value(uuid);
  },
  info: { arg reply, uuid;
    var mx = get.(uuid);
    reply.(mx.asJSON);
    // does not include cables yet
  },
  unitInfoAt: { arg reply, uuid, x, y;
    var mx = get.(uuid);
    reply.(mx.at(x@y).asJSON);
  },

  setInletValue: { arg reply, uuid, x, y, key, value;
    var mx = get.(uuid),
      inlet = mx.at(x@y).inlets.at(key);
    if(inlet.canSet, {
      inlet.set(value);
      reply.value(inlet.get());
    },{
      Error("Cannot set value on inlet" + inlet).throw;
    });
  },
  getInletValue: { arg reply, uuid, x, y, key;
    var mx = get.(uuid),
      inlet = mx.at(x@y).inlets.at(key);
    if(inlet.canGet, {
      reply.value(inlet.get());
    }, {
      Error("Cannot get value from inlet" + inlet).throw;
    });
  },

  getPointUUID: { arg reply, uuid, x, y;
    var mx = get.(uuid),
        unit = mx.at(x@y);
    reply.(API('player').call('prGetOrSet', unit.source));
  }
);
