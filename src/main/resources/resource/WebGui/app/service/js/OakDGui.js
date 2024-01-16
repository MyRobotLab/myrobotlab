angular.module('mrlapp.service.OakDGui', []).controller('OakDGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('OakDGuiCtrl')
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
        case 'onClassification':
              $scope.classification = data
              $scope.latency = Date.now() - data.ts
              $scope.$apply()             
            break
        case 'onImageToWeb':
              $scope.image = data
              $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    msg.subscribe('publishClassification')
    msg.subscribe('imageToWeb')    
    msg.subscribe(this)
}
])
