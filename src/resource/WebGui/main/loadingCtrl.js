angular.module('mrlapp.main.mainCtrl')
        .controller('loadingCtrl', ['$scope', '$log', 'mrl', 'serviceSvc', '$state', '$previousState',
            function ($scope, $log, mrl, serviceSvc, $state, $previousState) {
                $log.info('loadingCtrl');

                var isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };

                $scope.mrlReady = false;
                $scope.serviceSvcReady = false;
                mrl.init().then(function () {
                    $log.info('mrl inited!');
                    $scope.mrlReady = true;
                    if (serviceSvc.isReady()) {
                        $log.info('serviceSvc is already ready', serviceSvc);
                        $scope.serviceSvcReady = true;
                        go();
                    } else {
                        $log.info('wating for serviceSvc to become ready');
                        serviceSvc.waitToBecomeReady().then(function () {
                            $log.info('serviceSvc is now ready', serviceSvc);
                            $scope.serviceSvcReady = true;
                            go();
                        });
                    }
                }, function (msg_) {
                    $log.info('mrl.init()-meh!');
                });

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
