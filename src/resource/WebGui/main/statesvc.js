/*
    mis-named statesvc should be named statussvs
    new file is put in
    TODO - unused - could be removed now
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
