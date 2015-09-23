angular.module('mrlapp.service.JoystickGui', [])
.controller('JoystickGuiCtrl', ['$scope', '$log', 'mrl', '$controller', function($scope, $log, mrl, $controller) {
    $log.info('JoystickGuiCtrl');
    var _self = this;

    

    $scope.controller = 'controllers';

    $scope.btn0 = 1;
    //$scope.bnt0.state = 1;
    

    // FIXME needs a prototype to update the mrl service    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.buttons = {};
        $scope.axis = {};
        $scope.other = {};

        $scope.controller = service.controller;
        
        for (var compId in service.components) {
            if (service.components.hasOwnProperty(compId)) {
                var component = service.components[compId];
                if (component.type == "Button" || component.type == "Key") {
                    $scope.buttons[component.id] = component;
                } else if (component.type == "Axis") {
                    $scope.axis[component.id] = component;
                } else {
                    $scope.other[component.id] = component;
                }
            }
        }
    }
    ;
    
    // get latest copy of a services - it will be stale
    _self.updateState(mrl.getService($scope.service.name));
    
    $scope.input = {
        "id": "",
        "value": ""
    };
    
    $scope.panel.onMsg = function(msg) {
        
        switch (msg.method) {
        case 'onState':
            _self.updateState(msg.data[0]);
            $scope.$apply();
            break;
        case 'onComponents':
            $scope.pulseData = msg.data[0];
            $scope.$apply();
            break;
        case 'onJoystickStarted':
            /*
            $scope.label = "Stop";
            $scope.intervalDisabled = true;
            $scope.$apply();
            */
            break;
        case 'onInput':
            $scope.input = msg.data[0];
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
            break;
        }
    }
    ;
    
    $scope.setController = function(index) {
        // $log.info(index);
        mrl.sendTo($scope.service.name, "setController", index);
    }
    ;
    
    
    /*
    $scope.toggle = function(label, interval) {
        if (label == "Start") {
            mrl.sendTo($scope.service.name, "setInterval", interval);
            mrl.sendTo($scope.service.name, "startJoystick");
        } else {
            mrl.sendTo($scope.service.name, "stopJoystick");
        }
    }
    ;
    */
    
    //mrl.subscribe($scope.service.name, 'setController');
    //mrl.subscribe($scope.service.name, 'getComponents');
    mrl.subscribe($scope.service.name, 'publishInput');
    // mrl.subscribe($scope.service.name, 'publishState');
    
    //mrl.send($scope.service.name, 'broadcastState');
    mrl.sendTo($scope.service.name, 'broadcastState');
    
    // mrl.sendTo($scope.service.name, "broadcastState");
    $scope.panel.initDone();
}
]);
