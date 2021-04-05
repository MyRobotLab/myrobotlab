angular.module('mrlapp.service.MqttGui', [])
.controller('MqttGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('MqttGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.log = []
    $scope.maxRecords = 500
    $scope.rowCount = 0
    $scope.reverse = false
    $scope.sentMsgCnt = 0
    $scope.status = null
    $scope.publishTopic = null
    $scope.subscribeTopic = null
    $scope.sentMsgCnt = 0
    $scope.recvMsgCnt = 0
    $scope.topics = {
        "*": "*"
    }
    $scope.reverse = true
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }
            
    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
                _self.updateState(inMsg.data[0])
            break
        case 'onPulse':
                $scope.pulseData = inMsg.data[0]
            break
        case 'onStatus':
                $scope.status = inMsg.data[0]
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
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.publish = function(publishTopic, data, qos) {
        $scope.sentMsgCnt++
        msg.send('publish', publishTopic, data, qos)
    }


    $scope.connect = function(service) {
        if ($scope.service.username) {
            msg.send('setUsername', $scope.service.username)
        }
        if ($scope.service.password) {
            msg.send('setPassword', $scope.service.password)
        }
        msg.send('setClientId', $scope.service.clientId)
        msg.send('setUrl', $scope.service.url)
        // TODO - qos
        msg.send('connect')
    }
    
    
    msg.subscribe('publishMqttMsg')
    msg.subscribe(this)
}
])
