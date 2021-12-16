angular.module('mrlapp.service.MouthControlGui', []).controller('MouthControlGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('MouthControlGuiCtrl')
    let _self = this
    let msg = this.msg
    $scope.mrl = mrl
    // should be done in framework

    $scope.servos = []
    $scope.speechServices = []

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

        case 'onServoControl':
            $scope.servos = data
            $scope.$apply()
            break

        case 'onSpeechSynthesis':
            $scope.speechServices = data
            $scope.$apply()
            break

        case 'onStatus':
            break

        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.attach = function(fullname) {
        msg.send('attach', fullname)
    }

    $scope.update = function() {
        let service = $scope.service
        msg.send('setDelays', service.delaytime, service.delaytimestop, service.delaytimeletter)
        msg.send('setMouth', service.mouthClosedPos, service.mouthOpenedPos)
        msg.send('broadcastState')
    }

    $scope.servoOptions = {
        interface: 'SpeechSynthesis',
        attach: $scope.attach,
        // callback: function...
        attachName: $scope.mouth
    }

    $scope.speechOptions = {
        interface: 'ServoControl',
        attach: $scope.attach,
        // callback: function...
        attachName: $scope.servo
    }

    // msg.subscribe('publishAvailableInterfaces')
    msg.subscribe(this)
    // FIXME - optimize by putting it into msg.subscribe(this)

    // NOT YET READY
    // msg.sendTo('runtime', 'requestAttachMatrix')

}
])
