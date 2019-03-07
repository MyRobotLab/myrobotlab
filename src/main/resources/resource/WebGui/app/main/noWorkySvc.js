angular.module('mrlapp.main.noWorkySvc', [])
        .service('noWorkySvc', ['$uibModal', 'mrl', 'statusSvc', function ($uibModal, mrl, statusSvc) {
                //own service might be overheat,
                //but it is used in more than one place
                //e.g. navbar & in every service UI
                
                this.openNoWorkyModal = function (reason) {
                    //reason is maybe for later
                    //more advanced NoWorky's? reason = service send from?
                    var modalInstance = $uibModal.open({
                        animation: true,
                        templateUrl: 'nav/noWorky.html',
                        controller: 'noWorkyCtrl',
//                        size: 'sm',
                        resolve: {
                            reason: function () {
                                return reason;
                            }
                        }
                    });
                };
                
                var onNoWorky = function (noWorkyResultssMsg) {
                    var status = noWorkyResultssMsg.data[0];
                    if (status.level == 'error') {
                        statusSvc.addAlert('danger', 'the noWorky did not worky ! ' + status.key);
                    } else {
                        statusSvc.addAlert('success', 'noWorky sent !');
                    }
                };
                mrl.subscribeToServiceMethod(onNoWorky, mrl.getRuntime().name, 'publishNoWorky');
                mrl.subscribe(mrl.getRuntime().name, 'publishNoWorky');
            }]);
