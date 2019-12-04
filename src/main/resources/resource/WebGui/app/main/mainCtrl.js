angular.module('mrlapp.main.mainCtrl', ['mrlapp.mrl'])
        .controller('mainCtrl', ['$scope', '$log', '$state', 'mrl',
            function ($scope, $log, $state, mrl) {
                $log.info('mainCtrl');
                // FIXME - while (!mrl.isConnected()){ sleep(3);}
                
                if (!mrl.isConnected()) {              
                    $log.info('redirect main-->loading');
                    $state.go('.main');
                } else {
                    $log.info('redirect main-->sub-state .main');
                    $state.go('.main');
                }
            }]);
