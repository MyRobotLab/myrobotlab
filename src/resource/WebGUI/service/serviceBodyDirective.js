angular.module('mrlapp.service')

        .directive('serviceBody', ['$compile', function ($compile) {
                return {
                    scope: {
                        fw: '=',
                        data: '=',
                        guidata: '=',
                        methods: '=',
                        panelindex: '='
//                        inst: '=inst',
                    },
//                    controller: "@",  //Not working as I
//                    name: "ctrlName", //want it to
                    link: function (scope, elem, attr) {
//                        scope.fw = attr.inst.fw;
//                        scope.data = attr.inst.data;
//                        scope.guidata = attr.inst.guidata;
//                        scope.methods = attr.inst.methods;

//                        scope.getContentUrl = function () {
//                            //TODO: TEST THIS! - seems to not work as expected (altough it does work)
//                            var template = 'service/views/' + attr.type + 'gui.html';
//                            return (angular.isDefined(template)) ? template : 'service/views/default.html';
////                            return 'service/views/' + attr.type + 'gui.html';
//                        };

                        //inject template & controller into the service-panel
                        var html = '<div ng-controller=\"' + attr.simplename + 'GuiCtrl\"><div ng-include="\'service/views/' + attr.type + 'gui.html\'"></div></div>';
                        elem.html(html).show();
                        $compile(elem.contents())(scope);
                    }
//                    template: '<div ng-include="getContentUrl()"></div>'
                };
            }]);