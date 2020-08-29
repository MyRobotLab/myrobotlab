angular.module('mrlapp.nav')
        .controller('shutdownCtrl', ['$scope', '$uibModalInstance', 'type', 'mrl', function ($scope, $uibModalInstance, type, mrl) {
                $scope.close = $uibModalInstance.close;
                $scope.type = type;
        
                $scope.shutdown = function () {
                    switch (type) {
                        case 'shutdown':
                            console.info('attempt to SHUTDOWN');
                            mrl.sendTo(mrl.getRuntime().name, 'shutdown');
                            break;
                        case 'restart':
                            console.info('attempt to RESTART');
                            mrl.sendTo(mrl.getRuntime().name, 'restart');
                            break;
                        default:
                            break;
                    }
                };
            }]);