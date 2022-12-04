angular.module('mrlapp.service.HttpClientGui', []).controller('HttpClientGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('HttpClientGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.urls = []
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
        case 'onUrl':
            $scope.urls.push(data)
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

    msg.subscribe('publishUrl')
    msg.subscribe('publishText')
    msg.subscribe(this)
}
])
