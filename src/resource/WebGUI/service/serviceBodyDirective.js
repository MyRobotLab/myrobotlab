angular.module('mrlapp.service')

        .directive('serviceBody', ['$compile', function ($compile) {
                return {
                    scope: {
                        gui: '=',
                        service: '=',
                        panelindex: '='
                    },
                    link: function (scope, elem, attr) {
                        //inject template & controller into the service-panel
                        var html = '<div ng-controller=\"' + attr.simplename + 'GuiCtrl\"><div ng-include="\'service/views/' + attr.type + 'gui.html\'"></div></div>';
                        elem.html(html).show();
                        $compile(elem.contents())(scope);
                    }
                };
            }]);