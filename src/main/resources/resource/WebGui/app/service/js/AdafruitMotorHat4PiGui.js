angular.module('mrlapp.service.AdafruitMotorHat4PiGui', []).controller('AdafruitMotorHat4PiGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('AdafruitMotorHat4PiGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.controllers = []

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    // init scope variables
    $scope.onTime = null
    $scope.onEpoch = null

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        
        case 'onAttachMatrix':
            if (data['org.myrobotlab.service.interfaces.I2CController']) {
                $scope.controllers = data['org.myrobotlab.service.interfaces.I2CController']
            }

            console.info($scope.controllers)
            $scope.$apply()
            break

        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    msg.subscribe(this)
    msg.sendTo('runtime', 'requestAttachMatrix')
}
])
