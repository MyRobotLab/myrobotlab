angular.module('mrlapp.service.IntroGui', []).controller('IntroGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function($scope, $log, mrl, $timeout) {
    $log.info('IntroGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.mrl = mrl
    $scope.panel = mrl.getPanel('runtime')

    $scope.props = {}

    $scope.activePanel = 'settings'

    // GOOD Intro TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.props = service.props
        console.info($scope.props.servo01IsActive)
        console.info('here')
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
            // servo event in the past 
            // meant feedback from MRLComm.c
            // but perhaps its come to mean
            // feedback from the service.moveTo
        case 'onStatus':
            console.log(data)
            $scope.$apply()
            break

        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }
    }

    $scope.setPanel = function(panelName) {
        $scope.activePanel = panelName
    }

    $scope.showPanel = function(panelName) {
        return $scope.activePanel == panelName
    }

    $scope.setPanel('extension')

    $scope.get = function(key) {
        let ret = $scope.service.props[key]
        return ret
    }    

    msg.subscribe(this)
    msg.subscribe("registered")

}
])
