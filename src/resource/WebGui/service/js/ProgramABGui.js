angular.module('mrlapp.service.ProgramABGui', [])
.controller('ProgramABGuiCtrl', ['$scope', '$log', 'mrl', '$uibModal', '$sce', function($scope, $log, mrl, $uibModal, $sce) { // $modal ????
    $log.info('ProgramABGuiCtrl');
    // grab the self and message
    var _self = this;
    var startDialog = null;
    var msg = this.msg;
    
    // use $scope only when the variable
    // needs to interract with the display
    $scope.currResponse = '';
    $scope.utterance = '';
    $scope.currentText = '';

    $scope.currentSession = '';
    // TODO: which should we use?
    $scope.currentUserName =  '';
    $scope.currentBotName = '';

    // grab defaults.
    $scope.newUserName = $scope.service.currentUserName;
    $scope.newBotName = $scope.service.currentBotName;

    // start info status
    $scope.rows = [];
    
    // following the template.
    this.updateState = function(service) {
        // use another scope var to transfer/merge selection
        // from user - service.currentSession is always read-only
        // all service data should never be written to, only read from
        $scope.currentSession = service.currentUserName + "-" + service.currentBotName;
        $scope.service = service;
    }
    ;
    
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
                response: $sce.trustAsHtml(textData)
            });
            $log.info('currResponse', $scope.currResponse);
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
        ;
    }
    ;

    $scope.test = function(session, utterance) {
        msg.send("getCategories","hello");
    }
    
    
    $scope.getSessionResponse = function(session, utterance) {
    	$log.info("SESSION GET RESPONSE" + session);
    	$scope.getResponse(session.split("-")[0], session.split("-")[1], utterance);
    }
    
    $scope.getResponse = function(username, botname, utterance) {
    	$log.info("USER BOT RESPNSE" + username + " - " + botname);
        msg.send("getResponse", username, botname, utterance);
        $scope.rows.unshift({
            name: "User",
            response: $sce.trustAsHtml(utterance)
        });        
        $scope.utterance = "";
    };
    
    $scope.startDialog = function() {
        startDialog = $uibModal.open({
            templateUrl: "startDialog.html",
            scope: $scope,
            controller: function($scope) {
                $scope.cancel = function() {
                    startDialog.dismiss();
                }
                ;
            
            }
        });
    }
    
    $scope.startSession = function(username, botname) {
        $scope.currentUserName = username;
        $scope.currentBotName = botname;
    	$scope.rows.unshift("Reload Session for Bot " + botname);
        $scope.startSessionLabel = 'Reload Session';
        msg.send("startSession", username, botname);
        startDialog.dismiss();
    };
    
    $scope.savePredicates = function() {
        $scope.service = mrl.getService($scope.service.name);
        mrl.sendTo($scope.service.name, "savePredicates");
    };
    
    // subscribe to the response from programab.
    msg.subscribe('publishText');
    msg.subscribe(this);
}
]);


