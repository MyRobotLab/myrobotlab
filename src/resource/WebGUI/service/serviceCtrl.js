angular.module('mrlapp.service')

        .controller('ServiceCtrl', ['$scope', '$modal', '$ocLazyLoad', 'mrl', 'ServiceSvc',
            function ($scope, $modal, $ocLazyLoad, mrl, ServiceSvc) {
                console.log('testing', $scope);
                
               $scope.anker = $scope.spawndata.name + '_-_' + $scope.spawndata.panelindex + '_-_';

                var isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };

//                //make sure $scope.spawndata is there
//                var listener = $scope.$watch(function () {
//                    return $scope.spawndata;
//                }, function () {
//                    if (!isUndefinedOrNull($scope.spawndata.name)) {
//                        listener();
//                        init();
//                    }
//                });

//                var init = function () {
                    console.log('serviceShouldBeReady', $scope.spawndata);

                    //load the service(-module) (lazy) (from the server)
                    //TODO: should this really be done here?
                    console.log('lazy-loading:', $scope.spawndata.type);
                    $ocLazyLoad.load("service/js/" + $scope.spawndata.type + "gui.js").then(function () {
                        $scope.serviceloaded = true;
                    });

                    //START_specific Service-Initialisation
                    //"inst" is given to the specific service-UI
                    $scope.inst = ServiceSvc.getServiceInstance($scope.spawndata.name);
                    if ($scope.inst == null) {
                        $scope.inst = {};
                        $scope.inst.gui = {}; //framework-section - DO NOT WRITE IN THERE!
                        $scope.inst.service = mrl.getService($scope.spawndata.name); //mrl-data-section
                        ServiceSvc.addServiceInstance($scope.spawndata.name, $scope.inst);
                    }
                    $scope.gui = $scope.inst.gui;
                    $scope.service = $scope.inst.service;

                    //TODO: refactor
                    console.log("$scope,size", $scope.size);
                    if ($scope.size != null && $scope.size.lastIndexOf("force", 0) == 0) {
                        $scope.gui.oldsize = $scope.gui.size;
                        $scope.gui.size = $scope.size.substring(5, $scope.size.length);
                        $scope.gui.forcesize = true;
                    } else {
                        if ($scope.gui.oldsize != null) {
                            $scope.gui.size = $scope.gui.oldsize;
                            $scope.gui.oldsize = null;
                        }
                        $scope.gui.forcesize = false;
                    }
                    if (!$scope.gui.size) {
                        $scope.gui.size = "medium";
                        $scope.gui.oldsize = null;
                    }

                    //TODO: think of something better
                    var initDone = false;
                    $scope.gui.initDone = function () {
                        if (!initDone) {
                            initDone = true;
                            // create message bindings
                            mrl.subscribeToService($scope.service.onMsg, $scope.service.name);
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
                    if ($scope.service.onMsg == null) {
                        $scope.service.onMsg = function () {
                        };
                    }
                    //END_specific Service-Initialisation
//                };

                //footer-size-change-buttons
                $scope.changesize = function (size) {
                    console.log("button clicked", size);
                    $scope.gui.oldsize = $scope.gui.size;
                    $scope.gui.size = size;
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
                            $scope.gui.size = $scope.gui.oldsize;
                            $scope.gui.oldsize = null;
                        });
                    }
                };

//                angular.element(document).ready(function () {
//                    console.log('Hello World');
//                    mrl.registerForServices($scope.createService);
//                    mrl.connect(document.location.origin.toString() + '/api/messages');
//                });

            }]);
