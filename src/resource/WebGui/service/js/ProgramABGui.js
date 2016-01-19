angular.module('mrlapp.service.ProgramABGui', [])
.controller('ProgramABGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('ProgramABGuiCtrl');
    // grab the self and message
    var _self = this;
    var msg = this.msg;
    
    // use $scope only when the variable
    // needs to interract with the display
    $scope.currResponse = '';
    $scope.utterance = '';
    $scope.username = 'default';
    $scope.current_text = '';
    // start info status
    $scope.rows = [];
    
    // following the template.
    this.updateState = function (service) {
        $scope.service  = service;
        $scope.botname  = service.botName;
        $scope.username = service.currentUser;
    };
    
    _self.updateState($scope.service);
    
    this.onMsg = function(inMsg) {
        $log.info("Program AB Msg !");
        switch (inMsg.method) {
          case 'onState':
            _self.updateState(inMsg.data[0]);
            $scope.$apply();
            break;
          case 'onText':
            var textData = inMsg.data[0];
            $scope.currResponse = textData;
            $scope.rows.unshift({
                name: "Bot:",
                response: textData
            });
            $log.info('currResponse', $scope.currResponse);
            $scope.$apply();
            break;
          default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        };
    };
    
    $scope.askProgramAB = function(username, utterance) {
        msg.send("getResponse", username, utterance);
        $scope.rows.unshift({
            name: "User",
            response: utterance
        });

        $scope.utterance = "";
    };
    
    $scope.startSession = function(botpath, username, botname) {
        $scope.rows.unshift("Reload Session for Bot " + botname);
        $scope.startSessionLabel = 'Reload Session';
        $log.info("BOT PATH" + botpath);
        msg.send("startSession", botpath, username, botname);
        // $scope.$apply();
    };
    
    $scope.savePredicates = function() {
        $scope.service = mrl.getService($scope.service.name);
        mrl.sendTo($scope.service.name, "savePredicates");
    };
        
    // subscribe to the response from programab.
    msg.subscribe('publishText');
    msg.subscribe(this);
    // we're done.

}
]);

