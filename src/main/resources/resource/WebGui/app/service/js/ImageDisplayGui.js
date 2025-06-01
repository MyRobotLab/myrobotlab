angular.module('mrlapp.service.ImageDisplayGui', []).controller('ImageDisplayGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('ImageDisplayGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.setWebViewer = function() {
        msg.send('setWebViewer')
    }
    $scope.setNativeViewer = function() {
        msg.send('setNativeViewer')
    }
    $scope.setAlwaysOnTop = function() {
        msg.send('setAlwaysOnTop')
    }

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
        case 'onStatus':
            console.info(data)
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    msg.subscribe(this)
}
])
