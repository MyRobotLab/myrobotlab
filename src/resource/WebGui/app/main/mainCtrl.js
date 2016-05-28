angular.module('mrlapp.main.mainCtrl', ['mrlapp.mrl'])
        .controller('mainCtrl', ['$scope', '$log', '$state', 'mrl', 'serviceSvc',
            function ($scope, $log, $state, mrl, serviceSvc) {
                $log.info('mainCtrl');
                
                $log.info(mrl.isConnected(), serviceSvc.isReady());
                if (!mrl.isConnected() || !serviceSvc.isReady()) {
                    //you shouldn't be here if something isn't ready yet
                    $log.info('redirect to loading from main');
                    $state.go('loading');
                } else {
                    $log.info('redirecting to sub-state .main');
                    $state.go('.main');
                }
            }]);
