angular.module('mrlapp.service.PidGui', []).controller('PidGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('PidGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.maxRecords = 25

    // offsets of indexes
    const input = 0
    const setpoint = 1
    const output = 2

    $scope.option = {
        selected: null
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service

        // iterate through all pids
        for ([key,pidData] of Object.entries(service.data)) {
            console.log(key, pidData);

            // hold signal
            pidData.hold = false

            pidData.show = true

            // creating and organizing chart data labels etc
            pidData.chart = {
                data: [[], [], []],
                series: [key + ' input', key + ' setpoint', key + ' output'],
                labels: []
            }
        }
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onStatus':
            break
        case 'onPid':

            let pidData = $scope.service.data[data.key]

            if (pidData.hold) {
                // FIXME - hold per pidData
                break
            }

            // $scope.recordIndex++
            pidData.chart.data[input].unshift(data.input)
            pidData.chart.data[output].unshift(pidData.setpoint + data.value)
            pidData.chart.data[setpoint].unshift(pidData.setpoint)

            // x-axis time labels
            ts = new Date(data.ts)
            pidData.chart.labels.unshift(ts.getSeconds() * 1000 + ts.getMilliseconds())

            // pop the end off when maxRecords is reached - move window
            if (pidData.chart.data[input].length > $scope.maxRecords) {

                pidData.chart.data[input].pop()
                pidData.chart.data[output].pop()
                pidData.chart.data[setpoint].pop()

                // x-axis time labels
                pidData.chart.labels.pop()
            }

            $scope.$apply()
            break
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }
    }

    $scope.select = function() {
        console.info('selected ', $scope.option.selected)
    }

    $scope.delete = function(key) {
        msg.send('deletePid', key)
        msg.send('broadcastState')
    }

    $scope.update = function(key) {
        // text fields to null if empty
        if ($scope.option.selected.outMin == '') {
            $scope.option.selected.outMin = null
        }
        if ($scope.option.selected.outMax == '') {
            $scope.option.selected.outMax = null
        }
        msg.send('addPid', $scope.option.selected)
        msg.send('broadcastState')
    }

    $scope.hold = function(pidData) {
        pidData.hold = true
    }

    $scope.release = function(pidData) {
        pidData.hold = false
    }

    $scope.show = function(pidData) {
        pidData.show = true
    }

    $scope.hide = function(pidData) {
        pidData.show = false
    }

    $scope.reset = function(pidData) {
        msg.send('reset', pidData.key)
    }

    $scope.enable = function(pidData) {
        // toggle
        msg.send('enable', pidData.key, !pidData.enabled)
        msg.send('broadcastState')
    }

    msg.subscribe('publishPid')
    // msg.subscribe('addPid')
    msg.subscribe(this)

    $scope.onClick = function(points, evt) {
        console.log(points, evt)
    }

    $scope.options = {

        elements: {
            line: {
                fill: false
            }
        },
        animation: false,
        type: 'line'
    }

}
])
