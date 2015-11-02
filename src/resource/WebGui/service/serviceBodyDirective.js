angular.module('mrlapp.service')
        .directive('serviceBody', ['$compile', '$templateCache', '$log', 'mrl', function ($compile, $templateCache, $log, mrl) {
                return {
                    scope: {
                        panel: '='
                    },
                    link: function (scope, elem, attr) {

                        var isUndefinedOrNull = function (val) {
                            return angular.isUndefined(val) || val === null;
                        };

                        var watch = scope.$watch(function () {
                            return scope.panel.scope;
                        }, function () {
                            if (!isUndefinedOrNull(scope.panel.scope)) {
                                watch();
                                $log.info('got scope! using it', scope.panel.name, scope.panel.panelname);
                                var newscope = scope.panel.scope;
                                var html = $templateCache.get(scope.panel.simpleName + 'gui.html');
                                elem.html(html).show();
                                $compile(elem.contents())(newscope);
                            }
                        });
                    }
                };
            }]);