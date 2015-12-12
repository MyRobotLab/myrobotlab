angular.module('mrlapp.service.MarySpeechGui', [])
        .controller('MarySpeechGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('MarySpeechGuiCtrl');
                var _self = this;
                var msg = this.msg;

                this.updateState = function (service) {
                    $scope.service = service;
                };
                _self.updateState($scope.service);

                $scope.collapse = true;
                $log.info('Mary-voices', $scope.service.possibleVoices);

                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
                        case 'onState':
                            _self.updateState(inMsg.data[0]);
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };
                
                $scope.install = function () {
                    var toInstall = [];
                    angular.forEach($scope.service.possibleVoices, function (value, key) {
                        if (value.install) {
                            toInstall.push(value.name);
                        }
                    });
                    msg.send('installSelectedLanguagesAndVoices', toInstall);
                };
                
                msg.subscribe(this);
            }
        ]);
