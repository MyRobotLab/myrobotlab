angular.module('mrlapp.service.ArduinoGui', [])
.controller('ArduinoGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('ArduinoGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    $scope.editor = null ;
    
    $scope.version = "unknown";
    $scope.board = "";
    $scope.image = "service/arduino/Uno.png";

    $scope.connectedStatus = "";
    $scope.versionStatus = "";
    // Status - from the Arduino service
    $scope.statusLine = "";
    
    this.updateState = function(service) {
        $scope.service = service;
        $scope.board = service.board;
        $scope.arduinoPath = service.arduinoIdePath;
        $scope.image = "service/arduino/" + service.board + ".png";
        
        var serial = $scope.service.serial;

        // === service.serial begin ===
        $scope.serialName = service.serial.name;
        $scope.isConnected = (serial.portName != null );
        $scope.isConnectedImage = (serial.portName != null ) ? "connected" : "disconnected";
        
        if ($scope.isConnected) {
            $scope.portName = serial.portName;
            $scope.connectedStatus = "connected to " + serial.portName + " at " + serial.baudrate;
        } else {
            $scope.portName = serial.lastPortName;
            $scope.connectedStatus = "disconnected";
        }
        // === service.serial begin ===

        if ($scope.isConnected) {
            msg.send("getVersion");            
        } 
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
        case 'onStatus':
            $scope.statusLine = inMsg.data[0];
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
        case 'onGetVersion':{
            $scope.versionStatus = 'version ' + $scope.version;
            break;
        }
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
    
	$scope.setArduinoPath = function(arduinoPath,port,type){
		msg.send('uploadSketch',arduinoPath,port,type);
	};

    
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
    msg.subscribe(this);
}
]);
