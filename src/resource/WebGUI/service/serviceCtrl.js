angular.module('mrlapp.service')

        .controller('ServiceCtrl', ['$scope', '$modal', '$ocLazyLoad', 'mrl', 'ServiceSvc',
            function ($scope, $modal, $ocLazyLoad, mrl, ServiceSvc) {

                $scope.anker = $scope.spawndata.name + '_-_' + $scope.spawndata.panelindex + '_-_';

                console.log('serviceShouldBeReady', $scope.spawndata);

                //load the service(-module) (lazy) (from the server)
                //TODO: should this really be done here?
                console.log('lazy-loading:', $scope.spawndata.type);
                $ocLazyLoad.load("service/js/" + $scope.spawndata.type + "gui.js").then(function () {
                    $scope.serviceloaded = true;
                }, function (e) {
                    console.log('lazy-loading wasnt successful:', $scope.spawndata.type);
                    $scope.servicenotfound = true;
                });

                //START_specific Service-Initialisation
                //.gui & .service are given to the specific service-UI
                $scope.inst = ServiceSvc.getServiceInstance($scope.spawndata.name);
                if ($scope.inst == null) {
                    console.log("ERROR - ServiceInstance not found!");
                }
                $scope.gui = $scope.inst.gui; //framework-section - DO NOT WRITE IN THERE!
                $scope.service = $scope.inst.service; //mrl-data-section

                //size
                if (!$scope.spawndata.size) {
                    $scope.spawndata.size = 'medium';
                    $scope.spawndata.oldsize = null;
                }

                //TODO: think of something better
                var initDone = false;
                $scope.gui.initDone = function () {
                    if (!initDone) {
                        initDone = true;
                        // create message bindings
                        mrl.subscribeToService($scope.gui.onMsg, $scope.service.name);
                    }
                };
                $scope.gui.panelcount = $scope.spawndata.panelcount;
                $scope.gui.setPanelCount = function (number) {
                    console.log('setting panelcount', number);
                    var old = $scope.gui.panelcount;
                    $scope.gui.panelcount = number;
                    ServiceSvc.notifyPanelCountChanged($scope.service.name, old, number);
                };

                //TODO: not completly happy
                //to be overriden
                if ($scope.gui.onMsg == null) {
                    $scope.gui.onMsg = function () {
                        console.log('ERR got message to default service endpoint!');
                    };
                }
                //END_specific Service-Initialisation

                //footer-size-change-buttons
                $scope.changesize = function (size) {
                    console.log("change size", $scope.service.name, size);
                    $scope.spawndata.oldsize = $scope.spawndata.size;
                    $scope.spawndata.size = size;
                    if (size == "full") {
                        //launch the service as a modal ('full')
                        var modalInstance = $modal.open({
                            animation: true,
                            templateUrl: 'service/servicefulltemplate.html',
                            controller: 'ServiceFullCtrl',
                            size: 'lg',
                            resolve: {
                                spawndata: function () {
                                    return $scope.spawndata;
                                },
                                gui: function () {
                                    return $scope.gui;
                                },
                                service: function () {
                                    return $scope.service;
                                }
                            }
                        });
                        //modal closed -> recover to old size
                        modalInstance.result.then(function () {
                            $scope.spawndata.size = $scope.spawndata.oldsize;
                            $scope.spawndata.oldsize = null;
                        }, function (e) {
                            $scope.spawndata.size = $scope.spawndata.oldsize;
                            $scope.spawndata.oldsize = null;
                        });
                    }
                };
            }]);
