angular.module('mrlapp.service.NeoPixelGui', []).controller('NeoPixelGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('NeoPixelGuiCtrl')
    let _self = this
    let msg = this.msg

    $scope.color = '000000'
    $scope.address = 0
    $scope.leds = []
    $scope.pins = []
    $scope.speeds = []
    $scope.commonPixelCounts = [8, 12, 16, 24, 32, 64, 128, 256]
    $scope.animations = ['colorWipe', 'theaterChase', 'rainbow', 'scanner', 'randomFlash', 'theaterChaseRainbow', 'ironman', 'equalizer']

    _self.uiPixelCount = 0

    for (i = 0; i < 30; i++) {
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
            let rgb = color.substring(4, color.length - 1).replace(/ /g, '').split(',')
            $scope.address = api.getElement().attr('id')
            if ($scope.address == 'select') {
                msg.send('setColor', parseInt(rgb[0]), parseInt(rgb[1]), parseInt(rgb[2]))
            } else if ($scope.address == 'fill') {
                msg.send('fill', 0, $scope.service.pixelCount, parseInt(rgb[0]), parseInt(rgb[1]), parseInt(rgb[2]))
            } else {
                msg.send('setPixel', $scope.address, parseInt(rgb[0]), parseInt(rgb[1]), parseInt(rgb[2]))
            }
        },
        onBlur: function(api, color, $event) {},
        onOpen: function(api, color, $event) {},
        onClose: function(api, color, $event) {},
        onClear: function(api, color, $event) {},
        onReset: function(api, color, $event) {},
        onDestroy: function(api, color) {},
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if ($scope.service.pixelCount != _self.uiPixelCount) {
            $scope.drawPixels()
        }
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

    $scope.fill = function() {
        msg.send('fill')
    }

    $scope.attach = function(controller) {
        msg.send('attach', controller)
    }

    $scope.detach = function() {
        if ($scope.service.controller) {
            msg.send('detach', $scope.service.controller)
        }
    }

    $scope.drawPixels()
    msg.subscribe('setCount')
    msg.subscribe(this)
}
])
