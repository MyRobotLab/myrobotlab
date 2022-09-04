angular.module('mrlapp.service.AudioFileGui', []).controller('AudioFileGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('AudioFileGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.playFile = function() {
        msg.send('playFile', $scope.selectedFile)
    }

    // init

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
        default:
            $log.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    msg.subscribe(this)
}
])
