angular.module('mrlapp.service.TrackingGui', []).controller('TrackingGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('TrackingGuiCtrl')
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.panOptions.attachName = service.config.peers['pan'].name
        $scope.tiltOptions.attachName = service.config.peers['tilt'].name
        $scope.pidOptions.attachName = service.config.peers['pid'].name
        $scope.cvOptions.attachName = service.config.peers['cv'].name

    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]

        switch (inMsg.method) {
        case 'onStatus':
            // FIXME - do something with this
            console.info('onStatus', data)
            break
        case 'onStats':
            // FIXME - do something with this
            $scope.service.stats = data
            $scope.$apply()
            break
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onTrackingState':
            $scope.service.state = data
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

    $scope.panOptions = {
        interface: 'ServoControl',
        attach: this.selectController,
        controllerTitle: 'pan',
        // callback: function...
        attachName: null
    }

    $scope.tiltOptions = {
        interface: 'ServoControl',
        attach: this.selectController,
        controllerTitle: 'tilt',
        // callback: function...
        attachName: null
    }

    $scope.cvOptions = {
        interface: 'ComputerVision',
        attach: this.selectController,
        controllerTitle: 'opencv',
        // callback: function...
        attachName: null
    }

    $scope.pidOptions = {
        interface: 'PidControl',
        attach: this.selectController,
        controllerTitle: 'pid',
        // callback: function...
        attachName: null
    }

    $scope.attach = function() {
        if ($scope.panOptions.attachName){
            msg.send('attachPan', $scope.panOptions.attachName)    
        }
        
        if ($scope.tiltOptions.attachName){
            msg.send('attachTilt', $scope.tiltOptions.attachName)
        }

        if ($scope.cvOptions.attachName){
            msg.send('attachCv', $scope.cvOptions.attachName)
        }

        if ($scope.pidOptions.attachName){
            msg.send('attachPid', $scope.pidOptions.attachName)
        }

    }

    $scope.detach = function() {
        // FIXME - fix this in the mrl framework
        // so I can call msg.send('detach')
        if ($scope.service.controllerName) {
            msg.send('detach', $scope.service.controllerName)
        }
    }

    $scope.rest = function() {
        msg.send('rest')
    }
    
    // FIXME - which i could get rid of this
    // makes attach directive worky on first load
    msg.subscribe('publishTrackingState')
    msg.subscribe('publishStats')
    msg.subscribe(this)
}
])
