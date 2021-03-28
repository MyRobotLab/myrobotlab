angular.module('mrlapp.service.MqttBrokerGui', []).controller('MqttBrokerGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('MqttBrokerGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.log = []
    $scope.pauseText = null
    $scope.topics = {
        "*": "*"
    }
    $scope.maxRecords = 500
    $scope.rowCount = 0
    $scope.reverse = true

    $scope.start = function() {

        msg.send('setUsername', $scope.service.username)
        msg.send('setPassword', $scope.service.password)
        msg.send('setAddress', $scope.service.address)
        msg.send('setMqttPort', $scope.service.mqttPort)
        msg.send('setWsPort', $scope.service.wsPort)
        msg.send('listen')
    }

    $scope.stop = function() {
        msg.send('stop')
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    // init scope variables
    $scope.pulseData = ''

    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0])
            $scope.$apply()
            break

        case 'onMqttMsg':
            let mqtt = inMsg.data[0]
            var length = $scope.log.length

            if ($scope.pauseText != "pause") {
                // events.forEach(function(e) {
                let date = new Date(mqtt.ts)
                mqtt.date = date.toLocaleTimeString('en-GB')
                $scope.log.push(mqtt)
                $scope.rowCount++
                $scope.topics[mqtt.topicName] = mqtt.topicName

                // remove the beginning if we are at maxRecords
                if ($scope.log.length > $scope.maxRecords) {
                    $scope.log.shift()
                }
            }

            // $scope.log.concat(msg.data[0])
            $scope.$apply()

            break
        case 'onStatus':
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    msg.subscribe('publishMqttMsg')
    msg.subscribe(this)
}
])
