angular.module('mrlapp.main.mainCtrl')
        .controller('loadingCtrl', ['$scope', '$log', 'mrl', 'serviceSvc', '$state', '$previousState',
            function ($scope, $log, mrl, serviceSvc, $state, $previousState) {
                $log.info('loadingCtrl');

                mrl.init().then(function () {
                    $log.info('mrl inited!');
                    if (serviceSvc.isReady()) {
                        $log.info('serviceSvc is already ready', serviceSvc);
                        go();
                    } else {
                        $log.info('wating for serviceSvc to become ready');
                        serviceSvc.waitToBecomeReady().then(function () {
                            $log.info('serviceSvc is now ready', serviceSvc);
                            go();
                        });
                    }
                }, function (msg_) {
                    console.log('mrl.init()-meh!');
                });
                
                var go = function () {
                    var previousstate = $previousState.get();
                    $log.info('transitioning to state ', previousstate.state.name, ' with params ', previousstate.params);
                    $state.go(previousstate.state.name, previousstate.params);
                };
            }]);
