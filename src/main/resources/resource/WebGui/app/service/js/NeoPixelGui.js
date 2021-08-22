angular.module('mrlapp.service.NeoPixelGui', []).controller('NeoPixelGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('NeoPixelGuiCtrl')
    let _self = this
    let msg = this.msg

    $scope.color = '000000'
    $scope.address = 0
    $scope.leds = []
    $scope.pins = []
    $scope.speeds = []
    $scope.types = ['RGB', 'RGBW']
    $scope.animations = ['No animation', 'Stop', 'Color Wipe', 'Larson Scanner', 'Theater Chase', 'Theater Chase Rainbow', 'Rainbow', 'Rainbow Cycle', 'Flash Random', 'Ironman', 'equalizer']
    $scope.pixelCount = null
    var firstTime = true

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
        onClear: function(api, color, $event) {
            console.info('here')
        },
        onReset: function(api, color, $event) {},
        onDestroy: function(api, color) {},
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if ($scope.service.pixelCount != _self.uiPixelCount) {
            $scope.drawPixels()
        }

        if (firstTime){
            $scope.pixelCount = service.pixelCount
            firstTime = false
        }

        if (!$scope.state.controller){
            $scope.state.controller = $scope.service.controller 
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

    $scope.attach = function() {
        msg.send('setPin', $scope.service.pin)
        msg.send('setPixelCount', $scope.pixelCount)
        msg.send('attach', $scope.state.controller)
    }

    $scope.detach = function() {
        if ($scope.service.controller) {
            msg.send('detach', $scope.service.controller)
        }
    }

    $scope.setController = function(controller){
        $scope.state.controller = controller
    }

    $scope.drawPixels()
    msg.subscribe('setCount')
    msg.subscribe(this)
}
])
