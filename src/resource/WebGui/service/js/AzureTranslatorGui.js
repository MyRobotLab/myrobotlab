angular.module('mrlapp.service.AzureTranslatorGui', [])
.controller('AzureTranslatorGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('AzureTranslatorGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    $scope.translatedText = '';
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.from = service.fromLanguage;
        $scope.to = service.toLanguage;
    }
    ;
    _self.updateState($scope.service);
    
    // init scope variables
    $scope.translatedText = '';
    
    this.onMsg = function(inMsg) {
        
    }
    ;
    
    //mrl.subscribe($scope.service.name, 'pulse');
    msg.subscribe(this);
}
]);
