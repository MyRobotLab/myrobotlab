/*

 dependencies - tinygradient, tinycolor

 FIXME - all data $scope.oscope

 FIXME - all msgs / onMsg need to mirror the service gui this directive was put into
 - that should not be the case

 TODO !!! - 
          *  all data in _self.oscope
          *  dynamically adjustable screen size
          *  zoom
          *  multi-line overlay (no erase)
          *  trace directive
          *  no crappy scope parent stuff
          *  all parameters passed in
          *  list dependencies

*/
angular.module('mrlapp.service').directive('oscope', ['$compile', 'mrl', '$log', function($compile, mrl, $log) {
    return {
        restrict: "E",
        templateUrl: 'widget/oscope.html',
        scope: {
            serviceName: '@',
            hide:'='
        },
        link: function(scope, element) {
            var _self = this;
            var name = scope.serviceName;
            var service = mrl.getService(name);
            var minY = 20;
            var maxY = 180;
            // FIXME - possibly screen specific
            var width = 800;
            var height = 200;
            var scaleX = 1;
            var scaleY = 1;
            scope.blah = {};
            scope.blah.display = false;
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
            scope.oscope.trace = {};
            // display update interfaces
            // defintion stage
            var setTraceButtons = function(pinIndex) {
                if (pinIndex == null ) {
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
                    scope.oscope.trace[key] = {};
                    var trace = scope.oscope.trace[key];
                    // adding style
                    var color = colorsHsv[parseInt(key)];
                    trace.style = {
                        'background-color': color.toHexString()
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
                    setTraceButtons(inMsg.data[0].pinIndex);
                    scope.$apply();
                    break;
                case 'onPinArray':
                    x++;
                    pinArray = inMsg.data[0];
                    for (i = 0; i < pinArray.length; ++i) {
                        pinData = pinArray[i];
                        pinDef = scope.pinIndex[pinData.address];
                        // get correct screen and references
                        var screen = document.getElementById('oscope-address-' + pinData.address);
                        var ctx = screen.getContext('2d');
                        var trace = scope.oscope.trace[pinData.address];
                        var stats = trace.stats;
                        ctx.font = "16px Aria";
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
                        if (x > width) {
                            ctx.rect(0, 0, width, height);
                            ctx.fillStyle = "black";
                            ctx.fill();
                            ctx.fillStyle = trace.colorHexString;
                            // TODO - highlight saturtion of text
                            ctx.fillText('MAX ' + stats.max + '   ' + pinDef.name + ' ' + pinData.address, 10, 20);
                            ctx.fillText(('AVG ' + (stats.totalValue / stats.totalSample)).substring(0, 11), 10, 98);
                            ctx.fillText('MIN ' + stats.min, 10, 180);
                        }
                        // draw it
                        ctx.stroke();
                        ctx.closePath();
                    }
                    // for each pin
                    if (x > width) {
                        x = 0;
                        trace.posX = 0;
                    }
                    break;
                default:
                    console.log("ERROR - unhandled method " + inMsg.method);
                    break;
                }
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
            ;
            scope.toggleTrace = function(pinDef) {
                var trace = scope.oscope.trace[pinDef.address];
                var highlight = trace.color.getOriginalInput();

                if (trace.state) {
                    scope.blah.display = false;
                    // on to off
                    highlight.s = "40%";
                    var newColor = color = tinycolor(highlight);
                    trace.style = {
                        'background-color': newColor.toHexString()
                    };
                    mrl.sendTo(name, 'disablePin', pinDef.address);
                    trace.state = false;
                } else {
                    scope.blah.display = true;
                    // off to on
                    highlight.s = "90%";
                    var newColor = color = tinycolor(highlight);
                    trace.style = {
                        'background-color': newColor.toHexString()
                    };
                    mrl.sendTo(name, 'enablePin', pinDef.address);
                    trace.state = true;
                }
            }
            ;
            // FIXME !!! - route only publishPinArray - not all the others 
            // this sends everything which is sent to angular
            // here for this service
            mrl.subscribeToService(_self.onMsg, name);
            // this siphons off a single subscribe to the webgui
            // so it will be broadcasted back to angular
            mrl.subscribe(name, 'publishPinArray');
            // initializing display data      
            setTraceButtons(service.pinIndex);
        }
    };
}
]);
