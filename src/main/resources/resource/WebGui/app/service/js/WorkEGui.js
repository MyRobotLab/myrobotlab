angular.module('mrlapp.service.WorkEGui', []).controller('WorkEGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('WorkEGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.mrl = mrl
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.port = service.port
    }

    // init scope variables
    $scope.pulseData = ''
    //$scope.saveP
    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'display':
            console.info('display - ' + data)
            mrl.display(data)
            $scope.$apply()
            break
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.getPeerState = function(key){
        let cls = ['btn']
        if ($scope.service.serviceType.peers[key].state == 'started'){
            cls.push('primary')
        } else {
            cls.push('default')
        }
        return 
    }

    msg.subscribe(this)
}
])

