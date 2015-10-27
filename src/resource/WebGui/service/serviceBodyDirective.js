angular.module('mrlapp.service')
        .directive('serviceBody', ['$compile', '$templateCache', '$log', 'mrl', function ($compile, $templateCache, $log, mrl) {
                return {
                    scope: {
                        panel: '=',
                        ctrlfunctions: '='
                    },
                    link: function (scope, elem, attr) {

                        //prepare dynamic controller injection
//                        var html = '<div service-body-next '
//                                + 'controller-name="' + scope.panel.simpleName + 'GuiCtrl" '
//                                + 'name="' + scope.panel.name + '" '
//                                + 'size="panel.size" panelname="panel.panelname" cb="cb" '
//                                + 'get-service="ctrlfunctions.getService" '
//                                + 'subscribe="ctrlfunctions.subscribe" '
//                                + 'send="ctrlfunctions.send" '
//                                + 'set-panel-count="ctrlfunctions.setPanelCount" '
//                                + 'set-panel-names="ctrlfunctions.setPanelNames" '
//                                + 'set-panel-show-names="ctrlfunctions.setPanelShowNames" '
//                                + 'set-panel-sizes="ctrlfunctions.setPanelSizes"'
//                                + '></div>';
//                        elem.html(html).show();
//                        $compile(elem.contents())(scope);

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
//        .directive('serviceBodyNext', ['mrl', function (mrl) {
//                //dynamic controller & dynamic view
//                return {
//                    scope: {
//                        size: '=',
//                        panelname: '='
//                    },
//                    bindToController: {
//                        getService: '&',
//                        subscribe: '&',
//                        send: '&',
//                        setPanelCount: '&',
//                        setPanelNames: '&',
//                        setPanelShowNames: '&',
//                        setPanelSizes: '&'
//                    },
//                    controller: "@",
//                    controllerAs: "guictrl",
//                    name: "controllerName",
//                    templateUrl: function (element, attr) {
//                        return 'service/views/' + attr.type + 'gui.html';
//                    },
//                    link: function (scope, elem, attr) {
//                        //register service-subscription
//                        mrl.subscribeToService(scope.guictrl.onMsg, attr.name);
//                    }
//                };
//            }]);
//        .directive('serviceBodySecond', [function () {
//                return {
//                    //inject template into the service-panel
//                    templateUrl: function (element, attr) {
//                        return 'service/views/' + attr.type + 'gui.html';
//                    }
//                };
//            }]);