angular.module('mrlapp.service.ShoutboxGui', [])
.controller('ShoutboxGuiCtrl', ['$scope', '$log', 'mrl', '$sce', function($scope, $log, mrl, $sce) {
    $log.info('ShoutboxGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    var name = $scope.name;
    // var msg = mrl.createMsgInterface(name, $scope);
    
    // TODO - Directive for connected
    $scope.shoutMsg = '';
    $scope.nickname = '';
    $scope.placeholder = 'Choose Name';
    $scope.shouts = [];
    $sce.trustAsHtml($scope.shoutMsg);
    
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        // FIXME let the framework 
        mrl.updateState(service);
    
    }
    ;
    
    _self.updateState($scope.service);
    
    this.onMsg = function(inMsg) {
        
        switch (inMsg.method) {
        case 'onState':
            var service = inMsg.data[0];
            _self.updateState(service);
            $scope.shouts = service.shouts;
            $scope.$apply();
            break;
        case 'onShout':
            var shout = inMsg.data[0];
            $scope.shouts.push(shout);
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + "." + inMsg.method);
            break;
        }
    }
    ;
    
    $scope.getHtml = function(shout) {
        return shout.from + ':' + shout.msg;
    }
    
    $scope.trustAsHtml = $sce.trustAsHtml;
    
    // client shouting ...
    $scope.shout = function() {
        if ($scope.shoutMsg == '') {
            $log.info("empty shout");
            return;
        }
        
        if ($scope.nickname == '') {
            $scope.nickname = $scope.shoutMsg;
            $scope.placeholder = 'Message';
            $scope.shoutMsg = '';
            return;
        }
        
        msg.send("shout", $scope.nickname, $scope.shoutMsg);
        $scope.shoutMsg = '';
    }
    
    
    $scope.toggle = function(label, interval) {
        if (label == "Start") {
            mrl.sendTo($scope.service.name, "setInterval", interval);
            mrl.sendTo($scope.service.name, "startShoutbox");
        } else {
            mrl.sendTo($scope.service.name, "stopShoutbox");
        }
    }
    ;
    
    msg.subscribe('publishShout')
    //mrl.subscribe($scope.service.name, 'pulse');
    msg.subscribe(this);

}
]);
