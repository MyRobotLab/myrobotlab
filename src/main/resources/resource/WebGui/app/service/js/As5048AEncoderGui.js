angular.module('mrlapp.service.As5048AEncoderGui', []).controller('As5048AEncoderGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('As5048AEncoderGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.controllers = []

    // published EncoderData
    $scope.data = null
  
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
        case 'onEncoderData':
            $scope.data = data
            $scope.$apply()
            break
        case 'onStatus':
			console.info("On status")
        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    };

    msg.subscribe('publishEncoderData')
    msg.subscribe(this)
}
])
