angular.module('mrlapp.service')
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

//                            if (!width) {
//                                scope.resetResizing();
//                            }
                            width = width || scope.panel.panelsize.sizes[scope.panel.panelsize.aktsize].width;

                            element.css({
                                width: width + 'px'
                            });

                        };
                        scope.panel.notifySizeChanged();
                        
                        scope.panel.getCurrentWidth = function () {
                            return element.width();
                        };

                        scope.panel.notifyZIndexChanged = function () {
                            element.css({
                                'z-index': scope.panel.zindex
                            });
                        };
                        scope.panel.notifyZIndexChanged();

//                        scope.$watch(function () {
//                            return element.height();
//                        }, function () {
//                            scope.panel.height = element.height();
//                        });

                        //position: 'absolute' is necessary (even tough it introduces some more work)
                        //without it other panels jump / glitch around when a panel is (re-)moved from this list
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
            }]);