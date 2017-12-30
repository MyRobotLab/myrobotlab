angular.module('mrlapp.service.JoystickGui', []).controller('JoystickGuiCtrl', ['$scope', '$log', 'mrl', '$controller', function($scope, $log, mrl, $controller) {
    $log.info('JoystickGuiCtrl');
    var _self = this;
    var msg = this.msg;

    $scope.controller = 'controllers';
    $scope.input = {
        "id": "",
        "value": ""
    };

    $scope.axisMapOrdinal = {};
    $scope.axisValues = [];
    $scope.axisObject = {
        "key": "Series 1",
        "values": $scope.axisValues //[['pov', 0], ['pov', -0.75], ['x', 0], ['x', 0.93]]
    };

    $scope.axis = [$scope.axisObject];

    $scope.barValue = 'None';
    $scope.globalInt = 15;

    this.updateState = function(service) {
        $scope.service = service;
        $scope.buttons = {};
        $scope.other = {};

        // re-initialize axis data
        $scope.axisMapOrdinal = {};
        $scope.axisValues = [];
        $scope.axisObject = {
            "key": "Series 1",
            "values": $scope.axisValues //[['pov', 0], ['pov', -0.75], ['x', 0], ['x', 0.93]]
        };
        $scope.axis = [$scope.axisObject];

        $scope.controller = service.controller;

        for (var compId in service.components) {
            if (service.components.hasOwnProperty(compId)) {
                var component = service.components[compId];
                if (component.type == "Button" || component.type == "Key") {
                    $scope.buttons[component.id] = component;
                } else if (component.type == "Axis") {
                    //if ($scope.axisMapOrdinal.hasOwnProperty(input.id)) {
                    // $scope.axisMapOrdinal[component.id] = [component.id, 0];
                    // the 0 anchor
                    $scope.axisValues.push([component.id, 0]);
                    // set id to ordinal at next location
                    var ordinal = $scope.axisValues.length;
                    $scope.axisMapOrdinal[component.id] = ordinal;
                    // the variable end - its changes..
                    $scope.axisValues.push([component.id, 0]);
                    // watch this variable
                    // $scope.$watch(data, $scope.axisValues[ordinal][1]); 
                    //}
                } else {
                    $scope.other[component.id] = component;
                }
            }
        }
    }
    ;

    _self.updateState($scope.service);
    this.onMsg = function(inMsg) {

        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0]);
            //$scope.update(); // FIXME update() just sets random data
            $scope.$apply();
            break;
        case 'onComponents':
            $scope.pulseData = inMsg.data[0];
            $scope.$apply();
            break;
        case 'onJoystickInput':
            input = inMsg.data[0];
            $scope.input = input;

            // for buttons & maintaining values
            var comp = $scope.service.components[input.id];
            comp.value = input.value;

            if ($scope.axisMapOrdinal.hasOwnProperty(input.id)) {
                $scope.axisValues[$scope.axisMapOrdinal[input.id]] = [input.id, input.value];
            }

            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
    }
    ;

    $scope.setController = function(index) {
        msg.send("setController", index);
    }
    ;

    msg.subscribe('publishJoystickInput');
    msg.subscribe(this);

    $scope.xAxisTickFormatFunction = function() {
        return function(d) {
            //return d3.time.format('%b')(new Date(d));
            return d;
        }
    }

    var colorCategory = d3.scale.category20b();

    $scope.colorFunction = function() {
        return function(d, i) {
            // return colorCategory(i);
            return '#CCC';
        }
        ;
    }
}
]);
