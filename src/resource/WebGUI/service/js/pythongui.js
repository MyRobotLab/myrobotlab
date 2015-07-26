angular.module('mrlapp.service.pythongui', [])
        .controller('PythonGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('PythonGuiCtrl');

                // get fresh copy
                $scope.service = mrl.getService($scope.service.name);

                //you can access two objects
                //$scope.gui & $scope.service
                //$scope.gui contains some framwork functions related to your service panel
                //-> you can call functions on it, but NEVER write in it
                //$scope.service is your service-object, it is the representation of the service running in mrl

                //with this method, you can set how many panels you would like to show
                $scope.gui.setPanelCount(1);

                //you HAVE TO define this method &
                //it is the ONLY exception of writing into .gui
                //-> you will receive all messages routed to your service here
                $scope.gui.onMsg = function (msg) {
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
                $scope.gui.initDone();
            }]);
