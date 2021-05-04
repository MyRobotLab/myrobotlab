angular.module('mrlapp.service.Adafruit16CServoDriverGui', []).controller('Adafruit16CServoDriverGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('Adafruit16CServoDriverGuiCtrl')
    var _self = this
    var msg = this.msg

    // init
    $scope.selectedControllerName = null
    $scope.selectedDeviceBus = null
    $scope.selectedDeviceAddress = null

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.selectedControllerName = service.controllerName
        $scope.selectedDeviceAddress = service.deviceAddress
        $scope.selectedDeviceBus = service.deviceBus
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }
    }

    $scope.attach = function() {
        msg.send('attach', $scope.selectedControllerName, $scope.selectedDeviceBus, $scope.selectedDeviceAddress)
    }

    $scope.detach = function() {
        msg.send('detach')        
    }

    msg.subscribe(this)
}
])
