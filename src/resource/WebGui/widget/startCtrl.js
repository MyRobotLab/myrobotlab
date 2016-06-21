angular.module('mrlapp.widget.startCtrl', ['mrlapp.mrl'])
    .controller('startCtrl', ['$scope', '$log', function($scope, $log) {
        
    $scope.items = ['item1', 'item2', 'item3'];    
    //$scope.items = items;
    $scope.selected = {
        item: $scope.items[0]
    };
    
    $scope.ok = function() {
        $uibModalInstance.close($scope.selected.item);
    }
    ;
    
    $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
    }
    ;

}]);
