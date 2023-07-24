angular.module('mrlapp.service.CronGui', []).controller('CronGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('CronGuiCtrl')
    var _self = this
    var msg = this.msg

    // str verson of parameters from the input 
    // text form field
    $scope.parameters = null

    $scope.newTask = {
        id: null,
        cronPattern: null,
        name: null,
        method: null,
        data: null
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.addNamedTask = function() {
        if ($scope.parameters && $scope.parameters.length > 0) {
            $scope.newTask.data = JSON.parse($scope.parameters)
        } else {
            $scope.newTask.data = null
        }

        msg.send('addTask', $scope.newTask)
    }

    $scope.removeTask = function(id) {
        msg.send('removeTask', id)
    }

    msg.subscribe(this)
}
]).filter('epochToLocalDate', function() {
    return function(epochTime) {
        return new Date(epochTime).toLocaleString();
    }
    ;
});
