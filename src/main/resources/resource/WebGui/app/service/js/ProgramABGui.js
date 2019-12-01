angular.module('mrlapp.service.ProgramABGui', [])
.controller('ProgramABGuiCtrl', ['$scope', '$log', 'mrl', '$uibModal', '$sce', function($scope, $log, mrl, $uibModal, $sce) { // $modal ????
    $log.info('ProgramABGuiCtrl');
    // grab the self and message
    var _self = this;
    var startDialog = null;
    var msg = this.msg;
    
    // use $scope only when the variable
    // needs to interract with the display
    $scope.currentUserName =  '';
    $scope.currentBotName = '';
    $scope.utterance = '';

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
        $scope.currentUserName = service.currentUserName;
        $scope.currentBotName = service.currentBotName;
        $scope.service = service;
    }
    ;
    
    this.onMsg = function(inMsg) {
        $log.info("Program AB Msg !");
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0]);
            $scope.$apply();
            break;
        case 'onRequest':
            var textData = inMsg.data[0];
            $scope.rows.unshift({
                name: $scope.currentUserName,
                text: $sce.trustAsHtml(textData)
            });
            $log.info('currRequest', textData);
            $scope.$apply();
            break;
        case 'onText':
            var textData = inMsg.data[0];
            $scope.rows.unshift({
                name: $scope.currentBotName,
                text: $sce.trustAsHtml(textData)
            });
            $log.info('currResponse', textData);
            $scope.$apply();
            break;
        case 'onOOBText':
            var textData = inMsg.data[0];
            $scope.rows.unshift({
                name: " > oob <",
                text: $sce.trustAsHtml(textData)
            });
            $log.info('currResponse', textData);
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
    
    
    $scope.getSessionResponse = function(utterance) {
    	$log.info("SESSION GET RESPONSE (" + $scope.currentUserName + " " + $scope.currentBotName + ")");
    	$scope.getResponse($scope.currentUserName, $scope.currentBotName, utterance);
    }
    
    $scope.getResponse = function(username, botname, utterance) {
    	$log.info("USER BOT RESPONSE (" + username + " " + botname + ")");
        msg.send("getResponse", username, botname, utterance);
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
    msg.subscribe('publishRequest');
    msg.subscribe('publishText');
    msg.subscribe('publishOOBText');
    msg.subscribe(this);
}
]);