angular.module('mrlapp.service.Adafruit16CServoDriverGui', []).controller('Adafruit16CServoDriverGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('Adafruit16CServoDriverGuiCtrl')
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
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

    $scope.setController = function(name) {
        $scope.service.controllerName = name
    }

    $scope.attach = function() {
        console.log($scope.controllerOptions)
        if (!$scope.service.controllerName){
            $scope.service.controllerName = $scope.controllerOptions.attachName
        }
        msg.send('attach', $scope.service.controllerName, $scope.service.deviceBus, $scope.service.deviceAddress)
    }

    $scope.detach = function() {
        msg.send('detach')
    }

    $scope.controllerOptions = {
        interface: 'I2CController',
        attach: $scope.setController,
        // callback: function...
        attachName: $scope.service.controllerName,
        controllerTitle: 'i2c controller'
    }

    msg.subscribe(this)
}
])
