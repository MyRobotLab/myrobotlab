angular.module('mrlapp.service')
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