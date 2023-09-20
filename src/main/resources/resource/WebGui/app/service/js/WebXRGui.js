angular.module('mrlapp.service.WebXRGui', []).controller('WebXRGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('WebXRGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.poses = {}
    $scope.events = {}
    $scope.jointAngles = {}

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
        case 'onPose':
            $scope.poses[data.name] = data
            $scope.$apply()
            break
        case 'onEvent':
            $scope.events[data.uuid] = data
            $scope.$apply()
            break
        case 'onJointAngles':
            $scope.jointAngles = {...$scope.jointAngles, ...data} 
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    msg.subscribe('publishPose')
    msg.subscribe('publishEvent')
    msg.subscribe('publishJointAngles')
    msg.subscribe(this)
}
])
