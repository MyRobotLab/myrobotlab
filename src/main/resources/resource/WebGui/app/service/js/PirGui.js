angular.module('mrlapp.service.PirGui', []).controller('PirGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('PirGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.pinList = []
    for (let i = 0; i < 58; ++i) {
        $scope.pinList.push(i + '')
        // make strings 
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.options.attachName = service.config.controller
        $scope.options.isAttached = service.attached
        $scope.options.interface = 'PinArrayControl'

        // since attach broadcasts we'll get the pin list here
        if ($scope?.service?.config?.controller) {
            msg.send('getPinList', $scope.service.config.controller)
        }
    }

    // init scope variables
    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onStatus':
            console.info('onStatus', data)
            break
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onPinList':
            if (data && data.length){
                $scope.pinList = []
                for (var pinDef of data) {
                    $scope.pinList.push(pinDef.pin)
                }                
            }
            $scope.$apply()
            break
        case 'onSense':
            console.info('onSense', data)
            $scope.service.active = data
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    _self.selectController = function(controller) {
        //$scope.service.controllerName = controller
        $scope.service.config.controller = controller
        // get the pin list of the selected controller
        msg.send('setPinArrayControl', controller)
        msg.send('getPinList', controller)
    }

    $scope.options = {
        interface: 'PinArrayControl',
        attach: _self.selectController,
        // isAttached: $scope.service.config.controller // callback: function...
        // attachName: $scope.controllerName
    }

    $scope.attach = function() {
        msg.send('setPin', $scope.service.config.pin)
        msg.send('attach', $scope.service.config.controller)
        msg.send('enable')
    }

    $scope.detach = function() {
        if ($scope.service.config.controller) {
            msg.send('detach', $scope.service.config.controller)
        }
    }

    $scope.enable = function() {
        msg.send('enable')
        msg.send('broadcastState')
    }

    $scope.setPin = function() {
        if ($scope.service.config.pin) {
            msg.send('setPin', $scope.service.config.pin)
            msg.send('broadcastState')
        }
    }

    $scope.disable = function() {
        msg.send('disable')
        msg.send('broadcastState')
    }

    $scope.getActiveImage = function() {
        if ($scope.service.active) {
            return '../../green.png'
        } else if ($scope.service.active === false) {
            return '../../red.png'
        } else {
            // undefined / unknown
            return '../../grey.png'
        }
    }

    msg.subscribe('publishSense')
    msg.subscribe('getPinList')

    if ($scope?.service?.config?.controller) {
        msg.send('getPinList', $scope.service.config.controller)
    }

    msg.subscribe(this)
}
])
