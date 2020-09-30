angular.module('mrlapp.service.RasPiGui', [])
    .controller('RasPiGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function ($scope, $log, mrl, $timeout) {
        $log.info('RasPiGuiCtrl')
        var _self = this
        var msg = this.msg

        // GOOD TEMPLATE TO FOLLOW
        this.updateState = function (service) {
            $scope.service = service
        }


        this.onMsg = function (inMsg) {
            let data = inMsg.data[0]
            switch (inMsg.method) {
                case 'onState':
                    $timeout(function () {
                        _self.updateState(data)
                    })
                    break
                case 'onPulse':
                    $timeout(function () {
                        $scope.pulseData = data
                    })
                    break
                default:
                    $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
                    break
            }
        }

        msg.subscribe('pulse')
        msg.subscribe(this)
    }
    ])       
