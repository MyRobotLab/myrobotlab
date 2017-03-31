angular.module('mrlapp.main.statusSvc', [])
        .service('statusSvc', ['mrl', function (mrl) {
                var _self = this;

                //START_Status_Notification
                var updateSubscribtions = [];
                _self.subscribeToUpdates = function (callback) {
                    updateSubscribtions.push(callback);
                };
                _self.unsubscribeFromUpdates = function (callback) {
                    var index = updateSubscribtions.indexOf(callback);
                    if (index != -1) {
                        updateSubscribtions.splice(index, 1);
                    }
                };
                var notifyAllOfUpdate = function (status) {
                    angular.forEach(updateSubscribtions, function (value, key) {
                        value(status);
                    });
                };
                //END_Status_Notification

                //START_Status
                var statusList = [];

                this.addStatus = function (status) {
                    statusList.push(status);
                };

                this.getStatuses = function () {
                    return statusList;
                };

                this.clearStatuses = function () {
                    statusList = [];
                };

                var onStatus = function (statusMsg) {
                    _self.addStatus(statusMsg.data[0]);
                    notifyAllOfUpdate(statusMsg.data[0]);
                };
                
                mrl.subscribeToMethod(onStatus, "onStatus");
                //END_Status

   
            }]);
