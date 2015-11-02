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

                        //only here for compability reasons
                        scope.ctrlfunctions = {};
                        scope.ctrlfunctions.getService = function () {
                            return mrl.getService(scope.panel.name);
                        };
                        scope.ctrlfunctions.subscribe = function (method) {
                            return mrl.subscribe(scope.panel.name, method);
                        };
                        scope.ctrlfunctions.send = function (method, data) {
                            //TODO & FIXME !important! - what if it is has more than one data?
                            if (isUndefinedOrNull(data)) {
                                return mrl.sendTo(scope.panel.name, method);
                            } else {
                                return mrl.sendTo(scope.panel.name, method, data);
                            }
                        };
                        scope.ctrlfunctions.setPanelCount = function (number) {
                            $log.info('setting panelcount', number);
                            serviceSvc.notifyPanelCountChanged(scope.panel.name, number);
                        };
                        scope.ctrlfunctions.setPanelNames = function (names) {
                            $log.info('setting panelnames', names);
                            serviceSvc.notifyPanelNamesChanged(scope.panel.name, names);
                        };
                        scope.ctrlfunctions.setPanelShowNames = function (show) {
                            $log.info('setting panelshownames', show);
                            serviceSvc.notifyPanelShowNamesChanged(scope.panel.name, show);
                        };
                        scope.ctrlfunctions.setPanelSizes = function (sizes) {
                            $log.info('setting panelsizes', sizes);
                            serviceSvc.notifyPanelSizesChanged(scope.panel.name, sizes);
                        };

                        //prepare dynamic controller injection
                        var html = '<div service-ctrl-next '
                                + 'controller-name="' + scope.panel.simpleName + 'GuiCtrl" '
                                + 'name="panel.name" '
                                + 'service="service" '
                                + 'msginterface="msginterface" '
                                + 'msgmethods="msgmethods" '
                                + 'size="panel.size" panelname="panel.panelname" cb="cb" '
                                + 'get-service="ctrlfunctions.getService" '
                                + 'subscribe="ctrlfunctions.subscribe" '
                                + 'send="ctrlfunctions.send" '
                                + 'set-panel-count="ctrlfunctions.setPanelCount" '
                                + 'set-panel-names="ctrlfunctions.setPanelNames" '
                                + 'set-panel-show-names="ctrlfunctions.setPanelShowNames" '
                                + 'set-panel-sizes="ctrlfunctions.setPanelSizes"'
                                + '></div>';


                        var watch = scope.$watch(function () {
                            return scope.panel.templatestatus;
                        }, function () {
                            if (!isUndefinedOrNull(scope.panel.templatestatus) && scope.panel.templatestatus == 'loaded') {
                                watch();
                                $log.info('deps loaded, start ctrl', scope.panel.name, scope.panel.panelname);

                                mrl.createMsgInterface(scope.panel.name).then(function (msg_) {
                                    $log.info('msgInterface received', scope.panel.name, scope.panel.panelname);
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
                        service: '=' //Does it make sense to give him an instance of itself that may be outdated in just a bit? Or let it fetch it's instance himself`?
                    },
                    bindToController: {
                        getService: '&',
                        subscribe: '&',
                        send: '&',
                        setPanelCount: '&',
                        setPanelNames: '&',
                        setPanelShowNames: '&',
                        setPanelSizes: '&',
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