/**
* options:
*        interface: 'UltrasonicSensorController',
*        attach: - call back function on change        
*        attachName: - name of selected model
* FIXME - the UI should determine if the two attaching services are local
* to one another - if they are - then it should use shortnames
*
*/
angular.module('mrlapp.service').directive('attach', ['mrl', function(mrl) {
    return {
        restrict: "E", /* element only */
        templateUrl: 'widget/attach.html',
        scope: {
            options: '='/* 2 way binding - isolated scope */
        },
        // scope: false,
        // scope: true,
        link: function(scope, element) {
            var _self = this
            scope.mrl = mrl
            if (!scope.options.controllerTitle){
                scope.options.controllerTitle = 'controller'
            }
            
            // if this was full canonical name - would the msg.send be unusseary
            scope.interfaceName = scope.options.interface
            if (scope.interfaceName.indexOf('.') == -1) {
                scope.interfaceName = 'org.myrobotlab.service.interfaces.' + scope.options.interface
            }

            scope.attach = function(serviceName) {
                scope.options.attach(serviceName)
                scope.options.attachName = serviceName
            }
        }
    }
}
])
