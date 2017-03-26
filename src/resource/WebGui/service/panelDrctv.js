angular.module('mrlapp.service') 
        .directive('panelDrctv', [function () {
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
                            //check if a new width is to be set
                            //or the width should be resetted
                            if (width) {
                                scope.panel.width = width;
                            }
                            element.css({
                                width: scope.panel.width + 'px'
                            });

                        };
                        scope.panel.notifySizeChanged();
                        
                        scope.panel.getCurrentWidth = function () {
                            return element.width();
                        };

                        scope.panel.notifyZIndexChanged = function () {
                            element.css({
                                'z-index': scope.panel.zIndex
                            });
                        };
                        scope.panel.notifyZIndexChanged();

                        //position: 'absolute' is necessary (even tough it introduces some more work)
                        //without it other panels jump / glitch around when a panel is (re-)moved from this list
                        element.css({
                                position: 'absolute'
                        });

                        scope.panel.notifyPositionChanged = function () {
                            element.css({
                                top: scope.panel.posY + 'px',
                                left: scope.panel.posX + 'px'
                            });
                        };
                        scope.panel.notifyPositionChanged();
                    }
                };
            }]);