 /*
  from: serviceCtrl + service.html     
*/
angular.module('mrlapp.service')
.directive('serviceBody', ['$log', '$compile', function($log, $compile) {
        $log.info('serviceBodyDirective');
        return {
            scope: {
                panel: '=',
                service: '=',
                panelname: '='
            },
            link: function(scope, elem, attr) {
                $log.info('serviceBodyDirective.link');
                var ctrl = "";
                try {
                    var bla = scope.service;
                    ctrl = scope.panel.service.simpleName + "GuiCtrl";
                    var html = '<div ng-controller=\"' + ctrl + '\"><div service-body-second type="' + attr.type + '"></div></div>';
                    elem.html(html).show();
                    $compile(elem.contents())(scope);
                } catch (err) {
                    $log.error("serviceBodyDirective threw compiling ", ctrl, err);
                }
            }
        };
    }])

.directive('serviceBodySecond', ['$log', function($log) {
        $log.info('serviceBodyDirective - serviceBodySecond');
        return {
            //inject template into the service-panel
            templateUrl: function(element, attr) {
                return 'service/views/' + attr.type + 'gui.html';
            }
        };
    }]);
