angular.module('mrlapp.service.UltrasonicSensorGui', []).controller('UltrasonicSensorGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('UltrasonicSensorGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.controller = null
    $scope.continuous = true
    $scope.range = 0
    // $scope.service = {}
    // $scope.service.name = 'blah'

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        // if first - to not reset form
        $scope.options.attachName = service.controllerName
        $scope.options.isAttached = service.isAttached
    }

    // init scope variables
    $scope.pulseData = ''

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]

        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onRange':
            $scope.service.pingCount++
            $scope.range = data
            if ($scope.service.min == null || data < $scope.service.min) {
                $scope.service.min = data
            }
            if ($scope.service.max == null || data > $scope.service.max) {
                $scope.service.max = data
            }
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    this.selectController = function(controller) {
        $scope.controller = controller
    }

    $scope.options = {
        interface: 'UltrasonicSensorController',
        attach: this.selectController,
        // callback: function...
        attachName: null
    }

    $scope.attach = function() {
        msg.send('setTriggerPin', $scope.service.trigPin)
        msg.send('setEchoPin', $scope.service.echoPin)
        msg.send('attach', $scope.controller)
    }

    $scope.detach = function() {
        // FIXME - fix this in the mrl framework
        // so I can call msg.send('detach')
        if ($scope.service.controllerName) {
            msg.send('detach', $scope.service.controllerName)
        }

    }

    $scope.toggleRanging = function() {
        if (!$scope.service.isRanging) {
            msg.send('startRanging')
        } else {
            msg.send('stopRanging')
        }
        msg.send('broadcastState')
        $scope.$apply()
    }

    $scope.toggleRate = function() {
        if (!$scope.service.useRate) {
            msg.send('maxRate')
        } else {
            msg.send('useRate')
        }
        msg.send('broadcastState')
        $scope.$apply()
    }

    $scope.setRate = function() {
        const parsed = parseInt($scope.service.rateHz)
        msg.send('setRate', $scope.service.rateHz)
        msg.broadcastState()
    }

    // FIXME - which i could get rid of this
    // makes attach directive worky on first load

    msg.subscribe('publishRange')
    msg.subscribe(this)
}
])