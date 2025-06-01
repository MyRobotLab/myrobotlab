angular.module('mrlapp.service.JMonkeyEngineGui', []).controller('JMonkeyEngineGuiCtrl', ['$scope', 'mrl', function($scope, mrl, ) {
    console.info('JMonkeyEngineGuiCtrl')
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    this.onMsg = function(inMsg) {
        data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            break
        case 'onStatus':
            console.info()
            break
        case 'onNodes':
            $scope.pulseData = data
            break
        case 'onSelectedPath':
            $scope.selectedPath = data
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    msg.subscribe('getSelectedPath')
    msg.subscribe('publishSelected')
    msg.subscribe(this)

}
])
