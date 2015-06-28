angular.module('mrlapp.service.programabgui', [])
        .controller('ProgramABGuiCtrl', ['$scope', 'mrl', function ($scope, mrl) {
    console.log('ProgramABGuiCtrl');
    $scope.currResponse = '';
    $scope.utterance = '';
    $scope.rows = [];
    
    $scope.service = mrl.getService($scope.service.name);
    $scope.service.onMsg = function (msg) {
        console.log("Program AB Msg !");
        if (msg.method == "onText") {
            var textData = msg.data[0];
            //$scope.serviceDirectory[msg.sender].pulseData = pulseData;
            $scope.currResponse = textData;
            $scope.rows.unshift("Bot : " + textData);
            console.log('currResponse', $scope.currResponse);
            $scope.$apply();
        };
    };
    $scope.askProgramAB = function (utterance) {
    	$scope.service = mrl.getService($scope.service.name);
    	mrl.sendTo($scope.service.name, "getResponse", utterance);
    	$scope.rows.unshift("User : " + utterance);
    	$scope.utterance = '';
    };
    mrl.subscribe($scope.service.name, 'publishText'); 
    $scope.gui.initDone(); 
    
  }]);