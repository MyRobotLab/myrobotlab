angular.module('mrlapp.service.JMonkeyEngineGui', []).controller('JMonkeyEngineGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('JMonkeyEngineGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.knownDistanceM = 1

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
        case 'onStatus':
            break
        case 'onNodes':
            $scope.pulseData = data
            $scope.$apply()
            break
        case 'onSelectedPath':
            $scope.selectedPath = data
            $scope.$apply()
            break
        case 'onClickDistance':
            if ($scope.service) {
                $scope.service.lastClickDistanceM = data
            }
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.setDepthCloudScale = function() {
        msg.send('setDepthCloudScale', parseFloat($scope.service.config.depthCloudScale))
    }

    $scope.setReachCloud = function() {
        msg.send('setReachCloud', !!$scope.service.config.reachCloud)
    }

    $scope.resetDepthCloudScale = function() {
        $scope.service.config.depthCloudScale = 1
        msg.send('setDepthCloudScale', 1)
    }

    $scope.calibrateDepthScale = function() {
        msg.send('calibrateDepthScale', parseFloat($scope.knownDistanceM))
    }

    msg.subscribe('getSelectedPath')
    msg.subscribe('publishSelected')
    msg.subscribe('publishClickDistance')
    msg.subscribe(this)

}
])
