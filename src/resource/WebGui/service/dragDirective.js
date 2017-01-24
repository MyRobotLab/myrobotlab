angular.module('mrlapp.service')
        .directive('dragDirective', ['$document', 'serviceSvc', function ($document, serviceSvc) {
                return {
                    link: function (scope, element, attr) {

                        var startX = 0;
                        var startY = 0;
                        var resizeX = 10;

                        element.on('mousedown', function (event) {
                            startX = event.pageX - scope.panel.posx;
                            startY = event.pageY - scope.panel.posy;
                            if (scope.panel.size != 'free' ||
                                    (scope.panel.size == 'free'
                                            && startX < element.width() - resizeX)) {
                                // Prevent default dragging of selected content
                                event.preventDefault();

                                serviceSvc.putPanelZIndexOnTop(scope.panel.name);

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

                        function mouseup(event) {
                            $document.off('mousemove', mousemove);
                            $document.off('mouseup', mouseup);
                            // TODO send change of position to WebGui !!!
                            element.css({
                                cursor: 'auto'
                            });
                        }
                    }
                };
            }]);