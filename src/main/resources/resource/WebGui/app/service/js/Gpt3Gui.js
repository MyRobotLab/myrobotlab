angular.module('mrlapp.service.Gpt3Gui', []).controller('Gpt3GuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('Gpt3GuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.utterances = []
    $scope.maxRecords = 500
    $scope.text = null

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }


    // init scope variables
    $scope.onTime = null
    $scope.onEpoch = null

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onUtterance':
            $scope.utterances.push(data)
            // remove the beginning if we are at maxRecords
            if ($scope.utterances.length > $scope.maxRecords) {
                $scope.utterances.shift()
            }
            $scope.$apply()
            break
        case 'onRequest':
            request = {"username":"friend", "text":data}
            $scope.utterances.push(request)
            // remove the beginning if we are at maxRecords
            if ($scope.utterances.length > $scope.maxRecords) {
                $scope.utterances.shift()
            }
            $scope.$apply()
            break
        case 'onEpoch':
            $scope.onEpoch = data
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    msg.subscribe('publishRequest')
    msg.subscribe('publishUtterance')
    msg.subscribe(this)
}
])
