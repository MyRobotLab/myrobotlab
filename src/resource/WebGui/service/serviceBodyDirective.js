angular.module('mrlapp.service')
        .directive('serviceBody', ['$compile', '$templateCache', '$log', 'mrl', function ($compile, $templateCache, $log, mrl) {
                return {
                    scope: {
                        panel: '='
                    },
                    link: function (scope, elem, attr) {

                        elem.css({
                            'overflow-x': 'auto',
                            'overflow-y': 'auto'
                        });

                        scope.panel.notifySizeYChanged = function (height) {
                            elem.css({
                                height: height + 'px'
                            });
                        };

                        scope.panel.getCurrentHeight = function () {
                            return elem.height();
                        };

                        var isUndefinedOrNull = function (val) {
                            return angular.isUndefined(val) || val === null;
                        };

                        var watch = scope.$watch(function () {
                            return scope.panel.scope;
                        }, function () {
                            if (!isUndefinedOrNull(scope.panel.scope)) {
                                watch();
                                $log.info('got scope! using it', scope.panel.name);
                                var newscope = scope.panel.scope;
                                var html = $templateCache.get(scope.panel.simpleName + 'Gui.html');
                                elem.html(html).show();
                                $compile(elem.contents())(newscope);
                            }
                        });
                    }
                };
            }]);