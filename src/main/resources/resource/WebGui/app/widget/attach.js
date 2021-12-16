angular.module('mrlapp.service').directive('attach', ['mrl', function(mrl) {
    return {
        restrict: "E",
        templateUrl: 'widget/attach.html',
        scope: {
            // serviceName: '=',
            options: '='//,
            // portDirectiveScope: "=ngModel"
        },
        // scope: false,
        // scope: true,
        link: function(scope, element) {
            var _self = this
            scope.mrl = mrl
            
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
