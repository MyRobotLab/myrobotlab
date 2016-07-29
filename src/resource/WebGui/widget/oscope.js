/*

 dependencies - tinygradient, tinycolor

 FIXME - all data $scope.oscope

 FIXME - all msgs / onMsg need to mirror the service gui this directive was put into
 - that should not be the case

*/
angular.module('mrlapp.service').directive('oscope', ['$compile', 'mrl', '$log', function($compile, mrl, $log) {
    return {
        restrict: "E",
        templateUrl: 'widget/oscope.html',
        link: function(scope, element) {
            var _self = this;
            // WE DO NEED the service name who created us !
            // and to make oscopes work with other services they 
            // need to implement the same control & callback interfaces
            // oscope display interface should allow
            //  * dynamically changing the number of trace buttons
            //  * an interface to setup callback for publishing trace data
            //  * zoom
            //  * set gradient range
            // important var
            scope.pinIndex = {};
            scope.width = 0;
            scope.height = 0;
            scope.oscope = {};
            scope.oscope.name = 'screen';
            //scope.hscale = 0.5;
            scope.hscale = 1.0;
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
            var colorsHsv = null ;
            // FIXME - optimization - get pinIndex on change - includes pin type
            // use pinIndex defintion to determine details of incoming data
            // display update interfaces
            var setTraceButtons = function(pinIndex) {
                if (pinIndex == null ) {
                    return;
                }
                var size = Object.keys(pinIndex).length
                scope.pinIndex = pinIndex;
                colorsHsv = gradient.hsv(size);
                // pass over pinIndex add display data
                for (var key in pinIndex) {
                    if (!pinIndex.hasOwnProperty(key)) {
                        continue;
                    }
                    var pinDef = pinIndex[key];
                    // adding style
                    var color = colorsHsv[parseInt(key)];
                    pinDef.style = {
                        'background-color': color.toHexString()
                    };
                    pinDef.color = color;
                    pinDef.state = false;
                    // off
                    pinDef.posX = 0;
                    pinDef.posY = 0;
                    pinDef.count = 0;
                    pinDef.colorHexString = color.toHexString();
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
                        if (angular.isUndefined(pinDef.stats)) {
                            pinDef.stats = {
                                min: 1024,
                                max: 0,
                                totalValue: 0,
                                totalSample: 0
                            }
                        } else {
                            // TODO - sample rate Hz
                            pinDef.stats.totalSample++;
                            pinDef.stats.totalValue += pinData.value;
                            if (pinData.value < pinDef.stats.min) {
                                pinDef.stats.min = pinData.value;
                            }
                            if (pinData.value > pinDef.stats.max) {
                                pinDef.stats.max = pinData.value;
                            }
                        }
                        var y = scope.height - pinData.value;
                        // this certainly did not work
                        // ctx.putImageData(id, x, pinDev.posY);
                        _self.ctx.beginPath();
                        // from
                        _self.ctx.moveTo(x, pinDef.posY);
                        // to
                        pinDef.posY = y * scope.hscale;
                        _self.ctx.lineTo(x, pinDef.posY);
                        // color
                        _self.ctx.strokeStyle = pinDef.colorHexString;
                        // draw it
                        _self.ctx.stroke();
                        _self.ctx.closePath();
                    }
                    if (x > scope.width) {
                        x = 0;
                        scope.clearScreen(pinArray);
                    }
                    break;
                case 'onTX':
                    ++scope.txCount;
                    scope.tx += inMsg.data[0];
                    scope.$apply();
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
                    // scope.width = screen.width;
                    //scope.height = screen.height;
                    _self.ctx.rect(0, 0, scope.width, scope.height * scope.hscale);
                    _self.ctx.fillStyle = "black";
                    _self.ctx.fill();
                    _self.ctx.fillStyle = "white";
                    stats = pinDef.stats;
                    _self.ctx.fillText(pinDef.name + (' AVG ' + (stats.totalValue / stats.totalSample)).substring(0,11) + ' MIN ' + stats.min + ' MAX ' + stats.max , 10, 18);
                }
            }
            _self.scaleX = 1;
            _self.scaleY = 1;
            scope.zoomIn = function() {
                _self.scaleX += 1;
                _self.scaleY += 1;
                _self.ctx.scale(_self.scaleX, _self.scaleY);
            }
            scope.toggleTrace = function(pinDef) {
                var highlight = pinDef.color.getOriginalInput();
                if (pinDef.state) {
                    // on to off
                    highlight.s = "40%";
                    var newColor = color = tinycolor(highlight);
                    pinDef.style = {
                        'background-color': newColor.toHexString()
                    };
                    mrl.sendTo(name, 'disablePin', pinDef.address);
                    pinDef.state = false;
                } else {
                    // off to on
                    highlight.s = "90%";
                    var newColor = color = tinycolor(highlight);
                    pinDef.style = {
                        'background-color': newColor.toHexString()
                    };
                    mrl.sendTo(name, 'enablePin', pinDef.address);
                    pinDef.state = true;
                }
            }
            // FIXME - get name through attribute
            // FIXME - create isolated scope !
            var serviceScope = scope.$parent.$parent;
            // FIXME - this is a bit 'wack'
            var name = serviceScope.service.name;
            var service = mrl.getService(name);
            // this sends everything which is sent to angular
            // here for this service
            mrl.subscribeToService(_self.onMsg, name);
            // this siphons off a single subscribe to the webgui
            // so it will be broadcasted back to angular
            mrl.subscribe(name, 'publishPinArray');
            // initializing display data      
            setTraceButtons(service.pinIndex);
            // set up call backs to interfaces
            // getpinMap --> setTraceButtons
            // publishPin --> onTraceData
            // initial set of tracebuttons
            var screen = document.getElementById("screen");
            if (!screen.getContext) {
                $log.error("could not find oscope screen")
            }
            _self.ctx = screen.getContext('2d');
            //_self.ctx.font="12px Georgia";
            _self.ctx.font = "16px Aria";
            // ctx.scale(1, -1); // flip y around for cartesian - bad idea :P
            scope.width = screen.width;
            scope.height = screen.height;
            _self.ctx.rect(0, 0, screen.width, screen.height);
            _self.ctx.fillStyle = "black";
            _self.ctx.fill();
            // variable that decides if something should be drawn on mousemove
            var drawing = false;
            // the last coordinates before the current move
            var lastX;
            var lastY;
            element.bind('mousedown', function(event) {
                if (event.offsetX !== undefined) {
                    lastX = event.offsetX;
                    lastY = event.offsetY;
                } else {
                    lastX = event.layerX - event.currentTarget.offsetLeft;
                    lastY = event.layerY - event.currentTarget.offsetTop;
                }
                // begins new line
                ctx.beginPath();
                drawing = true;
            });
            element.bind('mousemove', function(event) {
                if (drawing) {
                    // get current mouse position
                    if (event.offsetX !== undefined) {
                        currentX = event.offsetX;
                        currentY = event.offsetY;
                    } else {
                        currentX = event.layerX - event.currentTarget.offsetLeft;
                        currentY = event.layerY - event.currentTarget.offsetTop;
                    }
                    draw(lastX, lastY, currentX, currentY);
                    // set current coordinates to last one
                    lastX = currentX;
                    lastY = currentY;
                }
            });
            element.bind('mouseup', function(event) {
                // stop drawing
                drawing = false;
            });
            // canvas reset
            function reset() {
                element[0].width = element[0].width;
            }
            function draw(lX, lY, cX, cY) {
                // line from
                ctx.moveTo(lX, lY);
                // to
                ctx.lineTo(cX, cY);
                // color
                ctx.strokeStyle = "#ccc";
                // draw it
                ctx.stroke();
            }
        }
    };
}
]);
