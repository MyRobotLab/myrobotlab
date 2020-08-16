angular.module('mrlapp.service.Adafruit16CServoDriverGui', []).controller('Adafruit16CServoDriverGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('Adafruit16CServoDriverGuiCtrl')
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if (!$scope.service.controllerName){
            $scope.service.controllerName = null
        }
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P
    msg.subscribe(this)
}
])
