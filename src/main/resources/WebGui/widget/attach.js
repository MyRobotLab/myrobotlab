angular.module('mrlapp.service').directive('attach', ['mrl', '$log', '$compile', function(mrl, $log, $compile) {
    var tpl = "<input type='text' ng-model='bindModel' ng-disabled='bindDisabled' class='form-control'\
    placeholder='attach'\
    uib-typeahead='controllerName as panel.name for panel in services | filter:{name:$viewValue}'\
    typeahead-min-length='0'\
    typeahead-template-url='widget/attachTemplate.html'\
    typeahead-on-select='onSelect($item, $model, $label)'/>";
    return {
        restrict: "AE",
        template: tpl,
        replace: false,
        // require: ['ngModel', 'ngDisabled'],
        require: 'ngModel',
        // define isolated scope bindings with parent bindings
        scope: {
            interface: '@',
            // interface to query
            select: "&",
            // ngModel: '=',
            bindModel:'=ngModel',
            bindDisabled:'=ngDisabled',
            bindAttr: '='
            // callback after a typeahead is selected
            // disabled: '=ngDisabled'
        },
        controller: function($scope) {
 /*           if ($scope.disabled) {
                var x = 1 + 1;
            }
            */
            $scope.onSelect = function(item, model, label) {
                $scope.item = item;
                $scope.controllerName = label;
                $scope.model = model;
                $scope.label = label;
                $scope.selection = label;
                // this concept is cool its the other side
                // of dependency injection - where you splat an object
                // against a method signature and have the names sort the mess out ;)
                $scope.select({
                    name: $scope.label
                });
                /*
                 if($scope.disabled){
                    $scope
                }
                */
            }
        },
        // build up typeahead with query to the registry for
        // services which implement applicable interfaces
        link: function($scope, elem, attrs, ngModelCtrl) {

            // builds appropriate list of services which it can 'attach' to
            var interface = $scope.interface;
            $scope.services = mrl.getServicesFromInterface(interface);

            /*
            var textField = $('input', elem).attr('ng-model', attrs.ngModel);
            $compile(textField)($scope.$parent);
            */

            /*
            scope.$watch(attrs.conditionalAutofocus, function(){
                if (scope.$eval(attrs.disabled)) {
                    element.focus();
                }else{
                    element.blur();
                }
            });
            */

            /*
            if (scope.condition()) {
                attrs.$set('autofocus', 'true');
            }
            if (scope.disabled) {//is disabled
            }
            */
        }
    };
}
]);
