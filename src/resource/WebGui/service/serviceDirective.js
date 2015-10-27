angular.module('mrlapp.service')
        .directive('serviceCtrlDirective', ['$compile', '$log', 'mrl', 'serviceSvc', function ($compile, $log, mrl, serviceSvc) {
                //TODO - move this
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
                //TODO - move this
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
//                    templateUrl: function (element, attr) {
//                        return 'service/views/' + attr.type + 'gui.html';
//                    },
                    link: function (scope, elem, attr) {
                        //register service-subscription
                        console.log(scope.name, 'serviceCtrlNext-link');
                        
                        mrl.subscribeToService(scope.guictrl.onMsg, scope.name);
                        serviceSvc.controllerscope(scope.name, scope);
                    }
                };
            }])
        .directive('serviceDirective', [function () {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        panel: '=panel'
                    },
                    templateUrl: 'service/service.html',
                    controller: 'serviceCtrl',
                    link: function (scope, element, attr) {
                        //width - change on size change
                        //-->preset sizes (width is undefined)
                        //-->free-form-resizing (width is defined)
                        scope.panel.notifySizeChanged = function (width) {
                            if (!width) {
                                scope.resetResizing();
                            }
                            width = scope.panel.panelsize.sizes[scope.panel.panelsize.aktsize].width + width || scope.panel.panelsize.sizes[scope.panel.panelsize.aktsize].width;
                            element.css({
                                width: width + 'px'
                            });
                        };
                        scope.panel.notifySizeChanged();

                        scope.panel.notifyZIndexChanged = function () {
                            element.css({
                                'z-index': scope.panel.zindex
                            });
                        };
                        scope.panel.notifyZIndexChanged();

                        scope.$watch(function () {
                            return element.height();
                        }, function () {
                            scope.panel.height = element.height();
                        });

                        //position: 'absolute' is necessary (even tough it introduces some more work)
                        //without it other panels jump / glitch around when a panel is moved from this list
                        if (!scope.panel.panelsize.sizes[scope.panel.panelsize.aktsize].denyMoving) {
                            element.css({
                                position: 'absolute'
                            });
                        }

                        scope.panel.notifyPositionChanged = function () {
                            element.css({
                                top: scope.panel.posy + 'px',
                                left: scope.panel.posx + 'px'
                            });
                        };
                        if (!scope.panel.panelsize.sizes[scope.panel.panelsize.aktsize].denyMoving) {
                            scope.panel.notifyPositionChanged();
                        }
                    }
                };
            }])
        .directive('dragDirective', ['$document', 'serviceSvc', function ($document, serviceSvc) {
                return {
                    link: function (scope, element, attr) {

                        var startX = 0;
                        var startY = 0;
                        var resizeX = 10;

                        element.on('mousedown', function (event) {
                            startX = event.pageX - scope.panel.posx;
                            startY = event.pageY - scope.panel.posy;
                            if (((!scope.panel.panelsize.sizes[scope.panel.panelsize.aktsize].freeform)
                                    || (scope.panel.panelsize.sizes[scope.panel.panelsize.aktsize].freeform
                                            && startX < element.width() - resizeX))
                                    && !scope.panel.panelsize.sizes[scope.panel.panelsize.aktsize].denyMoving) {
                                // Prevent default dragging of selected content
                                event.preventDefault();

                                serviceSvc.putPanelZIndexOnTop(scope.panel.name, scope.panel.panelname);

                                element.css({
                                    cursor: 'move'
                                });
                                $document.on('mousemove', mousemove);
                                $document.on('mouseup', mouseup);
                            }
                        });

                        function mousemove(event) {
                            scope.panel.posy = event.pageY - startY;
                            scope.panel.posx = event.pageX - startX;
                            scope.panel.notifyPositionChanged();
                        }

                        function mouseup() {
                            $document.off('mousemove', mousemove);
                            $document.off('mouseup', mouseup);
                            element.css({
                                cursor: 'auto'
                            });
                        }
                    }
                };
            }])
        .directive('resizeDirective', ['$document', function ($document) {
                return {
                    link: function (scope, element, attr) {

                        var x = 0;
                        var startX = 0;

                        scope.resetResizing = function () {
                            x = 0;
                            startX = 0;
                        };

                        element.on('mousedown', function (event) {
                            startX = event.pageX - x;

                            // Prevent default dragging of selected content
                            event.preventDefault();

                            element.css({
                                cursor: 'e-resize'
                            });
                            $document.on('mousemove', mousemove);
                            $document.on('mouseup', mouseup);
                        });

                        function mousemove(event) {
                            x = event.pageX - startX;
                            scope.panel.notifySizeChanged(x);
                        }

                        function mouseup() {
                            $document.off('mousemove', mousemove);
                            $document.off('mouseup', mouseup);
                            element.css({
                                cursor: 'auto'
                            });
                        }
                    }
                };
            }]);
