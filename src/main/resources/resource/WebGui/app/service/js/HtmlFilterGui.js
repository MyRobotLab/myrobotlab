angular.module('mrlapp.service.HtmlFilterGui', []).controller('HtmlFilterGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('HtmlFilterGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.rawText = ""
    $scope.text = ""

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
        case 'onRawText':
            $scope.rawText = data
            $scope.$apply()
            break
        case 'onText':
            $scope.text = data
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    msg.subscribe('publishRawText')
    msg.subscribe('publishText')
    msg.subscribe(this)
}
])
