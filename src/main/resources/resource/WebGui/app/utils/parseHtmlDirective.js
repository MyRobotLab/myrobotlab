angular.module('mrlapp.utils')
        //general HTML-compile/parse directive
        .directive('parseHtml', ['$compile', function ($compile) {
                return {
                    scope: {
                        html: '=html'
                    },
                    link: function (scope, elem, attr) {
                        var compile = function (html) {
                        console.log(scope, attr, html);
                        elem.html(html).show();
                        $compile(elem.contents())(scope);
                        };
                        
                        scope.$watch(attr.html, function (value) {
                            compile(value);
                        });
                        compile(scope.html);
                    }
                };
            }]);