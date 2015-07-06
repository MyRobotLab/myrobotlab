angular.module('mrlapp.service')

        .directive('serviceBody', ['$compile', function ($compile) {
                return {
                    scope: {
                        gui: '=',
                        service: '=',
                        size: '=',
                        panelname: '='
                    },
                    link: function (scope, elem, attr) {
                        //inject controller into the service-panel
//                        var html = '<div ng-controller=\"' + attr.simplename + 'GuiCtrl\"><div ng-include="\'service/views/' + attr.type + 'gui.html\'"></div></div>';
                        var html = '<div ng-controller=\"' + attr.simplename + 'GuiCtrl\"><div service-body-second type="' + attr.type + '"></div></div>';
                        elem.html(html).show();
                        $compile(elem.contents())(scope);
                    }
                };
            }])

        .directive('serviceBodySecond', [function () {
                return {
                    //inject template into the service-panel
                    templateUrl: function (element, attr) {
                        return 'service/views/' + attr.type + 'gui.html';
                    }
                };
            }]);