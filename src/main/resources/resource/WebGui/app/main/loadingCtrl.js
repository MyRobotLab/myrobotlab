angular.module('mrlapp.main.mainCtrl')
        .controller('loadingCtrl', ['$scope', '$log', 'mrl', 'panelSvc', '$state', '$previousState',
            function ($scope, $log, mrl, panelSvc, $state, $previousState) {
                $log.info('loadingCtrl.js');
                // test();
                var isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };

                $scope.mrlReady = false;
                $scope.serviceSvcReady = false;
                /*
                mrl.init().then(function () {
                    $log.info('mrl initialized!');
                    $scope.mrlReady = true;
                    if (panelSvc.isReady()) {
                        $log.info('panelSvc is already ready', panelSvc);
                        $scope.serviceSvcReady = true;
                        go();
                    } else {
                        $log.info('wating for panelSvc to become ready');
                        panelSvc.waitToBecomeReady().then(function () {
                            $log.info('panelSvc is now ready', panelSvc);
                            $scope.serviceSvcReady = true;
                            go();
                        });
                    }
                }, function (msg_) {
                    $log.info('mrl.init()-meh!');
                });
                */

                var go = function () {
                    var previousstate = $previousState.get();
                    if (isUndefinedOrNull(previousstate)) {
                        $log.warn('can"t go back - there is no back -> defaulting to /main');
                        $log.info('transitioning to state main');
                        $state.go('main');
                    } else {
                        $log.info('transitioning to state', previousstate.state.name, 'with params', previousstate.params);
                        $state.go(previousstate.state.name, previousstate.params);
                    }
                };
            }]);
