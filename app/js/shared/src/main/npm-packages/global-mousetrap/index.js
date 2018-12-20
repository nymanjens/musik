'use strict';

var Mousetrap = require("mousetrap")

// Fork of mousetrap-global-bind
// (https://github.com/ccampbell/mousetrap/blob/1.6.1/plugins/global-bind/mousetrap-global-bind.js)
// making it into an npm package.

if(Mousetrap.prototype) {
  var _globalCallbacks = {};
  var _originalStopCallback = Mousetrap.prototype.stopCallback;

  Mousetrap.prototype.stopCallback = function(e, element, combo, sequence) {
    var self = this;
    if (self.paused) {
      return true;
    }
    if (_globalCallbacks[combo] || _globalCallbacks[sequence]) {
      return false;
    }
    return _originalStopCallback.call(self, e, element, combo);
  };

  Mousetrap.init();

  module.exports = {
    bind: Mousetrap.bind,
    bindGlobal(keys, callback, action) {
      Mousetrap.bind(keys, callback, action);

      if (keys instanceof Array) {
        for (var i = 0; i < keys.length; i++) {
          _globalCallbacks[keys[i]] = true;
        }
        return;
      }

      _globalCallbacks[keys] = true;
    },
  };
} else {
  module.exports = {
    bind() {
      console.log(
          "Error: Called bindGlobal but Mousetrap was undefined", Mousetrap);
    },
    bindGlobal() {
      console.log(
          "Error: Called bindGlobal but Mousetrap was undefined", Mousetrap);
    },
  }
}
