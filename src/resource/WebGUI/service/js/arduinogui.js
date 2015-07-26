angular.module('mrlapp.service.arduinogui', [])
.controller('ArduinoGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
        $log.info('ArduinoGuiCtrl');
        _self = this;
        
        $scope.gui.onMsg = function(msg) {
            $log.info('CALLBACK - ' + msg.method);
            switch (msg.method) {
                case 'onPortNames':
                    $scope.possiblePorts = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onRefresh':
                    $scope.possiblePorts = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onState':
                    // backend update 
                    $scope.updateState(msg.data[0]);
                    $scope.$apply();
                    break;
                case 'onPin':
                    
                    break;
                case 'onTX':
                    ++$scope.txCount;
                    $scope.tx += msg.data[0];
                    $scope.$apply();
                    break;
                default:
                    $log.error("ERROR - unhandled method " + msg.method);
                    break;
            }        
        }
        
        $scope.service = mrl.getService($scope.service.name);
        var name = $scope.service.name;
        $scope.showMRLComm = true;
        
        $scope.updateState = function(service) {
            $scope.service = service;
            $scope.board = $scope.service.board;
            $scope.isConnected = ($scope.service.portName != null);
            $scope.isConnectedImage = ($scope.service.portName != null) ? "connected" : "disconnected";
            $scope.connectText = ($scope.service.portName == null) ? "connect" : "disconnect";
        }

        // $scope display methods
        $scope.onBoardChange = function(board) {
            if ($scope.service.board != $scope.board) {
                mrl.sendTo(name, 'setBoard', board);
                mrl.sendTo(name, 'broadcastState');
            }
        }
        
        $scope.toggleShowMRLComm = function() {
            $scope.showMRLComm = ($scope.showMRLComm) ? false : true;
        }
        
        $scope.aceLoaded = function(_editor) {
            // Options
            _editor.setReadOnly(true);
            _editor.$blockScrolling = Infinity;
            _editor.setValue($scope.service.sketch.data, -1);

            //editor.setValue(str, -1) // moves cursor to the start
        
        };
        
        $scope.aceChanged = function(e) {
        //
        };

        // initial update
        $scope.service = mrl.getService($scope.service.name);
        $scope.updateState($scope.service);

        /*
        var canvas = document.getElementById('myCanvas');
        if (canvas.getContext) {
            $log.info("drawing");
            var ctx = canvas.getContext("2d");
            //clear the canvas
            ctx.clearRect(0, 10, canvas.width, canvas.height);
            
            ctx.fillRect(0, 10, width, height);
        }
        */
        
        $scope.gui.initDone();
    }]);
