angular.module('mrlapp.service.RuntimeGui', []).controller('RuntimeGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function($scope, $log, mrl, $timeout) {
    $log.info('RuntimeGuiCtrl')
    var _self = this
    var msg = this.msg

    var statusMaxSize = 2500;

    this.updateState = function(service) {
        $scope.service = service
    }

    _self.updateState(mrl.getService($scope.service.name))

    $scope.platform = $scope.service.platform
    $scope.status = ""
    $scope.cmd = ""
    $scope.registry = {}

    $scope.sendToCli = function() {
        console.log("sendToCli " + $scope.cmd)
        msg.send("sendToCli", $scope.cmd)
        $scope.cmd = ""
        //$scope.$apply()
    }

    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            $timeout(function() {
                _self.updateState(inMsg.data[0])
            })
            break
        case 'onLocalServices':
            {
                $scope.registry = inMsg.data[0]
                break
            }
        case 'onRegistered':
            {
                // inMsg.data[0]
                console.log("onRegistered")
                break
            }
        case 'onStatus':
            {
                $scope.status = inMsg.data[0].name + ' ' + inMsg.data[0].level + ' ' + inMsg.data[0].detail + "\n" + $scope.status
                if ($scope.status.length > 300) {
                    $scope.status = $scope.status.substring(0, statusMaxSize);
                }
                break
            }
        case 'onSendToCli':
            {
                if (inMsg.data[0] != null) {
                    $scope.status = JSON.stringify(inMsg.data[0], null, 2) + "\n" + $scope.status;
                    if ($scope.status.length > 300) {
                        $scope.status = $scope.status.substring(0, statusMaxSize);
                    }
                    $scope.$apply()
                } else {
                    $scope.status += "null\n"
                }
                break
            }
        case 'onReleased':
            {
                $log.info("runtime - onRelease" + inMsg.data[0])
                break
            }
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.possibleServices = mrl.getPossibleServices()
    msg.subscribe("getLocalServices")
    msg.subscribe("registered")
    msg.subscribe("sendToCli")
    msg.send("getLocalServices")
    msg.subscribe(this)
}
])
