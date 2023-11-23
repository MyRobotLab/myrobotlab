angular.module('mrlapp.service.NeoPixelGui', []).controller('NeoPixelGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('NeoPixelGuiCtrl')
    let _self = this
    let msg = this.msg

    $scope.rgb = []

    $scope.color = '000000'
    $scope.address = 0
    $scope.leds = []
    $scope.pins = []
    $scope.speeds = []
    $scope.types = ['RGB', 'RGBW']
    $scope.animations = ['Stop', 'Color Wipe', 'Larson Scanner', 'Theater Chase', 'Theater Chase Rainbow', 'Rainbow', 'Rainbow Cycle', 'Flash Random', 'Ironman']
    $scope.pixelCount = null

    // set pixel position
    $scope.pos = 0

    var firstTime = true
    $scope.brightnesses = [1, 2, 3, 4, 5, 6, 7, 8, 10, 15, 20, 25, 50, 75, 100, 125, 150, 175, 200, 225, 255]

    $scope.state = {
        controller: null
    }

    _self.uiPixelCount = 0

    for (i = 0; i < 50; i++) {
        $scope.pins.push(i)
        $scope.speeds.push(i + 1)
    }

    $scope.drawPixels = function() {
        if ($scope.service.pixelCount) {
            $scope.leds = []
            for (i = 0; i < $scope.service.pixelCount; ++i) {
                $scope.leds.push({
                    "address": i,
                    "style": {
                        "color": "white",
                        "background-color": "coral"
                    }
                })
            }
        }
        // if service.pixelCount
    }

    $scope.colorPickerOptions = {
        // format: 'hex',
        format: 'rgb',
        alpha: false,
        swatchOnly: true,
        horizontal: true,
        preserveInputFormat: true
    }

    // api event handlers
    $scope.eventApi = {
        onChange: function(api, color, $event) {
            $scope.color = color
            let colorstr = color.substring(4, color.length - 1).replace(/ /g, '').split(',')
            $scope.rgb = [parseInt(colorstr[0]), parseInt(colorstr[1]), parseInt(colorstr[2])]
            msg.send('setColor', $scope.rgb[0], $scope.rgb[1], $scope.rgb[2])
            $scope.address = api.getElement().attr('id')
            /*
            if ($scope.address == 'select') {
                msg.send('setColor', parseInt(rgb[0]), parseInt(rgb[1]), parseInt(rgb[2]))
            } else if ($scope.address == 'fill') {
                msg.send('fill', 0, $scope.service.pixelCount, parseInt(rgb[0]), parseInt(rgb[1]), parseInt(rgb[2]))
            } else {
                msg.send('setPixel', $scope.address, parseInt(rgb[0]), parseInt(rgb[1]), parseInt(rgb[2]))
            }
            */
        },
        onBlur: function(api, color, $event) {},
        onOpen: function(api, color, $event) {},
        onClose: function(api, color, $event) {},
        onClear: function(api, color, $event) {
            console.info('here')
        },
        onReset: function(api, color, $event) {},
        onDestroy: function(api, color) {},
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.pickedColor = 'rgb(' + service.red + ', ' + service.green + ', ' + service.blue + ')'
        $scope.rgb = [service.red, service.green, service.blue]
        $scope.color = $scope.pickedColor

        if ($scope.service.pixelCount != _self.uiPixelCount) {
            $scope.drawPixels()
        }

        if (firstTime && service.pixelCount) {
            $scope.pixelCount = service.pixelCount
        }

        if (firstTime && service.pin) {
            $scope.pin = service.pin
        }

        if ($scope.service.controller) {
            $scope.state.controller = $scope.service.controller
        }

        firstTime = false
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onSetCount':
            $scope.service.pixelCount = data
            $scope.drawPixels()
            $scope.$apply()
            break
        case 'onStatus':
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.clear = function() {
        msg.send('clear')
        $scope.pickedColor = 'rgb(0, 0, 0)'
        $scope.color = $scope.pickedColor
        msg.send('broadcastState')
    }

    $scope.setPin = function(pin) {
        $scope.pin = pin
        msg.send('setPin', pin)
        // msg.send('broadcastState')
    }

    $scope.fill = function() {
        msg.send('fill', $scope.rgb[0], $scope.rgb[1], $scope.rgb[2])
    }

    $scope.setPixel = function() {
        msg.send('setPixel', $scope.pos, $scope.rgb[0], $scope.rgb[1], $scope.rgb[2])
    }

    $scope.attach = function() {
        msg.send('setPin', $scope.pin)
        msg.send('setPixelCount', $scope.pixelCount)
        msg.send('attach', $scope.state.controller)
    }

    $scope.detach = function() {
        if ($scope.service.controller) {
            msg.send('detach', $scope.service.controller)
        }
    }

    $scope.setController = function(controller) {
        $scope.state.controller = controller
    }

    $scope.controllerOptions = {
        interface: 'NeoPixelController',
        attach: $scope.setController,
        // callback: function...
        attachName: $scope.service.controller,
        controllerTitle: 'controller'
    }

    $scope.drawPixels()
    msg.subscribe('setCount')
    msg.subscribe(this)
}
])
