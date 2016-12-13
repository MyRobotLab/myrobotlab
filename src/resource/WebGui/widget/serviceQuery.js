angular.module('mrlapp.service').directive('serviceQuery', ['mrl', '$log', function(mrl, $log) {
    return {
        restrict: "E",
        templateUrl: 'widget/serviceQuery.html',
        scope: {
           // serviceName: '@',
            interface: '@',
            selection: '='
        },
        // scope: true,
        link: function(scope, element) {
            var _self = this;
            var interface = scope.interface;
            scope.services = mrl.getServicesFromInterface(interface);
            
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
