angular.module('mrlapp.main.statusSvc', [])
        .service('statusSvc', [function () {

                var statuslist = [];

                this.addStatus = function (status) {
                    statuslist.push(status);
                };

                this.getStatuses = function () {
                    return statuslist;
                };

                this.clearStatuses = function () {
                    statuslist = [];
                };
                
                //maybe let this evolve into it's own service?
                var addAlertCallback;
                
                this.registerAddAlertCallback = function (cb) {
                    addAlertCallback = cb;
                };
                
                this.addAlert = function(type, msg) {
                    addAlertCallback(type, msg);
                };
                
                //TODO - closeAlert ?
            }]);
