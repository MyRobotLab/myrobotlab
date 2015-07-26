 /*

 dependenci - tinygradient, tinycolor


*/
angular.module('mrlapp.service')
.directive('oscope', ['$log', 'mrl', function($log, mrl) {
        return {
            restrict: "E",
            templateUrl: 'widget/oscope.html',
            link: function(scope, element) {
                var _self = this;

                // FIXEME - should be a better way than 
                // using the grandparent scope :P
                // WE DO NEED the service name who created us !
                // and to make oscopes work with other services they 
                // need to implement the same control & callback interfaces

                // oscope display interface should allow
                //  * dynamically changing the number of trace buttons
                //  * an interface to setup callback for publishing trace data
                //  * zoom
                //  * set gradient range

                // important var
                scope.pinButtonList = [];
                scope.pinData = [];
                scope.width = 0;
                scope.height = 0;
                
                var gradient = tinygradient([
                    // tinycolor('#ff0000'),       // tinycolor object
                    // {r: 0, g: 255, b: 0},       // RGB object
                    {h: 0,s: 0.4,v: 1,a: 1},  // HSVa object
                    {h: 240,s: 0.4,v: 1,a: 1} //, // HSVa object
                //'rgb(120, 120, 0)',         // RGB CSS string
                //'gold'                      // named color
                ]);
                
                var colorsHsv = null;

                // display update interfaces
                var setTraceButtons = function(pinList) {
                    scope.pinList = pinList;
                    
                    colorsHsv = gradient.hsv(pinList.length);
                    
                    scope.pinButtonList = [pinList.length];
                    //scope.buttonStyle = 
                    //scope.buttonText = [service.pinList.length];
                    
                    for (i = 0; i < pinList.length; ++i) {

                        // == creating a pin button & data ==
                        // adding the pin
                        var pin = pinList[i];
                        scope.pinButtonList[i] = pin;
                        // adding style
                        var color = colorsHsv[i];
                        scope.pinButtonList[i].style = {'background-color': color.toHexString()};
                        scope.pinButtonList[i].color = color;
                        scope.pinButtonList[i].text = "";
                        scope.pinButtonList[i].state = false; // off
                        scope.pinData[i] = {};
                        scope.pinData[i].posX = 0;
                        scope.pinData[i].posY = 0;
                        scope.pinData[i].count = 0;
                        scope.pinData[i].colorHexString = color.toHexString();

                        /*   NOT NUMBER COUNT BAsED !! - need only a pin definition */
                        // FIXME - add rx tx type (mask) or string !!!!
                        if (i == 0) {
                            scope.pinButtonList[i].text = 'rx 0';
                        } else if (i == 1) {
                            scope.pinButtonList[i].text = 'tx 1';
                        } /* else {
                            if (i > 52) {
                                scope.pinButtonList[i].text = 'A' + String("00" + (i - 53)).slice(-2);
                            } else {
                                scope.pinButtonList[i].text = String("000" + i).slice(-3); //i;
                            }
                        }
                        */
                        if (pin.type == 1) {
                            scope.pinButtonList[i].text = String("000" + i).slice(-3);
                        } else if (pin.type == 3) {
                            scope.pinButtonList[i].text = 'A' + String("00" + (i/* - 53*/)).slice(-2);
                        } else if (pin.type == 2) {
                            scope.pinButtonList[i].text = 'P' + String("00" + (i)).slice(-2);
                        } else {
                            scope.pinButtonList[i].text = '?' + String("00" + (i)).slice(-2);
                        }
                    
                    }
                
                }
                
                scope.onMsg = function(msg) {
                    $log.info('CALLBACK - ' + msg.method);
                    switch (msg.method) {
                        case 'onState':
                            // backend update 
                            setTraceButtons(msg.data[0].pinList);
                            scope.$apply();
                            break;
                        case 'onPin':
                            // FIXME - (optimization) pin should not have to send pintype - its known
                            // when pinButtonList is built
                            inPin = msg.data[0];
                            pinData = scope.pinData[inPin.pin];
                            button = scope.pinButtonList[inPin.pin];

                            // FIXME - nice to have an offset to 0 so the value 0 is visible
                            var y = 0;
                            
                            if (button.type == 1) {
                                // digital
                                y = scope.height - inPin.value - 10 * inPin.pin;
                            } else {
                                // analog
                                y = scope.height - inPin.value - 10;
                            }

                            // from
                            _self.ctx.moveTo(pinData.posX, pinData.posY);
                            // to
                            pinData.posX++;
                            pinData.posY = y;
                            _self.ctx.lineTo(pinData.posX, pinData.posY);
                            // color
                            _self.ctx.strokeStyle = pinData.colorHexString;
                            // draw it
                            _self.ctx.stroke();
                            break;
                        case 'onTX':
                            ++scope.txCount;
                            scope.tx += msg.data[0];
                            scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + msg.method);
                            break;
                    }
                };
                
                scope.toggleTrace = function(pin) {
                    
                    var button = scope.pinButtonList[pin];
                    var highlight = button.color.getOriginalInput();
                    
                    if (button.state) {
                        // on to off
                        highlight.s = "40%";
                        var newColor = color = tinycolor(highlight);
                        button.style = {'background-color': newColor.toHexString()};
                        if (button.type == 1) { // digital
                            mrl.sendTo(name, 'digitalReadPollingStop', pin);
                        } else { // analog
                            mrl.sendTo(name, 'analogReadPollingStop', pin);
                        }
                        button.state = false;
                    
                    } else {
                        // off to on
                        highlight.s = "90%";
                        var newColor = color = tinycolor(highlight);
                        button.style = {'background-color': newColor.toHexString()};
                        mrl.sendTo(name, 'analogReadPollingStart', pin);
                        if (button.type == 1) { // digital
                            mrl.sendTo(name, 'digitalReadPollingStart', pin);
                        } else { // analog
                            mrl.sendTo(name, 'analogReadPollingStart', pin);
                        }
                        
                        button.state = true;
                    }
                }

                // FIXME - get name through attribute
                // FIXME - create isolated scope !
                var serviceScope = scope.$parent.$parent;
                var name = serviceScope.service.name;
                var service = mrl.getService(name);

                // this sends everything which is sent to angular
                // here for this service
                mrl.subscribeToService(scope.onMsg, name);

                // this siphons off a single subscribe to the webgui
                // so it will be broadcasted back to angular
                mrl.subscribe(name, 'publishPin');

                // initializing display data      
                setTraceButtons(service.pinList);

                // set up call backs to interfaces
                // getPinList --> setTraceButtons
                // publishPin --> onTraceData

                // initial set of tracebuttons
                
                var screen = document.getElementById("screen");
                
                
                if (!screen.getContext) {
                    $log.error("could not find oscope screen")
                }
                
                this.ctx = screen.getContext('2d');
                // ctx.scale(1, -1); // flip y around for cartesian - bad idea :P
                scope.width = screen.width;
                scope.height = screen.height;
                ctx.rect(0, 0, screen.width, screen.height);
                ctx.fillStyle = "black";
                ctx.fill();

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
    }]);
