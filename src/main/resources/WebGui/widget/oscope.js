/*

 dependencies - tinygradient, tinycolor

 FIXME - all data $scope.{name}_oscope

 FIXME - all msgs / onMsg need to mirror the service gui this directive was put into
 - that should not be the case

 TODO !!! - 
 		  *  subscribe only to the single service.method which is neeeded !
 		  *  line artifact on screen from last position
          *  all data in _self.oscope
          *  dynamically adjustable screen size
          *  zoom
          *  multi-line overlay (no erase)
          *  trace directive
          *  all parameters passed in
          *  list dependencies

*/
angular.module('mrlapp.service').directive('oscope', ['mrl', '$log', function(mrl, $log) {
    return {
        restrict: "E",
        templateUrl: 'widget/oscope.html',
        scope: {
            serviceName: '@'
        },
        // scope: true,
        link: function(scope, element) {
            var _self = this;
            var name = scope.serviceName;
            var service = mrl.getService(name);
            var mode = 'read';
            // 'read' || 'write'
            var width = 800;
            var height = 100;
            var margin = 10;
            var minY = margin;
            var maxY = height - margin;
            var scaleX = 1;
            var scaleY = 1;
            scope.readWrite = 'read';
            // button toggle read/write
            // scope.blah = {};
            // scope.blah.display = false;
            scope.pinIndex = {};
            var x = 0;
            var gradient = tinygradient([// tinycolor('#ff0000'),       // tinycolor object
            // {r: 0, g: 255, b: 0},       // RGB object
            {
                h: 0,
                s: 0.4,
                v: 1,
                a: 1
            }, // HSVa object
            {
                h: 240,
                s: 0.4,
                v: 1,
                a: 1
            }//, // HSVa object
            //'rgb(120, 120, 0)',         // RGB CSS string
            //'gold'                      // named color
            ]);
            scope.oscope = {};
            scope.oscope.traces = {};
            scope.oscope.writeStates = {};
            // display update interfaces
            // defintion stage
            var setTraceButtons = function(pinIndex) {
                if (pinIndex == null) {
                    return;
                }
                var size = Object.keys(pinIndex).length
                scope.pinIndex = pinIndex;
                var colorsHsv = gradient.hsv(size);
                // pass over pinIndex add display data
                for (var key in pinIndex) {
                    if (!pinIndex.hasOwnProperty(key)) {
                        continue;
                    }
                    scope.oscope.traces[key] = {};
                    var trace = scope.oscope.traces[key];
                    var pinDef = pinIndex[key];

                    // adding style
                    var color = colorsHsv[pinDef.address];
                    trace.readStyle = {
                        'background-color': color.toHexString()
                    };
                    trace.writeStyle = {
                        'background-color': '#eee'
                    };
                    trace.color = color;
                    trace.state = false;
                    // off
                    trace.posX = 0;
                    trace.posY = 0;
                    trace.count = 0;
                    trace.colorHexString = color.toHexString();
                    trace.stats = {
                        min: 0,
                        max: 1,
                        totalValue: 0,
                        totalSample: 1
                    }
                }
            }
            // FIXME this should be _self.onMsg = function(inMsg)
            this.onMsg = function(inMsg) {
                //console.log('CALLBACK - ' + msg.method);
                switch (inMsg.method) {
                case 'onState':
                    // backend update 
                    setTraceButtons(inMsg.data[0].pinDefs.pinIndex);
                    scope.$apply();
                    break;
                case 'onPinArray':
                    x++;
                    pinArray = inMsg.data[0];
                    for (i = 0; i < pinArray.length; ++i) {
                        // get pin data & definition
                        pinData = pinArray[i];
                        pinDef = scope.pinIndex[pinData.address];
                        // get correct screen and references
                        var screen = document.getElementById('oscope-address-' + pinData.address);
                        var ctx = screen.getContext('2d');
                        var trace = scope.oscope.traces[pinData.address];
                        var stats = trace.stats;
                        // TODO - sample rate Hz
                        trace.stats.totalSample++;
                        trace.stats.totalValue += pinData.value;
                        if (pinData.value < trace.stats.min) {
                            trace.stats.min = pinData.value;
                        }
                        if (pinData.value > trace.stats.max) {
                            trace.stats.max = pinData.value;
                        }
                        var maxX = trace.stats.max;
                        var minX = trace.stats.min;
                        var c = minY + ((pinData.value - minX) * (maxY - minY)) / (maxX - minX);
                        var y = height - c;
                        ctx.beginPath();
                        // from
                        ctx.moveTo(trace.posX, trace.posY);
                        // to
                        ctx.lineTo(x, y);
                        // save current values
                        trace.posX = x;
                        trace.posY = y;
                        // color
                        ctx.strokeStyle = trace.colorHexString;
                        // blank screen
                        // TODO - continuous pan would be better
                        ctx.stroke();
                        // blank screen if trace reaches end
                        if (x > width) {
                            trace.state = true;
                            scope.highlight(trace, true);
                            //scope.toggleReadButton(pinDef);
                            ctx.font = "10px Aria";
                            ctx.rect(0, 0, width, height);
                            ctx.fillStyle = "black";
                            ctx.fill();
                            var highlight = trace.color.getOriginalInput();
                            highlight.s = "90%";
                            var newColor = tinycolor(highlight);
                            ctx.fillStyle = trace.colorHexString;
                            // TODO - highlight saturtion of text
                            ctx.fillText('MAX ' + stats.max + '   ' + pinDef.name + ' ' + pinData.address, 10, minY);
                            ctx.fillText(('AVG ' + (stats.totalValue / stats.totalSample)).substring(0, 11), 10, height / 2);
                            ctx.fillText('MIN ' + stats.min, 10, maxY);
                            trace.posX = 0;
                        }
                        // draw it
                        ctx.closePath();
                    }
                    // for each pin
                    if (x > width) {
                        x = 0;
                    }
                    break;
                default:
                    // since we subscribed to "All" of Arduino's methods - most will escape here
                    // no reason to put an error .. however, it would be better to "Only" susbscribe to the ones
                    // we want
                    // console.log("ERROR - unhandled method " + inMsg.method);
                    break;
                }
            }
            ;
            scope.toggleReadWrite = function() {
                scope.readWrite = (scope.readWrite == 'write') ? 'read' : 'write';
            }
            ;
            scope.clearScreen = function(pinArray) {
                for (i = 0; i < pinArray.length; ++i) {
                    pinData = pinArray[i];
                    pinDef = scope.pinIndex[pinData.address];
                    _self.ctx = screen.getContext('2d');
                    // ctx.scale(1, -1); // flip y around for cartesian - bad idea :P
                    // width = screen.width;
                    //height = screen.height;
                    _self.ctx.rect(0, 0, width, height);
                    _self.ctx.fillStyle = "black";
                    _self.ctx.fill();
                    _self.ctx.fillStyle = "white";
                    stats = pinDef.stats;
                    _self.ctx.fillText(pinDef.name + (' AVG ' + (stats.totalValue / stats.totalSample)).substring(0, 11) + ' MIN ' + stats.min + ' MAX ' + stats.max, 10, 18);
                }
            }
            scope.zoomIn = function() {
                scaleX += 1;
                scaleY += 1;
                _self.ctx.scale(scaleX, scaleY);
            }
            ;// RENAME eanbleTrace - FIXME read values vs write values | ALL values from service not from ui !! - ui only sends commands
            scope.activateTrace = function(pinDef) {
                var trace = scope.oscope.traces[pinDef.address];
                if (trace.state) {
                    toggleReadButton(trace);
                    mrl.sendTo(name, 'disablePin', pinDef.address);
                    trace.state = false;
                } else {
                    toggleReadButton(trace);
                    mrl.sendTo(name, 'enablePin', pinDef.address);
                    trace.state = true;
                }
            }
            ;
            scope.reset = function() {
                mrl.sendTo(name, 'disablePins');
            }
            ;
            scope.write = function(pinDef) {
                scope.toggleWriteButton(trace);
                mrl.sendTo(name, 'digitalWrite', pinDef.address, 1);
                // trace.state = true;

                /* 3 states READ/ENABLE | DIGITALWRITE | ANALOGWRITE
                if (pinDef.pinName.charAt(0) == 'A') {
                    _self.toggleWriteButton(trace);
                    mrl.sendTo(name, 'analogWrite', 1);
                    trace.state = false;
                } else {
                    _self.toggleWriteButton(trace);
                    mrl.sendTo(name, 'digitalWrite', pinDef.address);
                    trace.state = true;
                }
                */
            }
            ;
            scope.reset = function() {
                mrl.sendTo(name, 'disablePins');
            }
            ;
            var toggleReadButton = function(trace) {
                var highlight = trace.color.getOriginalInput();
                if (trace.state) {
                    scope.highlight(trace, false);
                } else {
                    scope.highlight(trace, true);
                }
            };
            scope.highlight = function(trace, on) {
                var highlight = trace.color.getOriginalInput();
                if (!on) {
                    // scope.blah.display = false;
                    // on to off
                    highlight.s = "40%";
                    var newColor = color = tinycolor(highlight);
                    trace.readStyle = {
                        'background-color': newColor.toHexString()
                    };
                } else {
                    // scope.blah.display = true;
                    // off to on
                    highlight.s = "90%";
                    var newColor = color = tinycolor(highlight);
                    trace.readStyle = {
                        'background-color': newColor.toHexString()
                    };
                }
            }
            ;
            scope.toggleWriteButton = function(pinDef) {
                var highlight = trace.color.getOriginalInput();
                if (trace.state) {
                    // scope.blah.display = false;
                    // on to off
                    highlight.s = "40%";
                    var newColor = color = tinycolor(highlight);
                    trace.readStyle = {
                        'background-color': newColor.toHexString()
                    };
                } else {
                    // scope.blah.display = true;
                    // off to on
                    highlight.s = "90%";
                    var newColor = color = tinycolor(highlight);
                    trace.readStyle = {
                        'background-color': newColor.toHexString()
                    };
                }
            };
            // FIXME FIXME FIXME ->> THIS SHOULD WORK subscribeToServiceMethod  <- but doesnt
            mrl.subscribeToService(_self.onMsg, name);
            // this siphons off a single subscribe to the webgui
            // so it will be broadcasted back to angular
            mrl.subscribe(name, 'publishPinArray');
            mrl.subscribeToServiceMethod(_self.onMsg, name, 'publishPinArray');
            // initializing display data      
            setTraceButtons(service.pinIndex);
        }
    };
}
]);
