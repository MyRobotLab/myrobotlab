angular.module('mrlapp.service')
.controller('ServiceCtrl', ['$scope', '$log', '$modal', 'mrl', 'serviceSvc', 
function($scope, $log, $modal, mrl, serviceSvc) {
    $log.info('ServiceCtrl', $scope.panel.name);
    
    var isUndefinedOrNull = function(val) {
        return angular.isUndefined(val) || val === null ;
    }
    ;
    
    //init all functions on gui's controller scope when controller is ready
    $scope.cb = {};
    var controllerscope;
    $scope.cb.notifycontrollerisready = function(ctrlscope) {
        $log.info('notifycontrollerisready', $scope.panel.name);
        controllerscope = ctrlscope;
        controllerscope.name = $scope.panel.name;
        controllerscope.getService = function() {
            return mrl.getService($scope.panel.name);
        }
        ;
        controllerscope.subscribe = function(method) {
            return mrl.subscribe($scope.panel.name, method);
        }
        ;
        controllerscope.send = function(method, data) {
            //TODO & FIXME !important! - what if it is has more than one data?
            if (isUndefinedOrNull(data)) {
                return mrl.sendTo($scope.panel.name, method);
            } else {
                return mrl.sendTo($scope.panel.name, method, data);
            }
        }
        ;
        controllerscope.setPanelSizes = function(sizes) {
            $log.info('setting panelsizes', sizes);
            serviceSvc.notifyPanelSizesChanged($scope.panel.name, sizes);
        }
        ;
        //FIXME - only do this (init & subscribeToService) ONCE per service
        controllerscope.init();
        mrl.subscribeToService(controllerscope.onMsg, $scope.panel.name);
    }
    ;
    
    //service-menu-size-change-buttons
    $scope.changesize = function(size) {
        $log.info("change size", $scope.panel.name, size);
        if (size == 'min') {
            $scope.panel.panelsize.oldsize = $scope.panel.panelsize.aktsize;
            $scope.panel.panelsize.aktsize = size;
            $scope.panel.notifySizeChanged();
            serviceSvc.movePanelToList($scope.panel.name, 'min');
        } else if (size == 'unmin') {
            $scope.panel.panelsize.aktsize = $scope.panel.panelsize.oldsize;
            $scope.panel.notifySizeChanged();
            serviceSvc.movePanelToList($scope.panel.name, 'main');
        } else {
            $scope.panel.panelsize.oldsize = $scope.panel.panelsize.aktsize;
            $scope.panel.panelsize.aktsize = size;
            $scope.panel.notifySizeChanged();
            if ($scope.panel.panelsize.sizes[$scope.panel.panelsize.aktsize].fullscreen) {
                //launch the service as a modal ('full')
                var modalInstance = $modal.open({
                    animation: true,
                    templateUrl: 'service/servicefulltemplate.html',
                    controller: 'ServiceFullCtrl',
                    size: 'lg',
                    scope: $scope
                });
                //modal closed -> recover to old size
                modalInstance.result.then(function() {
                    $scope.panel.panelsize.aktsize = $scope.panel.panelsize.oldsize;
                    $scope.panel.panelsize.oldsize = null ;
                    $scope.panel.notifySizeChanged();
                }
                , function(e) {
                    $scope.panel.panelsize.aktsize = $scope.panel.panelsize.oldsize;
                    $scope.panel.panelsize.oldsize = null ;
                    $scope.panel.notifySizeChanged();
                }
                );
            }
        }
    }
    ;
}
]);
