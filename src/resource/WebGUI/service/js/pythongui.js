angular.module('mrlapp.service.pythongui', [])
.controller('PythonGuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('PythonGuiCtrl');
    
    // get fresh copy
    // basic data set
    var name = $scope.service.name;
    $scope.service = mrl.getService($scope.service.name);
    $scope.name = name;
    
    $scope.output = '';
    
    // the awesome ace editor 1
    var editor = null ;
    
    // FIXME needs a prototype to update the mrl service    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        // TODO make something like "files"
    
    }
    ;
    
    
    //you can access two objects
    //$scope.panel & $scope.service
    //$scope.panel contains some framwork functions related to your service panel
    //-> you can call functions on it, but NEVER write in it
    //$scope.service is your service-object, it is the representation of the service running in mrl
    
    //you HAVE TO define this method &
    //it is the ONLY exception of writing into .gui
    //-> you will receive all messages routed to your service here
    $scope.panel.onMsg = function(msg) {
        switch (msg.method) {
        case 'onState':
            _self.updateState(msg.data[0]);
            $scope.$apply();
            break;        
        case 'onStdOut':
            $scope.output = msg.data[0] + $scope.output;
            // $scope.output = $scope.output + msg.data[0];
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + msg.method);
            break;
        }
    }
    ;
    
    $scope.aceLoaded = function(e) {
        $log.info("ace loaded");
        // Options
        editor = e;
        //editor.setReadOnly(true);
    }
    ;
    
    $scope.aceChanged = function(e) {
        $log.info("ace changed");
        //
    }
    ;
    
    $scope.execute = function() {
        $log.info("execute");
        mrl.sendTo(name, "exec", editor.getValue());
    }
    ;
    
    //you can subscribe to methods
    mrl.subscribe(name, 'publishStdOut');
    mrl.subscribe(name, 'clockStarted');
    mrl.subscribe(name, 'clockStopped');
    
    // FIXME re-entrant?
    mrl.sendTo(name, "attachPythonConsole");
    
    mrl.sendTo(name, "broadcastState");
    
    //after you're done with setting up your service-panel, call this method
    $scope.panel.initDone();
}
]);
