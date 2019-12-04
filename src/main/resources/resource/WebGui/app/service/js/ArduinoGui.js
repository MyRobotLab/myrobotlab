angular.module('mrlapp.service.ArduinoGui', []).controller('ArduinoGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('ArduinoGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.editor = null
    $scope.version = "unknown"
    $scope.boardType = ""
    $scope.image = "Arduino/Uno.png"
    $scope.possibleBaud = ['600', '1200', '2400', '4800', '9600', '19200', '38400', '57600', '115200']
    $scope.versionStatus = ""
    $scope.rate = '115200'
    $scope.singleModel = 0
    $scope.isConnected = "disconnected"

    $scope.boardInfo = {
        "boardType": null,
        "deviceCount": null,
        "deviceList": null,
        "enableBoardInfo": false,
        "sram": null,
        "us": null,
        "version": null
    }

    // for port directive
    $scope.portDirectiveScope = {}

    $scope.toBoardType = function(boardName) {
        if (boardName.includes('mega')){
            return 'mega'
        }
        if (boardName.includes('uno')){
            return uno
        }
        return boardName
    }

    // Status - from the Arduino service
    $scope.statusLine = ""
    this.updateState = function(service) {
        $scope.service = service
        $scope.boardType = service.board // $scope.toBoardType(service.boardInfo.boardTypeName)
        $scope.arduinoPath = service.arduinoIdePath
        $scope.image = "Arduino/" + service.board + ".png"
        var serial = $scope.service.serial

        $scope.serialName = null

        // === service.serial begin ===
        if (service.mrlCommVersion != null) {
            $scope.versionStatus = " with firmware version " + service.mrlCommVersion
        } else {
            $scope.versionStatus = null
        }
    }

    $scope.base64ToArrayBuffer = function(string) {
        const binaryString = window.atob(string);
        // Comment this if not using base64
        const bytes = new Uint8Array(binaryString.length);
        return bytes.map((byte,i)=>binaryString.charCodeAt(i));
    }


    this.onMsg = function(inMsg) {
        // TODO - make "super call" as below
        // this.constructor.prototype.onMsg.call(this, inMsg)
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onStatus':
            // FIXME - onStatus needs to be handled in the Framework !!!
            // $scope.statusLine = data
            break
        case 'onPinArray':
            // a NOOP - but necessary 
            break
        case 'onPortNames':
            $scope.possiblePorts = data
            $scope.$apply()
            break
        case 'onBoardInfo':
            $scope.boardInfo = data
            $scope.$apply()
            break
        case 'onVersion':
            $scope.version = data
            if ($scope.version != service.mrlCommVersion) {
                $scope.version = "expected version or MRLComm.c is " + service.mrlCommVersion + " board returned " + $scope.version + " please upload version " + service.mrlCommVersion
            }
            $scope.$apply()
            break
        case 'onBase64ZippedMrlComm':

            let binaryString = $scope.base64ToArrayBuffer(data);
            var textFileAsBlob = new Blob([binaryString]);

            var downloadLink = document.createElement("a");
            downloadLink.download = 'MrlComm.zip';
            downloadLink.innerHTML = "Download File";
            if (window.webkitURL != null) {
                // Chrome allows the link to be clicked
                // without actually adding it to the DOM.
                downloadLink.href = window.webkitURL.createObjectURL(textFileAsBlob);
            } else {
                // Firefox requires the link to be added to the DOM
                // before it can be clicked.
                downloadLink.href = window.URL.createObjectURL(textFileAsBlob);
                downloadLink.onclick = destroyClickedElement;
                downloadLink.style.display = "none";
                document.body.appendChild(downloadLink);
            }

            downloadLink.click();
            break
        case 'onPortNames':
            $scope.possiblePorts = data
            $scope.$apply()
            break
            // FIXME - this should be in a prototype    
        case 'onConnect':
            $scope.isConnectedImage = "connected"
            $scope.isConnected = true
            $scope.$apply()
            break
        case 'onDisconnect':
            $scope.isConnectedImage = "disconnected"
            $scope.isConnected = false
            $scope.$apply()
            break
            // FIXME - this should be in a prototype    
        case 'onSerial':
            $scope.serial = data
            if ($scope.serial != null) {
                $scope.isConnected = ($scope.serial.portName != null)
                $scope.isConnectedImage = ($scope.serial.portName != null) ? "connected" : "disconnected"
                $scope.connectText = ($scope.serial.portName == null) ? "connect" : "disconnect"
                if ($scope.isConnected) {
                    $scope.portName = $scope.serial.portName
                } else {
                    $scope.portName = $scope.serial.lastPortName
                }
            }
            break
        case 'onPin':
            break
        case 'onTX':
            ++$scope.txCount
            $scope.tx += data
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    // $scope display methods
    $scope.onBoardChange = function(boardType) {
        if ($scope.service.boardType != boardType) {
            msg.send('setBoard', boardType)
        }
    }

    $scope.upload = function(arduinoPath, portDirectiveScope, type) {
        // FIXME !!! - nicer global check empty method
        // FIXME !!! - parent error warn info - publishes to the appropriate service
        if (angular.isUndefined(arduinoPath) || arduinoPath == null || arduinoPath == "") {
            msg.send('error', 'arduino path is not set')
            return
        }
        if (angular.isUndefined(portDirectiveScope) || portDirectiveScope.portName == null || portDirectiveScope.portName == "") {
            msg.send('error', 'port name not set')
            return
        }
        if (angular.isUndefined(type) || type == null || type == "") {
            msg.send('error', 'board type not set')
            return
        }
        msg.send('uploadSketch', arduinoPath, portDirectiveScope.portName, type)
    }

    $scope.aceLoaded = function(editor) {
        // FIXME - can't we get a handle to it earlier ?
        $scope.editor = editor
        // Options
        editor.setReadOnly(true)
        editor.$blockScrolling = Infinity
        editor.setValue($scope.service.sketch.data, -1)
    }

    $scope.aceChanged = function(e) {}

    $scope.oink = function(e) {
        $log.info('hello')
    }

    // get version
    msg.subscribe('publishVersion')
    msg.subscribe('publishBoardInfo')
    msg.subscribe('getPortNames')
    msg.subscribe('publishConnect')
    msg.subscribe('publishDisconnect')
    msg.subscribe('getPortNames')
    msg.subscribe('getSerial')
    msg.subscribe('publishPinArray')
    // msg.subscribe('getZippedMrlComm')
    msg.subscribe('getBase64ZippedMrlComm')

    msg.send('getPortNames')
    msg.send('getSerial')
    // FIXME - onSerial ...
    // msg.subscribe('publishSensorData')
    msg.subscribe(this)
}
])
