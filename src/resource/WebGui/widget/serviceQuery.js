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
        // replace: true,
        scope: {
            // serviceName: '@',
            interface: '@',
            selection: '='
        },
        // scope: true,
        link: function(scope, iElement, iAttrs, ngModelCtrl) {
            var _self = this;
            var interface = scope.interface;
            // _self.ngModelCtrl = ngModelCtrl;
            scope.services = mrl.getServicesFromInterface(interface);
            scope.onSelect = function(item, model, label) {
                scope.$item = item;
                scope.controllerName = label;
                scope.$model = model;
                scope.$label = label;
                $log.info('here');
                $log.info(_self.ngModelCtrl);
                scope.controllerName = c;
            }
            scope.ngModelOptionsSelected = function(value) {
                if (arguments.length) {
                    _selected = value;
                } else {
                    return _selected;
                }
            }
            ;
            // FIXME FIXME FIXME ->> THIS SHOULD WORK subscribeToServiceMethod  <- but doesnt
            // mrl.subscribeToService(_self.onMsg, name);
            // this siphons off a single subscribe to the webgui
            // so it will be broadcasted back to angular
            // mrl.subscribe(name, 'publishPinArray');
            // mrl.subscribeToServiceMethod(_self.onMsg, name, 'publishPinArray');
            // initializing display data      
        }
    };
}
]);
