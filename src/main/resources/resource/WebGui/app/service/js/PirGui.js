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
        // initialize attach directive (one time ???)
        $scope.options.attachName = service.controllerName
        $scope.options.isAttached = service.controllerName?true:false
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
        case 'onSense':
            console.info('onSense', data)
            $scope.service.isActive = data
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    _self.selectController = function(controller) {
        //$scope.service.controllerName = controller
        $scope.controllerName = controller
    }

    $scope.options = {
        interface: 'PinArrayControl',
        attach: _self.selectController,
        isAttached: $scope.service.controllerName // callback: function...
        // attachName: $scope.controllerName
    }

    $scope.attach = function() {
        msg.send('setPin', $scope.service.pin)
        msg.send('attach', $scope.controllerName)
    }

    $scope.detach = function() {
        // FIXME - fix this in the mrl framework
        // so I can call msg.send('detach')
        if ($scope.service.controllerName) {
            msg.send('detach', $scope.service.controllerName)
        }
    }

    $scope.enable = function() {
        msg.send('enable')
        msg.send('broadcastState')
    }

    $scope.disable = function() {
        msg.send('disable')
        msg.send('broadcastState')
    }

    // FIXME - which i could get rid of this
    // makes attach directive worky on first load   
    msg.subscribe('publishSense')
    msg.subscribe(this)
}
])
