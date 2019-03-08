angular.module('mrlapp.main.mainCtrl', ['mrlapp.mrl'])
        .controller('mainCtrl', ['$scope', '$log', '$state', 'mrl', 'panelSvc',
            function ($scope, $log, $state, mrl, panelSvc) {
                $log.info('mainCtrl');
                // FIXME - while (!mrl.isConnected()){ sleep(3);}
                $log.info('mrl.isConnected', mrl.isConnected(), 'panelSvc.isRead', panelSvc.isReady());
                if (!mrl.isConnected() || !panelSvc.isReady()) {
                    //you shouldn't be here if something isn't ready yet
                    $log.info('redirect main-->loading');
                    $state.go('loading');
                } else {
                    $log.info('redirect main-->sub-state .main');
                    $state.go('.main');
                }
            }]);
