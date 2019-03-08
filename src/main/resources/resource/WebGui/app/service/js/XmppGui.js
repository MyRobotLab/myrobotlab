angular.module('mrlapp.service.XmppGui', [])
.controller('XmppGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('XmppGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    $scope.isConnectedImage = "disconnected";
    $scope.log = "";
    $scope.sendToContacts = {};
    $scope.sendInput = "";
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.isConnected = service.isConnected;
        $scope.hostname = service.hostname;
        $scope.port = service.port;
        $scope.username = service.username;
        $scope.password = service.password;
        $scope.contacts = service.contacts;
        $scope.isConnectedImage = (service.isConnected) ? "connected" : "disconnected";
        
        angular.forEach(service.contacts, function(value, key) {
            $scope.sendToContacts[key] = false;
        });
        
        // sendToContacts
    }
    ;
    
    _self.updateState($scope.service);
    
    // init scope variables
    $scope.pulseData = '';
    
    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0]);
            $scope.$apply();
            break;
        case 'onXmppMsg':
            var xmppMsg = inMsg.data[0];
            var chat = new Date().getTime() + ":" + xmppMsg.from + ":" + xmppMsg.msg + "\n";
            $scope.log += chat;
            $scope.$apply();
            break;
        case 'onSentXmppMsg':
            var xmppMsg = inMsg.data[0];
            var chat = new Date().getTime() + ":" + $scope.username + ":" + xmppMsg.msg + "\n";
            $scope.log += chat;
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
    }
    ;
    
    $scope.prepareSend = function() {
        angular.forEach($scope.sendToContacts, function(value, key) {
            if (value) {
                msg.send("sendMessage", key, $scope.sendInput);
            }
        });
        $scope.sendInput = "";
    }
    
    //mrl.subscribe($scope.service.name, 'pulse');
    msg.subscribe('publishXmppMsg');
    msg.subscribe('publishSentXmppMsg');
    msg.subscribe(this);
}
]);
