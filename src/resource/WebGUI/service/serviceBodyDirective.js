angular.module('mrlapp.service')
        .directive('serviceBody', ['$compile', function ($compile) {
                return {
                    scope: {
                        data: '=',
                        size: '=',
                        panelname: '=',
                        cb: '='
                    },
                    link: function (scope, elem, attr) {
//                        console.log('test-scope2', scope);
                        //inject controller into the service-panel
                        var html = '<div ng-controller=\"' + attr.simplename + 'GuiCtrl as guictrl\"><div service-body-second type="' + attr.type + '" cb=' + scope.cb + '></div></div>';
                        elem.html(html).show();
                        $compile(elem.contents())(scope);
//                        console.log('test-scope3', scope);
                    }
                };
            }])
        .directive('serviceBodySecond', [function () {
                return {
                    //inject template into the service-panel
                    templateUrl: function (element, attr) {
                        return 'service/views/' + attr.type + 'gui.html';
                    },
//                    //set controller properties
//                    link: function (scope, elem, attr) {
//                        console.log('test-scope', scope, attr);
//                        scope.cb.notifycontrollerisready(scope.guictrl);
//                    }
                };
            }]);