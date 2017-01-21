angular.module('mrlapp.service')
        .directive('serviceCtrlDirective', ['$compile', '$log', 'mrl', 'serviceSvc', function ($compile, $log, mrl, serviceSvc) {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        panel: '=panel'
                    },
                    link: function (scope, elem, attr) {

                        scope.service = mrl.getService(scope.panel.name);

                        var isUndefinedOrNull = function (val) {
                            return angular.isUndefined(val) || val === null;
                        };

                        scope.panelconfig = {};
                        scope.panelconfig.setPanelCount = function (number) {
                            $log.info('setting panelcount', number);
                            serviceSvc.notifyPanelCountChanged(scope.panel.name, number);
                        };
                        scope.panelconfig.setPanelNames = function (names) {
                            $log.info('setting panelnames', names);
                            serviceSvc.notifyPanelNamesChanged(scope.panel.name, names);
                        };
                        scope.panelconfig.setPanelShowNames = function (show) {
                            $log.info('setting panelshownames', show);
                            serviceSvc.notifyPanelShowNamesChanged(scope.panel.name, show);
                        };

                        //prepare dynamic controller injection
                        var html = '<div service-ctrl-next '
                                + 'controller-name="' + scope.panel.simpleName + 'GuiCtrl" '
                                + 'name="panel.name" '
                                + 'service="service" '
                                + 'msginterface="msginterface" '
                                + 'msgmethods="msgmethods" '
                                + 'panelconfig="panelconfig" '
                                + 'size="panel.size" panelname="panel.panelname" cb="cb"'
                                + '></div>';


                        var watch = scope.$watch(function () {
                            return scope.panel.templatestatus;
                        }, function () {
                            if (!isUndefinedOrNull(scope.panel.templatestatus) && scope.panel.templatestatus == 'loaded') {
                                watch();
                                $log.info('deps loaded, start ctrl', scope.panel.name, scope.panel.panelname);

                                mrl.createMsgInterface(scope.panel.name).then(function (msg_) {
                                    $log.info('msgInterface received', scope.panel.name, scope.panel.panelname);
                                    scope.panel.msg_ = msg_;
                                    scope.msginterface = msg_;
                                    scope.msgmethods = msg_.temp.msg;

                                    elem.html(html).show();
                                    $compile(elem.contents())(scope);
                                }, function (msg_) {
                                    console.log('msgInterface-meh!');
                                });
                            }
                        });
                    }
                };
            }])
        .directive('serviceCtrlNext', ['mrl', 'serviceSvc', function (mrl, serviceSvc) {
                //dynamic controller
                return {
                    scope: {
                        msg: '=msgmethods',
                        name: '=',
                        service: '=', //Does it make sense to give him an instance of itself that may be outdated in just a bit? Or let it fetch it's instance himself`?
                        size: '='
                    },
                    bindToController: {
                        panelconfig: '=',
                        msg: '=msginterface'
                    },
                    controller: "@",
                    controllerAs: "guictrl",
                    name: "controllerName",
                    link: function (scope, elem, attr) {
                        console.log(scope.name, 'serviceCtrlNext-link');

//                        mrl.subscribeToService(scope.guictrl.onMsg, scope.name);
                        serviceSvc.controllerscope(scope.name, scope);
                    }
                };
            }]);