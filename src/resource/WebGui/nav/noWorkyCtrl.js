angular.module('mrlapp.nav')
        .controller('noWorkyCtrl', ['$scope', '$uibModalInstance', 'reason', 'mrl', 'statusSvc', function ($scope, $uibModalInstance, reason, mrl, statusSvc) {
                $scope.close = $uibModalInstance.close;
        
                $scope.status = 'waitingforinformation';
                
                $scope.noWorky = function (userId) {
                    $scope.status = 'sendingnoworky';
                    mrl.noWorky(userId);
                };
                
                var onNoWorky = function (noWorkyResultssMsg) {
                    var status = noWorkyResultssMsg.data[0];
//                    console.log('noWorkySvc-onNoWorky', status);
                    if (status.level == 'error') {
                        $scope.status = 'error';
                        $scope.statuskey = status.key;
                        statusSvc.addAlert('danger', 'the noWorky did not worky ! ' + status.key);
                    } else {
                        $scope.status = 'success';
                        statusSvc.addAlert('success', 'noWorky sent !');
                    }
                };
                mrl.subscribeToServiceMethod(onNoWorky, mrl.getRuntime().name, 'publishNoWorky');
                mrl.subscribe(mrl.getRuntime().name, 'publishNoWorky');
            }]);