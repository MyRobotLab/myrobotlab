angular.module('mrlapp.service.ArduinoGui', []).controller('ArduinoGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('ArduinoGuiCtrl');
    var _self = this;
    var msg = this.msg;
    $scope.editor = null ;
    $scope.version = "unknown";
    $scope.boardType = "";
    $scope.image = "service/arduino/Uno.png";
    $scope.connectedStatus = "";
    $scope.versionStatus = "";
    // $scope.boardInfo = 0;
    $scope.singleModel = 0; 

    $scope.boardInfo = {
        "boardType": null,
        "deviceCount": null ,
        "deviceList": null ,
        "enableBoardInfo":false,
        "sram": null ,
        "us": null ,
        "version": null
    }   

    // for port directive
    $scope.portDirectiveScope = {};
    
    // Status - from the Arduino service
    $scope.statusLine = "";
    this.updateState = function(service) {
        $scope.service = service;
        $scope.boardType = service.boardType;
        $scope.arduinoPath = service.arduinoIdePath;
        $scope.image = "service/arduino/" + service.boardType + ".png";
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
        if (service.mrlCommVersion != null ) {
            $scope.versionStatus = " with firmware version " + service.mrlCommVersion;
        } else {
            $scope.versionStatus = null ;
        }
        // infinite loop
        /*
        if ($scope.isConnected) {
            msg.send("getBoardInfo");
        }
        */
    }
    ;
    _self.updateState($scope.service);
    this.onMsg = function(inMsg) {
        // TODO - make "super call" as below
        // this.constructor.prototype.onMsg.call(this, inMsg);
        var data = inMsg.data[0];
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data);
            $scope.$apply();
            break;
        case 'onStatus':
            // FIXME - onStatus needs to be handled in the Framework !!!
            // $scope.statusLine = data;
            break;
        case 'onPinArray':
         // a NOOP - but necessary 
        break;
        case 'onPortNames':
            $scope.possiblePorts = data;
            $scope.$apply();
            break;
        case 'onBoardInfo':
            $scope.boardInfo = data;
            $scope.$apply();
            break;
        case 'onVersion':
            $scope.version = data;
            if ($scope.version != service.mrlCommVersion) {
                $scope.version = "expected version or MRLComm.c is " + service.mrlCommVersion + " board returned " + $scope.version + " please upload version " + service.mrlCommVersion;
            }
            $scope.$apply();
            break;
        case 'onRefresh':
            $scope.possiblePorts = data;
            $scope.$apply();
            break;
            // FIXME - this should be in a prototype    
        case 'onStatus':
            // backend update 
            // FIXME - SHOULD BE MODIFYING PARENT'S STATUS
            // $scope.updateState(data);
            // $scope.$apply();
            break;        
        case 'onPin':
            break;
        case 'onTX':
            ++$scope.txCount;
            $scope.tx += data;
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
    }
    ;
    // $scope display methods
    $scope.onBoardChange = function(boardType) {
        if ($scope.service.boardType != boardType) {
            msg.send('setBoard', boardType);
        }
    }
    ;
    $scope.upload = function(arduinoPath, portDirectiveScope, type) {
        // FIXME !!! - nicer global check empty method
        // FIXME !!! - parent error warn info - publishes to the appropriate service
        if (angular.isUndefined(arduinoPath) || arduinoPath == null || arduinoPath == "" ){
            msg.send('error', 'arduino path is not set');
            return;
        }
        if (angular.isUndefined(portDirectiveScope) || portDirectiveScope.portName == null || portDirectiveScope.portName == "" ){
            msg.send('error', 'port name not set');
            return;
        }
        if (angular.isUndefined(type) || type == null || type == "" ){
            msg.send('error', 'board type not set');
            return;
        }
        msg.send('uploadSketch', arduinoPath, portDirectiveScope.portName, type);
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
    $scope.aceChanged = function(e) {}
    ;
    $scope.oink = function(e) {
        $log.info('hello');
    }
    ;
    // get version
    msg.subscribe('publishVersion');
    msg.subscribe('publishBoardInfo');
    msg.subscribe('publishBoardInfo');
   // msg.subscribe('publishSensorData');
    msg.subscribe(this);
}
]);
