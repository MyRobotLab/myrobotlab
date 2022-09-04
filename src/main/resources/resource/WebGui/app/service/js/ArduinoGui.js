angular.module('mrlapp.service.ArduinoGui', []).controller('ArduinoGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('ArduinoGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.editor = null
    $scope.version = "unknown"
    $scope.boardType = ""
    $scope.image = "Arduino/Uno.png"
    $scope.possibleBaud = ['600', '1200', '2400', '4800', '9600', '19200', '38400', '57600', '115200']
    $scope.versionStatus = ""
    $scope.rate = '115200'
    $scope.isConnected = false

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
        if (boardName.includes('mega')) {
            return 'mega'
        }
        if (boardName.includes('uno')) {
            return uno
        }
        return boardName
    }

    this.updateState = function(service) {
        $scope.service = service
        $scope.boardType = service.board
        $scope.image = "Arduino/" + service.board + ".png"

        // === service.serial begin ===
        if (service.mrlCommVersion != null) {
            $scope.versionStatus = " with firmware version " + service.mrlCommVersion
        } else {
            $scope.versionStatus = null
        }

        for (const [pin,pinDef] of Object.entries(service.addressIndex)) {
            pinDef.readWrite = (pinDef.mode == 'OUTPUT') ? true : false
            pinDef.valueDisplay = (pinDef.value == 0) ? false : true
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

            // it may seem silly that there are 3 callbacks related
            // to determining the connected state of the Arduino
            // onConnect and onDisconnect are events owned by the SerialDevice
            // and propegated through the Arduino - since they are only events
            // its required to ask the Arduino onLoad if its in a connected state
            // hence the onIsConnected callback
        case 'onIsConnected':
            $scope.isConnected = data
            if (data) {
                $scope.isConnectedImage = "connected"
            } else {
                $scope.isConnectedImage = "disconnected"
            }
            $scope.$apply()
            break

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
        case 'onPin':
            break
        case 'onTX':
            ++$scope.txCount
            $scope.tx += data
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    // $scope display methods
    $scope.onBoardChange = function(boardType) {
        if ($scope.service.boardType != boardType) {
            msg.send('setBoard', boardType)
        }
    }

    $scope.readWrite = function(pinDef) {
        console.info(pinDef)
        msg.send('pinMode', pinDef.pin, pinDef.readWrite ? 'OUTPUT' : 'INPUT')
    }

    $scope.write = function(pinDef) {
        console.info(pinDef)
        msg.send('digitalWrite', pinDef.pin, pinDef.valueDisplay ? 1 : 0)
    }

    $scope.pwm = function(pinDef) {
        console.info(pinDef)
        msg.send('analogWrite', pinDef.pin, pinDef.value)
    }

    $scope.inputMode = function(pinDef) {
        console.info(pinDef)
        msg.send('pinMode', pinDef.pin, pinDef.inputModeDisplay ? 'PULLUP' : 'INPUT')
    }

    // get version
    msg.subscribe('publishVersion')
    msg.subscribe('publishBoardInfo')
    msg.subscribe('getPortNames')
    msg.subscribe('publishConnect')
    msg.subscribe('publishDisconnect')
    msg.subscribe('getPortNames')
    msg.subscribe('isConnected')
    msg.subscribe('publishPinArray')
    msg.subscribe('getBase64ZippedMrlComm')

    msg.send('getPortNames')
    msg.send('isConnected')
    msg.subscribe(this)
}
])
