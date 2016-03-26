angular.module('mrlapp.nav')
        .controller('shutdownCtrl', ['$scope', '$uibModalInstance', '$log', 'type', 'mrl', function ($scope, $uibModalInstance, $log, type, mrl) {
                $scope.close = $uibModalInstance.close;
                $scope.type = type;
        
                $scope.shutdown = function () {
                    switch (type) {
                        case 'shutdown':
                            $log.info('attempt to SHUTDOWN');
                            //TODO - important - send message to runtime!
                            break;
                        case 'restart':
                            $log.info('attempt to RESTART');
                            //TODO - important - send message to runtime!
                            break;
                        default:
                            break;
                    }
                };
            }]);