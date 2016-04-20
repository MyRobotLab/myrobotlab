angular.module('mrlapp.service.ArduinoGui', [])
.controller('ArduinoGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('ArduinoGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    $scope.editor = null ;
    
    $scope.statusLine = "";
    $scope.version = "unknown";
    $scope.board = "";
    $scope.image = "service/arduino/Uno.png";
    
    this.updateState = function(service) {
        $scope.service = service;
        $scope.board = service.board;
        $scope.image = "service/arduino/" + service.board + ".png";
        
        // === service.serial begin ===
        $scope.serialName = service.serial.name;
        $scope.isConnected = ($scope.service.serial.portName != null );
        $scope.isConnectedImage = ($scope.service.serial.portName != null ) ? "connected" : "disconnected";
        $scope.connectText = ($scope.service.serial.portName == null ) ? "connect" : "disconnect";
        if ($scope.isConnected) {
            $scope.portName = $scope.service.serial.portName;
        } else {
            $scope.portName = $scope.service.serial.lastPortName;
        }
        // === service.serial begin ===
        
        $scope.statusLine = $scope.board;
        if ($scope.isConnected) {
            $scope.statusLine += ' connected to ' + $scope.portName + ' version ' + $scope.version;
        } else {
            $scope.statusLine += ' disconnected'
        }
        
        //$scope.version = service.mrlCommVersion;
    }
    ;
    _self.updateState($scope.service);
    
    this.onMsg = function(inMsg) {
        // TODO - make "super call" as below
        // this.constructor.prototype.onMsg.call(this, inMsg);
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0]);
            $scope.$apply();
        case 'onPortNames':
            $scope.possiblePorts = inMsg.data[0];
            $scope.$apply();
            break;
        case 'onVersion':
            $scope.version = inMsg.data[0];
            if ($scope.version != service.mrlCommVersion) {
                $scope.version = "expected version or MRLComm.c is " + service.mrlCommVersion + " board returned " + $scope.version + " please upload version " + service.mrlCommVersion;
            }
            $scope.$apply();
            break;
        case 'onRefresh':
            $scope.possiblePorts = inMsg.data[0];
            $scope.$apply();
            break;
            // FIXME - this should be in a prototype    
        case 'onStatus':
            // backend update 
            // FIXME - SHOULD BE MODIFYING PARENT'S STATUS
            // $scope.updateState(inMsg.data[0]);
            // $scope.$apply();
            break;
        case 'onPin':
            
            break;
        case 'onTX':
            ++$scope.txCount;
            $scope.tx += inMsg.data[0];
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
    }
    ;
    
    // $scope display methods
    $scope.onBoardChange = function(board) {
        if ($scope.service.board != board) {
            msg.send('setBoard', board);
        }
    }
    ;
    
    $scope.aceLoaded = function(editor) {
        // FIXME - can't we get a handle to it earlier ?
        $scope.editor = editor;
        // Options
        editor.setReadOnly(true);
        editor.$blockScrolling = Infinity;
        editor.setValue($scope.service.sketch.data, -1);
    }
    ;
    
    $scope.aceChanged = function(e) {
    }
    ;
    
 
    // get version
    msg.subscribe('publishVersion');
    msg.send("getVersion");
    msg.subscribe(this);
}
]);
