angular.module('mrlapp.nav')
        .controller('noWorkyCtrl', ['$scope', '$uibModalInstance', 'reason', 'mrl', function ($scope, $uibModalInstance, reason, mrl) {
                $scope.close = $uibModalInstance.close;
        
                $scope.status = 'waitingforinformation';
                
                $scope.noWorky = function (userId) {
                    $scope.status = 'sendingnoworky';
                    mrl.noWorky(userId);
                };
                
                var onNoWorky = function (noWorkyResultssMsg) {
                    var status = noWorkyResultssMsg.data[0];
                    if (status.level == 'error') {
                        $scope.status = 'error';
                        $scope.statuskey = status.key;
                    } else {
                        $scope.status = 'success';
                    }
                };
                mrl.subscribeToServiceMethod(onNoWorky, mrl.getRuntime().name, 'publishNoWorky');
            }]);