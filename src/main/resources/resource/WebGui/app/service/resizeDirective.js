angular.module('mrlapp.service')
        .directive('resizeDirective', ['$document', function ($document) {
                return {
                    link: function (scope, element, attr) {

                        var x = 0;
                        var startX = 0;
                        
                        var y = 0;
                        var startY = 0;

                        scope.resetResizing = function () {
                            //can this be removed now?
                            //I think so
                            //TODO - remove
                            x = 0;
                            startX = 0;
                            y = 0;
                            startY = 0;
                        };

                        element.on('mousedown', function (event) {
                            x = scope.panel.getCurrentWidth();
                            y = scope.panel.getCurrentHeight();
                            
                            startX = event.pageX - x;
                            startY = event.pageY - y;

                            // Prevent default dragging of selected content
                            event.preventDefault();

                            element.css({
                                cursor: 'nw-resize'
                            });
                            $document.on('mousemove', mousemove);
                            $document.on('mouseup', mouseup);
                        });

                        function mousemove(event) {
                            x = event.pageX - startX;
                            y = event.pageY - startY;
                            
                            scope.panel.notifySizeChanged(x);
                            scope.panel.notifySizeYChanged(y);
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