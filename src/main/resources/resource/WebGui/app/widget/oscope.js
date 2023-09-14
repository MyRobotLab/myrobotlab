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
angular.module('mrlapp.service').directive('oscope', ['mrl', function(mrl) {
    return {
        restrict: "E",
        templateUrl: 'widget/oscope.html',
        scope: {
            serviceName: '@'
        },
        // scope: true,
        link: function(scope, element) {
            var _self = this
            var name = scope.serviceName
            var service = mrl.getService(name)
            var width = 800
            var height = 100
            var margin = 10
            var minY = margin
            var maxY = height - margin
            var scaleX = 1
            var scaleY = 1
            scope.readWrite = 'read'
            scope.pinIndex = service.pinIndex;
            // var x = 0
            var gradient = tinygradient([{
                h: 0,
                s: 0.4,
                v: 1,
                a: 1
            }, {
                h: 240,
                s: 0.4,
                v: 1,
                a: 1
            }])
            scope.oscope = {}
            scope.oscope.traces = {}
            scope.oscope.writeStates = {}

            var setTraceButtons = function(pinIndex) {

                if (Object.keys(scope.oscope.traces).length > 0) {
                    return
                }

                // let pinIndex = service.pinIndex
                var size = Object.keys(pinIndex).length
                if (size > 0) {
                    var colorsHsv = gradient.hsv(size)

                    Object.keys(pinIndex).forEach(function(pin) {
                        if (!pinIndex.hasOwnProperty(pin)) {
                            return
                        }
                        var trace = {
                            readStyle: {},
                            state: false,
                            posX: 0,
                            posY: 0,
                        x0: 0,
                        y0: 0,
                        x1: 0,
                        y1: 0,
                            count: 0,
                            stats: {
                                min: 0,
                                max: 1,
                                totalValue: 0,
                                totalSample: 1
                            }
                        }
                        var pinDef = pinIndex[pin]
                        var color = colorsHsv[pinDef.address]
                        if (!color) {
                            return
                        }
                        trace.readStyle['background-color'] = color.toHexString()
                        trace.color = color
                        trace.colorHexString = color.toHexString()
                        scope.oscope.traces[pin] = trace
                    })
                }
            }
            // FIXME this should be _self.onMsg = function(inMsg)
            this.onMsg = function(inMsg) {
                //console.log('CALLBACK - ' + msg.method)
                switch (inMsg.method) {
                case 'onState':
                    // backend update
                    scope.pinIndex = inMsg.data[0].pinIndex 
                    setTraceButtons(inMsg.data[0].pinIndex)
                    scope.$apply()
                    break
                case 'onPinArray':
                    // all pin traces are going to be traced at the same x position
                    // x++
                    pinArray = inMsg.data[0]
                    for (i = 0; i < pinArray.length; ++i) {
                        // get pin data & definition
                        pinData = pinArray[i]
                        pinDef = scope.pinIndex[pinData.pin]
                        // get correct screen and references
                        // change to LET !!
                        let screen = document.getElementById(scope.serviceName + '-oscope-pin-' + pinData.pin)
                        let ctx = screen.getContext('2d');
                        let trace = scope.oscope.traces[pinData.pin]
                        let stats = trace.stats
                                
                        // blank screen if trace reaches end
                        if (trace.x1 > width || trace.x0 == 0) {
                            trace.state = true
                            scope.highlight(trace, true)
                            ctx.font = "10px Aria"
                            ctx.rect(0, 0, width, height)
                            ctx.fillStyle = "black"
                            ctx.fill()
                            var highlight = trace.color.getOriginalInput()
                            highlight.s = "90%"
                            var newColor = tinycolor(highlight)
                            ctx.fillStyle = trace.colorHexString
                            // TODO - highlight saturtion of text
                            ctx.fillText('MAX ' + stats.max + '   ' + pinDef.pin + ' ' + pinDef.address, 10, minY)
                            ctx.fillText(('AVG ' + (stats.totalValue / stats.totalSample)).substring(0, 11), 10, height / 2)
                            ctx.fillText('MIN ' + stats.min, 10, maxY)
                            trace.x0 = 0
                            trace.x1 = 0
                        }
                        // draw it


                                
                        // TODO - sample rate Hz
                        trace.stats.totalSample++
                        trace.stats.totalValue += pinData.value
                        if (pinData.value < trace.stats.min) {
                            trace.stats.min = pinData.value
                        }
                        if (pinData.value > trace.stats.max) {
                            trace.stats.max = pinData.value
                        }
                        var maxX = trace.stats.max
                        var minX = trace.stats.min
                        var c = minY + ((pinData.value - minX) * (maxY - minY)) / (maxX - minX)
                        var y = height - c
                        ctx.beginPath()
                        // move to last position...
                        ctx.moveTo(trace.x0, trace.y0)
                        trace.x1++
                        trace.y1 = y
                        // draw line to x1,y1 
                        ctx.lineTo(trace.x1, trace.y1)
                        // save current values
                        trace.x0 = trace.x1
                        trace.y0 = trace.y1
                        // color
                        ctx.strokeStyle = trace.colorHexString
                        // blank screen
                        // TODO - continuous pan would be better
                        ctx.stroke()
                        ctx.closePath()
                    }
                    break
                default:
                    // since we subscribed to "All" of Arduino's methods - most will escape here
                    // no reason to put an error .. however, it would be better to "Only" susbscribe to the ones
                    // we want
                    // console.log("ERROR - unhandled method " + inMsg.method)
                    break
                }
            }

            scope.clearScreen = function(pinArray) {
                for (i = 0; i < scope.pinArray.length; ++i) {
                    pinData = pinArray[i]
                    pinDef = scope.pinIndex[pinData.pin]
                    _self.ctx = screen.getContext('2d')
                    _self.ctx.rect(0, 0, width, height)
                    _self.ctx.fillStyle = "black"
                    _self.ctx.fill()
                    _self.ctx.fillStyle = "white"
                    stats = pinDef.stats
                    _self.ctx.fillText(pinDef.name + (' AVG ' + (stats.totalValue / stats.totalSample)).substring(0, 11) + ' MIN ' + stats.min + ' MAX ' + stats.max, 10, 18)
                }
            }
            // RENAME eanbleTrace - FIXME read values vs write values | ALL values from service not from ui !! - ui only sends commands
            scope.activateTrace = function(pinDef) {
                var trace = scope.oscope.traces[pinDef.pin]
                if (trace.state) {
                    toggleReadButton(trace)
                    mrl.sendTo(name, 'disablePin', pinDef.pin)
                    trace.state = false
                } else {
                    toggleReadButton(trace)
                    // mrl.sendTo(name, 'enablePin', pinDef.pin)
                    mrl.sendTo(name, 'enablePin', pinDef.pin, 1)
                    trace.state = true
                }
            }

            scope.reset = function() {
                mrl.sendTo(name, 'disablePins')
            }

            scope.reset = function() {
                mrl.sendTo(name, 'disablePins')
            }

            var toggleReadButton = function(trace) {
                var highlight = trace.color.getOriginalInput()
                if (trace.state) {
                    scope.highlight(trace, false)
                } else {
                    scope.highlight(trace, true)
                }
            }
            scope.highlight = function(trace, on) {
                var highlight = trace.color.getOriginalInput()
                if (!on) {
                    // scope.blah.display = false
                    // on to off
                    highlight.s = "40%"
                    var newColor = color = tinycolor(highlight)
                    trace.readStyle = {
                        'background-color': newColor.toHexString()
                    }
                } else {
                    // scope.blah.display = true
                    // off to on
                    highlight.s = "90%"
                    var newColor = color = tinycolor(highlight)
                    trace.readStyle = {
                        'background-color': newColor.toHexString()
                    }
                }
            }
            scope.toggleWriteButton = function(pinDef) {
                var highlight = trace.color.getOriginalInput()
                if (trace.state) {
                    // scope.blah.display = false
                    // on to off
                    highlight.s = "40%"
                    var newColor = color = tinycolor(highlight)
                    trace.readStyle = {
                        'background-color': newColor.toHexString()
                    }
                } else {
                    // scope.blah.display = true
                    // off to on
                    highlight.s = "90%"
                    var newColor = color = tinycolor(highlight)
                    trace.readStyle = {
                        'background-color': newColor.toHexString()
                    }
                }
            }
            // FIXME FIXME FIXME ->> THIS SHOULD WORK subscribeToServiceMethod  <- but doesnt
            mrl.subscribeToService(_self.onMsg, name)
            // this siphons off a single subscribe to the webgui
            // so it will be broadcasted back to angular
            mrl.subscribe(name, 'publishPinArray')
            mrl.subscribeToServiceMethod(_self.onMsg, name, 'publishPinArray')
            // initializing display data     

            setTraceButtons(service.pinIndex)
        }
    }
}
]).filter('toArray', function() {
    return function(obj) {
        if (!angular.isObject(obj)) {
            return obj;
        }
        return Object.keys(obj).map(function(key) {
            return obj[key];
        });
    }
    ;
});
