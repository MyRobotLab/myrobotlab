angular.module('mrlapp.service')
        .directive('dragDirective', ['$document', 'panelSvc', function ($document, panelSvc) {
                return {
                    link: function (scope, element, attr) {

                        var startX = 0;
                        var startY = 0;
                        var resizeX = 10;

                        element.on('mousedown', function (event) {
                            startX = event.pageX - scope.panel.posX;
                            startY = event.pageY - scope.panel.posY;
                            // FIXME - remove 'free' stuff...
                            if (scope.panel.size != 'free' ||
                                    (scope.panel.size == 'free'
                                            && startX < element.width() - resizeX)) {
                                // Prevent default dragging of selected content
                                event.preventDefault();

                                panelSvc.putPanelZIndexOnTop(scope.panel.name);

                                element.css({
                                    cursor: 'move'
                                });
                                $document.on('mousemove', mousemove);
                                $document.on('mouseup', mouseup);
                            }
                        });

                        function mousemove(event) {
                            // FIXME ! - should "only" be updating through panelSvc !!!
                            // NOT modifying the panel data directly !
                            scope.panel.posY = event.pageY - startY;
                            scope.panel.posX = event.pageX - startX;
                            scope.panel.notifyPositionChanged();
                        }

                        function mouseup(event) {
                            $document.off('mousemove', mousemove);
                            $document.off('mouseup', mouseup);
                            panelSvc.savePanel(scope.panel.name);
                            element.css({
                                cursor: 'auto'
                            });
                        }
                    }
                };
            }]);