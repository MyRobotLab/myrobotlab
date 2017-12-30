angular.module('mrlapp.service.PythonGui', []).controller('PythonGuiCtrl', ['$log', '$scope', 'mrl', '$timeout', function($log, $scope, mrl, $timeout) {
    $log.info('PythonGuiCtrl');
    _self = this;
    var msg = this.msg;
    var name = $scope.name;
    // init scope values
    $scope.service = mrl.getService(name);
    $scope.output = '';
    $scope.activeTabIndex = 0;
    $scope.activeScript = null;
    $scope.scripts = {};//$scope.service.openedScripts; FIX -> call updateState($scope.service);

    // IF update of 'currentScript' not equal to webgui version - create new tab ?
    this.updateState = function(service) {
        $scope.service = service;
        angular.forEach(service.openedScripts ,function(value, key){
                if(!angular.isDefined($scope.scripts[key])){
                    $scope.scripts[key] = value;
                }

                /*
                if(!angular.isDefined($scope.scripts[key].editor)){

                }
                */
            })
    }
    ;
    this.onMsg = function(msg) {
        switch (msg.method) {
            // FIXME - bury it ?
        case 'onState':
            // its important to externalize the updating
            // of the service body in a method rather than doing the 
            // updates inline here - because when things are first initialized
            // we want to call the same method - and if it was inline that
            // would make a mess
            _self.updateState(msg.data[0]);
            $scope.$apply();
            break;
        case 'onStdOut':
            $scope.output = $scope.output + msg.data[0];
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + msg.method);
            break;
        }
    }
    ;
    // utility methods //
    // gets script name from full path name
    $scope.getName = function(path) {
        if (path.indexOf("/") >= 0) {
            return ( path.split("/").pop()) ;
        }
        if (path.indexOf("\\") >= 0) {
            return ( path.split("\\").pop()) ;
        }
        return path;
    }
    
    //----- ace editors related callbacks begin -----//
    $scope.aceLoaded = function(e) {
        $log.info("ace loaded");
    }
    ;
    $scope.aceChanged = function(e) {
        $log.info("ace changed");
    }
    //----- ace editors related callbacks end -----//
    $scope.addScript = function() {
        var newScript = {
            name: 'Script ' + ($scope.scripts.length + 1),
            code: ''
        };
        $scope.scripts.push(newScript);
        $timeout(function() {// $scope.activeTabIndex = ($scope.scripts.length - 1);
        });
        console.log($scope.activeTabIndex);
    }
    ;
    $scope.exec = function() {
        // $log.info('here');
        // $scope.activeTabIndex;
        // $log.info($scope.scripts[$scope.activeTabIndex]);
        msg.send('exec', $scope.activeScript.code);
    }
    $scope.tabSelected = function(script) {
        $log.info('here');
        $scope.activeScript = script;
        // need to get a handle on hte tab's ui / text
        // $scope.editors.setValue(script.code);
    }
  
    msg.subscribe('publishStdOut');
    msg.send("attachPythonConsole");
    msg.subscribe(this);
}
]);
