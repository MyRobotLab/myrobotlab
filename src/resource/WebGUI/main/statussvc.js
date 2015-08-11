angular.module('mrlapp.main.statussvc', [])
        .service('StatusSvc', [function () {

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
            }]);
