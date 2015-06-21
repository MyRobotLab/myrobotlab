angular.module('mrlapp.service')

        .directive('serviceBody', [function () {
                return {
                    scope: {
                        fw: '=',
                        data: '=',
                        guidata: '=',
                        methods: '='
//                        inst: '=inst',
                    },
//                    controller: "@",  //Not working as I
//                    name: "ctrlName", //want it to
                    link: function (scope, elem, attr) {
//                        scope.fw = attr.inst.fw;
//                        scope.data = attr.inst.data;
//                        scope.guidata = attr.inst.guidata;
//                        scope.methods = attr.inst.methods;

                        scope.getContentUrl = function () {
                            //TODO: TEST THIS! - seems to not work as expected
                            var template = 'service/views/' + attr.type + 'gui.html';
                            return (angular.isDefined(template)) ? template : 'service/views/default.html';
//                            return 'service/views/' + attr.type + 'gui.html';
                        };
                    },
                    template: '<div ng-include="getContentUrl()"></div>'
                };
            }]);