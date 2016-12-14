angular.module('mrlapp.service').directive('serviceQuery', ['mrl', '$log', function(mrl, $log) {
    var tpl = "<input type='text' ng-model='controllerName' class='form-control'\
    placeholder='attach'\
    uib-typeahead='controllerName as panel.name for panel in services | filter:{name:$viewValue}'\
    typeahead-min-length='0'\
    typeahead-template-url='widget/serviceQueryTemplate.html'\
    typeahead-on-select='onSelect($item, $model, $label)'></input>";
    return {
        restrict: "E",
        template: tpl,
        require: 'ngModel',
        // define isolated scope bindings with parent bindings
        scope: {
            // serviceName: '@',
            interface: '@',
            select: "&" // callback after a typeahead is selected
        },
        controller: function($scope) {
            $scope.onSelect = function(item, model, label) {
                $scope.item = item;
                $scope.controllerName = label;
                $scope.model = model;
                $scope.label = label;
                $scope.selection = label;
                $scope.select({
                    name: $scope.label
                });
            }
        },
        // build up typeahead with query to the registry for 
        // services which implement applicable interfaces
        link: function(scope, iElement, iAttrs, ngModelCtrl) {
            var interface = scope.interface;
            scope.services = mrl.getServicesFromInterface(interface);
        }
    };
}
]);
