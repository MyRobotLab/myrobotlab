angular.module('mrlapp.service')

.controller('ServiceCtrl', ['$scope', '$modal', '$ocLazyLoad', 'mrl', 'ServiceSvc', '$log', 
    function($scope, $modal, $ocLazyLoad, mrl, ServiceSvc, $log) {
        
        $scope.anker = $scope.spawndata.name + '_-_' + $scope.spawndata.panelname + '_-_';
        
        $log.info('serviceShouldBeReady', $scope.spawndata);

        //load the service(-module) (lazy) (from the server)
        //TODO: should this really be done here?
        $log.info('lazy-loading:', $scope.spawndata.type);
        $ocLazyLoad.load("service/js/" + $scope.spawndata.type + "gui.js").then(function() {
            $scope.serviceloaded = true;
        }, function(e) {
            $log.info('lazy-loading wasnt successful:', $scope.spawndata.type);
            $scope.servicenotfound = true;
        });

        //START_specific Service-Initialisation
        //.gui & .service are given to the specific service-UI
        $scope.inst = ServiceSvc.getServiceInstance($scope.spawndata.name);
        if ($scope.inst == null) {
            $log.error("ERROR - ServiceInstance not found!");
        }
        $scope.gui = $scope.inst.gui; //framework-section - DO NOT WRITE IN THERE!
        $scope.service = $scope.inst.service; //mrl-data-section

        //size
        //                if (!$scope.spawndata.size) {
        //                    $scope.spawndata.size = 'medium';
        //                    $scope.spawndata.oldsize = null;
        //                }

        //TODO: think of something better
        var initDone = false;
        $scope.gui.initDone = function() {
            if (!initDone) {
                initDone = true;
                // create message bindings
                mrl.subscribeToService($scope.gui.onMsg, $scope.service.name);
            }
        };
        $scope.gui.panelcount = $scope.spawndata.panelcount;
        $scope.gui.setPanelCount = function(number) {
            $log.info('setting panelcount', number);
            var old = $scope.gui.panelcount;
            $scope.gui.panelcount = number;
            ServiceSvc.notifyPanelCountChanged($scope.service.name, old, number);
        };
        $scope.gui.setPanelNames = function(names) {
            $log.info('setting panelnames', names);
            ServiceSvc.notifyPanelNamesChanged($scope.service.name, names);
        };
        $scope.gui.setPanelShowNames = function(show) {
            $log.info('setting panelshownames', show);
            ServiceSvc.notifyPanelShowNamesChanged($scope.service.name, show);
        };
        $scope.gui.setPanelSizes = function(sizes) {
            $log.info('setting panelsizes');
            ServiceSvc.notifyPanelSizesChanged($scope.service.name, sizes);
        };

        //TODO: not completly happy
        //to be overriden
        if ($scope.gui.onMsg == null) {
            $scope.gui.onMsg = function() {
                console.log('ERR got message to default service endpoint!');
            };
        }
        //END_specific Service-Initialisation

        //footer-size-change-buttons
        $scope.changesize = function(size) {
            console.log("change size", $scope.service.name, size);
            $scope.spawndata.panelsize.oldsize = $scope.spawndata.panelsize.aktsize;
            $scope.spawndata.panelsize.aktsize = size;
            $scope.notifySizeChanged();
            if (size == "full") {
                //launch the service as a modal ('full')
                var modalInstance = $modal.open({
                    animation: true,
                    templateUrl: 'service/servicefulltemplate.html',
                    controller: 'ServiceFullCtrl',
                    size: 'lg',
                    resolve: {
                        spawndata: function() {
                            return $scope.spawndata;
                        },
                        gui: function() {
                            return $scope.gui;
                        },
                        service: function() {
                            return $scope.service;
                        }
                    }
                });
                //modal closed -> recover to old size
                modalInstance.result.then(function() {
                    $scope.spawndata.panelsize.aktsize = $scope.spawndata.panelsize.oldsize;
                    $scope.spawndata.panelsize.oldsize = null;
                    $scope.notifySizeChanged();
                }, function(e) {
                    $scope.spawndata.panelsize.aktsize = $scope.spawndata.panelsize.oldsize;
                    $scope.spawndata.panelsize.oldsize = null;
                    $scope.notifySizeChanged();
                });
            }
        };
    }]);
