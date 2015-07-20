angular.module('mrlapp.service.arduinogui', [])
.controller('ArduinoGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
        console.log('ArduinoGuiCtrl');
        _self = this;
        
        var onMsg = function(msg) {
            //console.log('CALLBACK - ' + msg.method);
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
                    console.log("ERROR - unhandled method " + msg.method);
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
        
        $scope.aceLoaded = function(editor) {
            // Options
            editor.setReadOnly(true);
            editor.$blockScrolling = Infinity;
            editor.setValue($scope.service.sketch.data, -1);        
        };
        
        $scope.aceChanged = function(e) {
        //
        };

        // initial update
        $scope.service = mrl.getService($scope.service.name);
        $scope.updateState($scope.service);


        // subsumption !!! - we want to repress serial messages they are
        // WAY TO MANY AND NOT INTEREsTING !!
        // we want to unregister tx & rx events !!
        // now the tricky part of finding "real" name of a peer ?
        // mrl.getPeerName()
        // mrl.sendTo(name, 'unsubscribe', board);
        
        $scope.panel.initDone();
    }]);
