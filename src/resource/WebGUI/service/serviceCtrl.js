angular.module('mrlapp.service')

        .controller('ServiceCtrl', ['$scope', '$modal', '$ocLazyLoad', 'mrl', 'ServiceSvc', '$log',
            function ($scope, $modal, $ocLazyLoad, mrl, ServiceSvc, $log) {

                $scope.anker = $scope.spawndata.name + '_-_' + $scope.spawndata.panelname + '_-_';

                $log.info('ServiceCtrl', $scope.spawndata.name);

                var isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };

                //load the service(-module) (lazy) (from the server)
                //TODO: should this really be done here?
                $log.info('lazy-loading:', $scope.spawndata.type);
                $ocLazyLoad.load("service/js/" + $scope.spawndata.type + "gui.js").then(function () {
                    $log.info('lazy-loading successful:', $scope.spawndata.type);
                    $scope.serviceloaded = true;
                }, function (e) {
                    $log.warn('lazy-loading wasnt successful:', $scope.spawndata.type);
                    $scope.servicenotfound = true;
                });

                //START_specific Service-Initialisation
                //get the service-data (same for all panels off a service)
                $scope.servicedata = ServiceSvc.getServiceInstance($scope.spawndata.name);

                $scope.cb = {};
                var controllerscope;
                $scope.cb.notifycontrollerisready = function (ctrlscope) {
                    $log.info('notifycontrollerisready', $scope.spawndata.name);
                    controllerscope = ctrlscope;
                    controllerscope.getService = function () {
                        return mrl.getService($scope.spawndata.name);
                    };
                    controllerscope.subscribe = function (method) {
                        return mrl.subscribe($scope.spawndata.name, method);
                    };
                    controllerscope.send = function (method, data) {
                        //TODO - what if it is has more than one data?
                        if (isUndefinedOrNull(data)) {
                            return mrl.sendTo($scope.spawndata.name, method);
                        } else {
                            return mrl.sendTo($scope.spawndata.name, method, data);
                        }
                    };
                    controllerscope.setPanelCount = function (number) {
                        $log.info('setting panelcount', number);
                        ServiceSvc.notifyPanelCountChanged($scope.spawndata.name, number);
                    };
                    controllerscope.setPanelNames = function (names) {
                        $log.info('setting panelnames', names);
                        ServiceSvc.notifyPanelNamesChanged($scope.spawndata.name, names);
                    };
                    controllerscope.setPanelShowNames = function (show) {
                        $log.info('setting panelshownames', show);
                        ServiceSvc.notifyPanelShowNamesChanged($scope.spawndata.name, show);
                    };
                    controllerscope.setPanelSizes = function (sizes) {
                        $log.info('setting panelsizes');
                        ServiceSvc.notifyPanelSizesChanged($scope.spawndata.name, sizes);
                    };
                    controllerscope.init();
                    mrl.subscribeToService(controllerscope.onMsg, $scope.spawndata.name);
                };
                //END_specific Service-Initialisation

                //service-menu-size-change-buttons
                $scope.changesize = function (size) {
//                    $log.info("change size", $scope.service.name, size);
//                    if (size == 'min') {
//                        $scope.spawndata.panelsize.oldsize = $scope.spawndata.panelsize.aktsize;
//                        $scope.spawndata.panelsize.aktsize = size;
//                        $scope.notifySizeChanged();
//                        ServiceSvc.movePanelToList($scope.spawndata.name, $scope.spawndata.panelname, 'min');
//                    } else if (size == 'unmin') {
//                        $scope.spawndata.panelsize.aktsize = $scope.spawndata.panelsize.oldsize;
//                        $scope.notifySizeChanged();
//                        ServiceSvc.movePanelToList($scope.spawndata.name, $scope.spawndata.panelname, 'main');
//                    } else {
//                        $scope.spawndata.panelsize.oldsize = $scope.spawndata.panelsize.aktsize;
//                        $scope.spawndata.panelsize.aktsize = size;
//                        $scope.notifySizeChanged();
//                        if ($scope.spawndata.panelsize.sizes[$scope.spawndata.panelsize.aktsize].fullscreen) {
//                            //launch the service as a modal ('full')
//                            var modalInstance = $modal.open({
//                                animation: true,
//                                templateUrl: 'service/servicefulltemplate.html',
//                                controller: 'ServiceFullCtrl',
//                                size: 'lg',
//                                resolve: {
//                                    spawndata: function () {
//                                        return $scope.spawndata;
//                                    },
//                                    gui: function () {
//                                        return $scope.gui;
//                                    },
//                                    service: function () {
//                                        return $scope.service;
//                                    }
//                                }
//                            });
//                            //modal closed -> recover to old size
//                            modalInstance.result.then(function () {
//                                $scope.spawndata.panelsize.aktsize = $scope.spawndata.panelsize.oldsize;
//                                $scope.spawndata.panelsize.oldsize = null;
//                                $scope.notifySizeChanged();
//                            }, function (e) {
//                                $scope.spawndata.panelsize.aktsize = $scope.spawndata.panelsize.oldsize;
//                                $scope.spawndata.panelsize.oldsize = null;
//                                $scope.notifySizeChanged();
//                            });
//                        }
//                    }
                };
            }]);
