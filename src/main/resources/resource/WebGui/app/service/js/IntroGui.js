angular.module('mrlapp.service.IntroGui', []).controller('IntroGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function($scope, $log, mrl, $timeout) {
    $log.info('IntroGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.mrl = mrl
    $scope.panel = mrl.getPanel('runtime')

    $scope.activePanel = 'settings' 

    // GOOD Intro TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }
    
    $scope.setPanel = function(panelName) {
        $scope.activePanel = panelName
    }

    $scope.showPanel = function(panelName) {
        return $scope.activePanel == panelName
    }   
  
    $scope.setPanel('extension')
    
    msg.subscribe(this)
}
])
