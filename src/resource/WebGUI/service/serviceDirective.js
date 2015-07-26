angular.module('mrlapp.service')
        .directive('serviceDirective', [function () {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        spawndata: '=panel'
                    },
                    templateUrl: 'service/service.html',
                    controller: 'ServiceCtrl',
                    link: function (scope, element, attr) {

                        //width - change on size change
                        //-->preset sizes (width is undefined)
                        //-->free-form-resizing (width is defined)
                        scope.notifySizeChanged = function (width) {
                            if (!width) {
                                scope.resetResizing();
                            }
                            width = scope.spawndata.panelsize.sizes[scope.spawndata.panelsize.aktsize].width + width || scope.spawndata.panelsize.sizes[scope.spawndata.panelsize.aktsize].width;
                            element.css({
                                width: width + 'px'
                            });
                        };
                        scope.notifySizeChanged();

                        scope.spawndata.notifyZIndexChanged = function () {
                            element.css({
                                'z-index': scope.spawndata.zindex
                            });
                        };
                        scope.spawndata.notifyZIndexChanged();

                        scope.$watch(function () {
                            return element.height();
                        }, function () {
                            scope.spawndata.height = element.height();
                        });

                        //position: 'absolute' is necessary (even tough it introduces some more work)
                        //without it other panels jump / glitch around when a panel is moved from this list
                        if (!scope.spawndata.panelsize.sizes[scope.spawndata.panelsize.aktsize].denyMoving) {
                            element.css({
                                position: 'absolute'
                            });
                        }

                        scope.onMoved = function () {
                            element.css({
                                top: scope.spawndata.posy + 'px',
                                left: scope.spawndata.posx + 'px'
                            });
                        };
                        if (!scope.spawndata.panelsize.sizes[scope.spawndata.panelsize.aktsize].denyMoving) {
                            scope.onMoved();
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
                            startX = event.pageX - scope.spawndata.posx;
                            startY = event.pageY - scope.spawndata.posy;
                            if (((!scope.spawndata.panelsize.sizes[scope.spawndata.panelsize.aktsize].freeform)
                                    || (scope.spawndata.panelsize.sizes[scope.spawndata.panelsize.aktsize].freeform
                                            && startX < element.width() - resizeX))
                                    && !scope.spawndata.panelsize.sizes[scope.spawndata.panelsize.aktsize].denyMoving) {
                                // Prevent default dragging of selected content
                                event.preventDefault();

                                ServiceSvc.putPanelZIndexOnTop(scope.spawndata.name, scope.spawndata.panelname);

                                element.css({
                                    cursor: 'move'
                                });
                                $document.on('mousemove', mousemove);
                                $document.on('mouseup', mouseup);
                            }
                        });

                        function mousemove(event) {
                            scope.spawndata.posy = event.pageY - startY;
                            scope.spawndata.posx = event.pageX - startX;
                            scope.onMoved();
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
                            scope.notifySizeChanged(x);
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