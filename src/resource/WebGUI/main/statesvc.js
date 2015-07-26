angular.module('mrlapp.main.statesvc', [])
        .service('StateSvc', [function () {

                var statuslist = [];

                this.addStatus = function (name, level, detail) {
                    statuslist.push({
                        name: name,
                        level: level,
                        detail: detail
                    });
                };

                this.getStatuses = function () {
                    return statuslist;
                };

                this.clearStatuses = function () {
                    statuslist = [];
                };
            }]);