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
            scope.service = mrl.getService(name)
            var mode = 'read'
            // 'read' || 'write'
            var width = 800
            var height = 100
            var margin = 10
            var minY = margin
            var maxY = height - margin

            scope.readWrite = 'read'
            // button toggle read/write
            // scope.blah = {}
            // scope.blah.display = false

            scope.pinIndex = service.pinIndex
            scope.addressIndex = service.addressIndex
            var x = 0
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

            // display update interfaces
            // defintion stage
            var setTraceButtons = function(pinIndex) {
                if (pinIndex == null) {
                    return
                }

                scope.addressIndex = mrl.getService(name).addressIndex
                        
                var size = Object.keys(pinIndex).length
                if (size && size > 0) {
                    scope.pinIndex = pinIndex
                    var colorsHsv = gradient.hsv(size)
                    // pass over pinIndex add display data
                    for (var pin in pinIndex) {
                        if (!pinIndex.hasOwnProperty(pin)) {
                            continue
                        }
                        scope.oscope.traces[pin] = {}
                        var trace = scope.oscope.traces[pin]
                        var pinDef = pinIndex[pin]

                        // adding style
                        var color = colorsHsv[pinDef.address]
                        if (!color) {
                            continue
                        }
                        trace.readStyle = {
                            'background-color': color.toHexString()
                        }
                        trace.writeStyle = {
                            'background-color': '#eee'
                        }
                        trace.color = color
                        trace.state = false
                        // off
                        trace.posX = 0
                        trace.posY = 0
                        trace.count = 0
                        trace.colorHexString = color.toHexString()
                        trace.stats = {
                            min: 0,
                            max: 1,
                            totalValue: 0,
                            totalSample: 1
                        }
                    }
                }
            }
            // FIXME this should be _self.onMsg = function(inMsg)
            this.onMsg = function(inMsg) {
                //console.log('CALLBACK - ' + msg.method)
                switch (inMsg.method) {
                case 'onState':
                    // backend update 
                    setTraceButtons(inMsg.data[0].pinIndex)
                    scope.$apply()
                    break
                case 'onPinArray':
                    pinArray = inMsg.data[0]

                    pinArray.forEach(pinData=>{
                        const pinDef = scope.pinIndex[pinData.pin]
                        const screen = document.getElementById(scope.serviceName + '-oscope-pin-' + pinData.pin)
                        const ctx = screen.getContext('2d')
                        const trace = scope.oscope.traces[pinData.pin]
                        const stats = trace.stats

                        if (trace.posX == 0) {

                            trace.state = true
                            scope.highlight(trace, true)
                            ctx.font = '10px Arial'
                            ctx.rect(0, 0, width, height)
                            ctx.fillStyle = 'black'
                            ctx.fill()

                            const highlight = trace.color.getOriginalInput()
                            highlight.s = '90%'
                            const newColor = tinycolor(highlight)
                            ctx.fillStyle = trace.colorHexString

                            ctx.fillText('MAX ' + stats.max + '   ' + pinDef.pin + ' ' + pinDef.address, 10, minY)
                            ctx.fillText(('AVG ' + (stats.totalValue / stats.totalSample)).substring(0, 11), 10, height / 2)
                            ctx.fillText('MIN ' + stats.min, 10, maxY)
                            trace.posX = 0
                        }

                        // Update stats
                        stats.totalSample++
                        stats.totalValue += pinData.value
                        stats.min = Math.min(stats.min, pinData.value)
                        stats.max = Math.max(stats.max, pinData.value)

                        const maxX = stats.max
                        const minX = stats.min
                        const c = minY + ((pinData.value - minX) * (maxY - minY)) / (maxX - minX)
                        const y = height - c

                        ctx.beginPath()
                        ctx.moveTo(trace.posX, trace.posY)
                        trace.posX++
                        trace.posY = y
                        ctx.lineTo(trace.posX, y)

                        ctx.strokeStyle = trace.colorHexString
                        ctx.stroke()
                        // ctx.closePath()

                        if (trace.posX > width) {
                            trace.posX = 0
                        }
                    }
                    )

                    break

                default:
                    // since we subscribed to "All" of Arduino's methods - most will escape here
                    // no reason to put an error .. however, it would be better to "Only" susbscribe to the ones
                    // we want
                    // console.log("ERROR - unhandled method " + inMsg.method)
                    break
                }
            }

            scope.toggleReadWrite = function() {
                scope.readWrite = (scope.readWrite == 'write') ? 'read' : 'write'
            }

            scope.clearScreen = function() {
                Object.keys(scope.oscope.traces).forEach(key=>{
                    let trace = scope.oscope.traces[key]
                    trace.posX = 0
                }
                )
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
                    mrl.sendTo(name, 'enablePin', pinDef.pin, 10)
                    trace.state = true
                }
            }

            scope.reset = function() {
                mrl.sendTo(name, 'disablePins')
            }

            scope.write = function(pinDef) {
                scope.toggleWriteButton(trace)
                mrl.sendTo(name, 'digitalWrite', pinDef.pin, 1)
                // trace.state = true

                /* 3 states READ/ENABLE | DIGITALWRITE | ANALOGWRITE
                if (pinDef.pinName.charAt(0) == 'A') {
                    _self.toggleWriteButton(trace)
                    mrl.sendTo(name, 'analogWrite', 1)
                    trace.state = false
                } else {
                    _self.toggleWriteButton(trace)
                    mrl.sendTo(name, 'digitalWrite', pinDef.address)
                    trace.state = true
                }
                */
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
            return obj
        }
        return Object.keys(obj).map(function(key) {
            return obj[key]
        })
    }

})
