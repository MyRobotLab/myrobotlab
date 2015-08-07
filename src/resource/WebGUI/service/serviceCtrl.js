// FIXME FIXME FIXME
// THIS IS NOT SERVICE SPECIFIC !! MEANING IT NEEDS TO BE COMBINED WITH mainCtrl !!!
// DO NOT ADD NEW DATA TO service
// panel should probably be retrieved from serviceSvc
angular.module('mrlapp.service')
.controller('ServiceCtrl', ['$log', '$rootScope', '$compile', '$scope', '$modal', '$ocLazyLoad', 'mrl', 'serviceSvc', '$http', 
function($log, $rootScope, $compile, $scope, $modal, $ocLazyLoad, mrl, serviceSvc, $http) {
    // TEMPORARY $scope variables !!!!
    // this runs through for each service - 
    // all scope data is temporary unless its put into an associative  array
    // the name switches every time.
    // $scope.anker = name + '_-_' + $scope.service.panelname + '_-_';
    var name = $scope.service.name;
    var panel = serviceSvc.getServicePanel(name);
    
    $scope.anker = name;
    $scope.panel = panel;
    
    $log.info('ServiceCtrl', name);
    
    var type = $scope.panel.type;
    
    $log.info('lazy-loading:', type);
    $ocLazyLoad.load("service/js/" + type + "gui.js").then(function() {
        $scope.serviceloaded = true;
        // FIXME why is this needed?
    }
    , function(e) {
        $log.info('lazy-loading wasnt successful:', type);
        $scope.servicenotfound = true;
    }
    );
    
    
    //TODO: think of something better
    var initDone = false;
    panel.initDone = function() {
        if (!initDone) {
            initDone = true;
            // create message bindings
            mrl.subscribeToService(panel.onMsg, name);
        }
    }
    ;
    
    panel.setPanelNames = function(names) {
        $log.info('setting panelnames', names);
    }
    ;
    panel.setPanelShowNames = function(show) {
        $log.info('setting panelshownames', show);
    }
    ;
    panel.setPanelSizes = function(sizes) {
        $log.info('setting panelsizes');
        serviceSvc.notifyPanelSizesChanged(name, sizes);
    }
    ;
    
    //TODO: not completly happy
    //to be overriden  
    // - What is this GAP ?
    if (panel.onMsg == null ) {
        panel.onMsg = function() {
            $log.error('empty onMsg body!');
        }
        ;
    }
    ;
    
    $scope.setShow = function(val) {
        $log.info('setShow ' + $scope.show);
        $scope.show = val;
    }
    ;
    
    $scope.setPosition = function(x, y) {
        $log.info('setPosition ', x, ',', y);
        // FIXME !! MERGE STYLE !!! 
        // $scope.style = "{position:'static', top:" + 50 +", left:" + 50 +"}";
        $scope.style = "{'color':'green','top':" + 250 + ", 'left':" + 250 + "}";
        //$scope.$apply();
    }
    ;
    
    
    $scope.test = function() {
        var serviceList = angular.element(document.querySelector('#serviceList'));
        $http.get("service/service.html")
        .then(function(response) {
            //$('body').prepend($compile(response.data)($scope));
            serviceList.prepend($compile(response.data)($scope));
        }
        );
    }
    ;
    
    
    
    //footer-size-change-buttons
    $scope.changesize = function(size) {
        $log.info("change size", $scope.service.name, size);
        if (size == 'min') {
            $scope.panel.panelsize.oldsize = $scope.panel.panelsize.aktsize;
            $scope.panel.panelsize.aktsize = size;
            $scope.notifySizeChanged();
            serviceSvc.movePanelToList($scope.panel.name, $scope.panel.panelname, 'min');
        } else if (size == 'unmin') {
            $scope.panel.panelsize.aktsize = $scope.panel.panelsize.oldsize;
            $scope.notifySizeChanged();
            serviceSvc.movePanelToList($scope.panel.name, $scope.panel.panelname, 'main');
        } else {
            $scope.panel.panelsize.oldsize = $scope.panel.panelsize.aktsize;
            $scope.panel.panelsize.aktsize = size;
            $scope.notifySizeChanged();
            if ($scope.panel.panelsize.sizes[$scope.panel.panelsize.aktsize].fullscreen) {
                //launch the service as a modal ('full')
                var modalInstance = $modal.open({
                    animation: true,
                    templateUrl: 'service/servicefulltemplate.html',
                    controller: 'ServiceFullCtrl',
                    size: 'lg',
                    resolve: {
                        panel: function() {
                            return $scope.panel;
                        },
                        gui: function() {
                            return $scope.panel;
                        },
                        service: function() {
                            return $scope.service;
                        }
                    }
                });
                //modal closed -> recover to old size
                modalInstance.result.then(function() {
                    $scope.panel.panelsize.aktsize = $scope.panel.panelsize.oldsize;
                    $scope.panel.panelsize.oldsize = null ;
                    $scope.notifySizeChanged();
                }
                , function(e) {
                    $scope.panel.panelsize.aktsize = $scope.panel.panelsize.oldsize;
                    $scope.panel.panelsize.oldsize = null ;
                    $scope.notifySizeChanged();
                }
                );
            }
        }
    }
    ;
}
]);
