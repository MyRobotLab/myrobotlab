angular.module('mrlapp.service.RosGui', []).controller('RosGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('RosGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.mrl = mrl
    $scope.topics = []
    $scope.maxRecords = 100
    $scope.log = []
    $scope.rowCount = 0

    $scope.state = {
        selectedTopic: null
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if (service.config.subscriptions) {
            service.config.subscriptions.sort()
        }
        if (service.connected) {
            msg.send('rosCallService', '/rosapi/topics')
        }

    }

    // init scope variables

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onRosMsg':
            // demux
            let rosMsg = data
            // JSON.parse(data)
            if (rosMsg.service == "/rosapi/topics") {
                $scope.topics = rosMsg.values.topics
                $scope.topics.sort()
            } else {
                // TODO - default dump to rotating log
                console.info('onRosMsg', rosMsg)
                rosMsg.ts = Date.now()
                $scope.log.unshift(rosMsg)
                $scope.rowCount++

                if ($scope.log.length > $scope.maxRecords) {
                    $scope.log.pop()
                }

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

    $scope.connect = function() {
        msg.send('connect', $scope.service.config.bridgeUrl)
        msg.send('broadcastState')
    }

    $scope.disconnect = function() {
        msg.send('disconnect')
        msg.send('broadcastState')
    }

    $scope.subscribe = function() {
        msg.send('rosSubscribe', $scope.state.selectedTopic)
        msg.send('broadcastState')
    }

    $scope.unsubscribe = function() {
        msg.send('rosUnsubscribe', $scope.state.selectedTopic)
        msg.send('broadcastState')
    }

    $scope.publish = function() {
        msg.send('rosPublish', $scope.state.selectedTopic, $scope.publishMsg)
    }

    $scope.clear = function() {
        $scope.log = []
        $scope.rowCount = 0
    }

    msg.subscribe('publishRosMsg')
    msg.subscribe(this)

}
])
