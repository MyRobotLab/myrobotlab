/*
    mis-named statesvc should be named statussvs
*/
angular.module('mrlapp.main.statesvc', ['mrlapp.mrl'])
.service('StateSvc', [function() {
    
    var statuslist = [];
    
    this.addStatus = function(status) {
        statuslist.push(status.level + " " + status.name + " " + status.key + " " + status.detail);
    }
    ;
    
    this.getStatuses = function() {
        return statuslist;
    }
    ;
    
    this.clearStatuses = function() {
        statuslist = [];
    }
    ;
}
]);
