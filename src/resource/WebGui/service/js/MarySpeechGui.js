angular.module('mrlapp.service.MarySpeechGui', [])
        .controller('MarySpeechGuiCtrl', ['$scope', '$log', 'mrl', '$modal', function ($scope, $log, mrl, $modal) {
                $log.info('MarySpeechGuiCtrl');
                var _self = this;
                var msg = this.msg;

                this.updateState = function (service) {
                    var installationstatechanged = false;
                    if (service.installationstate != $scope.service.state) {
                        installationstatechanged = true;
                    }
                    $scope.service = service;
                    console.log('mary-updateState', service);
                    if (installationstatechanged) {
                        switch (service.installationstate) {
                            case 'noinstallationstarted':
                                break;
                            case 'nothingselected':
                            case 'installcomponents':
                                var modalInstance = $modal.open({
                                    animation: true,
                                    templateUrl: 'MarySpeechInstallationNothingSelected.html',
                                    controller: 'MarySpeechInstalltionNothingSelectedCtrl',
                                    size: 'sm',
                                    scope: $scope
                                });
                                break;
                        }
                    }
                };
                _self.updateState($scope.service);

                $scope.collapse = true;
                $log.info('Mary-voices', $scope.service.possibleVoices);
                $scope.componenturl = $scope.service.INSTALLFILEURL;
                console.log('mary', $scope.service);

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
        ])

        .controller('MarySpeechInstalltionNothingSelectedCtrl', function ($scope, $modalInstance) {
            $scope.close = function () {
                $modalInstance.close();
            };
        });
