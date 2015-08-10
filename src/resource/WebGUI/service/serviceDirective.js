angular.module('mrlapp.service')
        .directive('serviceDirective', [function () {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        panel: '=panel'
                    },
                    templateUrl: 'service/service.html',
                    controller: 'ServiceCtrl',
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
        .directive('dragDirective', ['$document', 'ServiceSvc', function ($document, ServiceSvc) {
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

                                ServiceSvc.putPanelZIndexOnTop(scope.panel.name, scope.panel.panelname);

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
