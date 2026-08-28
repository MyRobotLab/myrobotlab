angular.module('mrlapp.service.OakDGui', []).controller('OakDGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('OakDGuiCtrl')
    var _self = this
    var msg = this.msg

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
        case 'onClassification':
              $scope.classification = data
              $scope.latency = Date.now() - data.ts
              $scope.$apply()             
            break
        case 'onImageToWeb':
              $scope.image = data
              $scope.$apply()
            break
        case 'onDepthHud':
              $scope.depthHud = data
              $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.startDepth = function() {
        msg.send('startDepth')
    }
    $scope.stopDepth = function() {
        msg.send('stopDepth')
    }
    $scope.startSynthetic = function() {
        msg.send('startSyntheticDepth')
    }
    $scope.knownDistanceM = 1

    $scope.setRgbMesh = function() {
        msg.send('setRgbMesh', $scope.service.config.rgbMesh)
    }

    $scope.setDepthCloudScale = function() {
        msg.send('setDepthCloudScale', parseFloat($scope.service.depthCloudScale))
    }

    $scope.resetDepthCloudScale = function() {
        $scope.service.depthCloudScale = 1
        msg.send('setDepthCloudScale', 1)
    }

    $scope.calibrateDepthScale = function() {
        msg.send('calibrateDepthScale', parseFloat($scope.knownDistanceM))
    }

    msg.subscribe('publishClassification')
    msg.subscribe('imageToWeb')
    msg.subscribe('publishDepthHud')
    msg.subscribe(this)
}
])
