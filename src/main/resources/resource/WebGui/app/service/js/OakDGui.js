angular.module('mrlapp.service.OakDGui', []).controller('OakDGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('OakDGuiCtrl')
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }
    msg.subscribe('publishTime')
    msg.subscribe('publishEpoch')
    msg.subscribe(this)
}
])    