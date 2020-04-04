angular.module('mrlapp.service.IntroGui', []).controller('IntroGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function($scope, $log, mrl, $timeout) {
    $log.info('IntroGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.mrl = mrl
    $scope.mrl = mrl
    $scope.panel = mrl.getPanel('runtime')

    $scope.activePanel = 'settings' 

    // GOOD Intro TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }
    
    $scope.setPanel = function(panelName) {
        $scope.activePanel = panelName

        // unselect active buttons by removing active class
        var container = document.querySelector("#containerWebGui");
        if (container!=null) {
            var matchesItems = container.querySelectorAll(".dotWebGuiActive");
            for (var i = 0; i < matchesItems.length; i++) { matchesItems[i].classList.remove('dotWebGuiActive'); }

            var matchesItems = container.querySelectorAll(".dotWebGuiButtonsActive");
            for (var i = 0; i < matchesItems.length; i++) { matchesItems[i].classList.remove('dotWebGuiButtonsActive'); matchesItems[i].classList.add('dotTorsoButtons'); }
        }     

        // add activ class to dot ans button object
        if (document.querySelector("#"+panelName+"Dot")!=null) {
            document.querySelector("#"+panelName+"Dot").classList.add('dotWebGuiActive');
            document.querySelector("#"+panelName+"Button").classList.add('dotWebGuiButtonsActive');
        }   

    }

    $scope.showPanel = function(panelName) {
        return $scope.activePanel == panelName
    }   
  
    $scope.setPanel('extension')
    
    msg.subscribe(this)
}
])
