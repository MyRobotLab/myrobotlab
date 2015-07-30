angular.module('mrlapp.service.guiservicegui', [])
        .controller('GUIServiceGuiCtrl', ['$scope', 'mrl', function ($scope, mrl) {
                $log.info('GUIServiceGuiCtrl');

                // get fresh copy
                $scope.service = mrl.getService($scope.service.name);

                //you can access two objects
                //$scope.panel & $scope.service
                //$scope.panel contains some framwork functions related to your service panel
                //-> you can call functions on it, but NEVER write in it
                //$scope.service is your service-object, it is the representation of the service running in mrl


                //you HAVE TO define this method &
                //it is the ONLY exception of writing into .gui
                //-> you will receive all messages routed to your service here
                $scope.panel.onMsg = function (msg) {
                    switch (msg.method) {
                        case 'onPulse':
                            $scope.pulseData = msg.data[0];
                            $scope.$apply();
                            break;
                        case 'onClockStarted':
                            $scope.label = "Stop";
                            $scope.$apply();
                            break;
                        case 'onClockStopped':
                            $scope.label = "Start";
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + msg.method);
                            break;
                    }
                };

                //you can subscribe to methods
                mrl.subscribe($scope.service.name, 'pulse');
                mrl.subscribe($scope.service.name, 'clockStarted');
                mrl.subscribe($scope.service.name, 'clockStopped');

                //after you're done with setting up your service-panel, call this method
                $scope.panel.initDone();
            }]);
