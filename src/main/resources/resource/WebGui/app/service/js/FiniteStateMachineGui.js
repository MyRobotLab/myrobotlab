angular.module('mrlapp.service.FiniteStateMachineGui', []).controller('FiniteStateMachineGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('FiniteStateMachineGuiCtrl')
    var _self = this
    var msg = this.msg

    // current state
    $scope.current = null
    // desired state when set
    $scope.state = null
    // event key to fire
    $scope.msgKey = null

    $scope.addTransition = function() {
        msg.send('addTransition', $scope.from, $scope.on, $scope.to)
        msg.send('broadcastState')
    }

    $scope.removeTransition = function(from, on, to) {
        msg.send('removeTransition', from, on, to)
        msg.send('broadcastState')
    }
    

    $scope.setCurrent = function() {
        msg.send('setCurrent', $scope.state)
        msg.send('broadcastState')
    }

    $scope.fire = function() {
        msg.send('fire', $scope.msgKey)
    }


    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if (service.current) {
            $scope.current = service.current.name
        }

    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onStatus':
            break
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onStateChange':
            $scope.current = data.current
            $scope.service.history.push(data)
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    msg.subscribe("publishStateChange")
    msg.subscribe(this)
}
])
