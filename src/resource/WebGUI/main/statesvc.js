/*
    mis-named statesvc should be named statussvs
*/
angular.module('mrlapp.main.statesvc', [])
        .service('StateSvc', [function () {
        
        var statuslist = [];
        
        this.addStatus = function(status) {
            statuslist.push(status);
        };
        
                this.getStatuses = function () {
            return statuslist;
        };
        
                this.clearStatuses = function () {
            statuslist = [];
        };
    }]);
