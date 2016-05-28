angular.module('mrlapp.service.MarySpeechGui', [])
        .controller('MarySpeechGuiCtrl', ['$scope', '$log', 'mrl', '$uibModal', function ($scope, $log, mrl, $uibModal) {
                $log.info('MarySpeechGuiCtrl');
                var _self = this;
                var msg = this.msg;

                this.updateState = function (service) {
                    var installationstatechanged = false;
                    if (service.installationstate != $scope.service.state
                            || service.installationstate == 'installationprogress') {
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
                            case 'installationprogress':
                                var modalInstance = $uibModal.open({
                                    animation: true,
                                    templateUrl: 'MarySpeechInstallation' + service.installationstate + '.html',
                                    controller: 'MarySpeechInstallationNothingSelectedCtrl',
                                    size: 'sm',
                                    scope: $scope
                                });
                                break;
                            case 'showlicenses':
                                var modalInstance = $uibModal.open({
                                    animation: true,
                                    templateUrl: 'MarySpeechInstallation' + service.installationstate + '.html',
                                    controller: 'MarySpeechInstallationNothingSelectedCtrl',
                                    scope: $scope,
                                    size: 'lg'
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
                        if (value.isSelected) {
                            toInstall.push(value.name);
                        }
                    });
                    msg.send('installSelectedLanguagesAndVoices', toInstall);
                };

                msg.subscribe(this);
            }
        ])

        .controller('MarySpeechInstallationNothingSelectedCtrl', function ($scope, $uibModalInstance, $http, $sce) {
            $scope.close = $uibModalInstance.close;

            $scope.isUndefinedOrNull = function (val) {
                return angular.isUndefined(val) || val === null;
            };

            if ($scope.service.installationstate == 'showlicenses') {
                $scope.alllicenses = [];
                angular.forEach($scope.service.installationstateparam1, function (value, key) {
                    $scope.alllicenses.push(key);
                });
                $scope.counter = -1;

                $scope.showNextLicense = function () {
                    $scope.counter++;
                    console.log($scope.counter, $scope.alllicenses.length);
                    if ($scope.counter >= $scope.alllicenses.length) {
                        $scope.msg.installSelectedLanguagesAndVoices3();
                        $scope.close();
                    } else {
                        $scope.license = 'Loading license';
                        console.log($scope.alllicenses[$scope.counter]);
                        if (!$scope.isUndefinedOrNull($scope.alllicenses[$scope.counter])) {
                            $scope.license = $sce.trustAsHtml($scope.service.installationstateparam2[$scope.alllicenses[$scope.counter]]);
                        } else {
                            $scope.license = 'No license found';
                        }
                    }
                };

                $scope.showNextLicense();
            }
        });
