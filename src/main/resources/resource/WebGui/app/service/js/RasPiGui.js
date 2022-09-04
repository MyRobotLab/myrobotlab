angular.module('mrlapp.service.RasPiGui', []).controller('RasPiGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('RasPiGuiCtrl')
    var _self = this
    var msg = this.msg

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
        case 'XXXonPinDefinition':
            $scope.service.pinIndex[data.pin] = data
            $scope.service.addressIndex[data.address] = data
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.scan = function(bus){
        msg.send('scan', bus)
    }

    $scope.write = function(pinDef){
        msg.send('write', pinDef.address, pinDef.valueDisplay?1:0)
    }

    $scope.readWrite = function(pinDef) {
        console.info(pinDef)
        // FIXME - standardize interface with Arduino :(
        msg.send('pinMode', pinDef.pin, pinDef.readWrite?1:0)
    }

    msg.subscribe('publishPinDefinition')
    msg.subscribe(this)
}
])
