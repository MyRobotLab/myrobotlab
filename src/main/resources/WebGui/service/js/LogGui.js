angular.module('mrlapp.service.LogGui', [])
.controller('LogGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('LogGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    var name = $scope.name;
//    var msg = mrl.createMsgInterface(name, $scope);
    // TODO singleton -  clear log / log level / appenderes / format
    
    // init scope variables
    $scope.log = '';
    $scope.logButton = '';
    $scope.level = '';
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        // FIXME let the framework 
        mrl.updateState(service);
        
        // set logging state
        if (service.isLogging == true) {
            $scope.logButton = "Stop Logging";
        } else {
            $scope.logButton = "Start Logging";
        }
        
        // set level
        $scope.logLevel = service.logLevel;
    
    }
    ;
    
    _self.updateState($scope.service);
    
    this.onMsg = function(msg) {
        
        switch (msg.method) {
        case 'onState':
            _self.updateState(msg.data[0]);
            $scope.$apply();
            break;
        case 'onLogEvent':
            $scope.log += '\n' + msg.data[0];
            // TODO: test this.
            var maxLength = 50000;
            var length = $scope.log.length;
            if (length > maxLength) {
            	// avoid if newline is the first char.. 
            	var nextLine = $scope.log.indexOf("\n",1);
            	if (nextLine != -1) {
            		$scope.log = $scope.log.substring(nextLine, length);
            	} 
            }
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + "." + msg.method);
            break;
        }
    }
    ;

    $scope.clear = function() {
        $scope.log = '';
        $scope.apply();
    }
    
    $scope.toggle = function(label, interval) {
        if (label == "Start") {
            mrl.sendTo($scope.service.name, "setInterval", interval);
            mrl.sendTo($scope.service.name, "startLog");
        } else {
            mrl.sendTo($scope.service.name, "stopLog");
        }
    }
    ;
    
    msg.subscribe('publishLogEvent')
    //mrl.subscribe($scope.service.name, 'pulse');
    msg.subscribe(this);
}
]);
