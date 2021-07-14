angular.module('mrlapp.service.NeoPixel2Gui', []).controller('NeoPixel2GuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('NeoPixel2GuiCtrl')
    let _self = this
    let msg = this.msg

    $scope.color = '000000'
    $scope.address = 0
    $scope.leds = []
    $scope.commonPixelCounts = [8, 12, 16, 24, 32, 64, 128, 256]

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
        } // if service.pixelCount
    }

    $scope.colorPickerOptions = {
        format: 'hex',
        alpha: false,
        swatchOnly: true,
        horizontal: true,
        preserveInputFormat: true
    }

    // api event handlers
    $scope.eventApi = {
        onChange: function(api, color, $event) {
            $scope.color = color
            $scope.address = api.getElement().attr('id')
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
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.drawPixels()
    msg.subscribe('setCount')
    msg.subscribe(this)
}
])
