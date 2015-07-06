angular.module('mrlapp.service.templategui', [])
        .controller('TemplateGuiCtrl', ['$scope', 'mrl', function ($scope, mrl) {
                console.log('TemplateGuiCtrl');

                // get fresh copy
                $scope.service = mrl.getService($scope.service.name);

                //you can access two objects
                //$scope.gui & $scope.service
                //$scope.gui contains some framwork functions related to your service panel
                //-> you can call functions on it, but NEVER write in it
                //$scope.service is your service-object, it is the representation of the service running in mrl

                //with this method, you can set how many panels you would like to show
                $scope.gui.setPanelCount(1);
                //set custom panel-names
                $scope.gui.setPanelNames(['me1', 'me2', 'me3']);
                //set if panel-name should be shown or hidden (true->show)
                $scope.gui.setPanelShowNames([true, false, true]);
                //set custom-sizes
                $scope.gui.setPanelSizes([
                    {/*panel1*/
                        sizes: {
                            //size-options, these will be shown as a option to select from
                            //(and can be applied)
                            tinye: {
                                glyphicon: 'glyphicon glyphicon-minus', //define a glyphicon to show
                                width: 200, //width of this size-setting
                                body: 'collapse' //means that the body-section of the panel won't be shown
                            },
                            small: {
                                glyphicon: 'glyphicon glyphicon-resize-small',
                                width: 300
                            },
                            large: {
                                glyphicon: 'glyphicon glyphicon-resize-full',
                                width: 500
                            },
                            full: {
                                glyphicon: 'glyphicon glyphicon-fullscreen',
                                width: 0,
                                body: 'collapse'
                            }},
                        aktsize: 'large', //set this as the start-value
                        oldsize: null //save-point for size-changes
                    },
                    {/*panel2*/},
                    {/*panel3*/}]);

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
                            console.log("ERROR - unhandled method " + msg.method);
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
