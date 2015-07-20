angular.module('mrlapp.service')
        .directive('serviceDirective', ['$log','$document', function ($log, $document) {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        service: '=service'
                    },
                    templateUrl: 'service/service.html',
                    controller: 'ServiceCtrl',
                    link: function (scope, element, attr) {
                        $log.info('serviceDirective.link ', attr)
                        scope.show = true;
                        // FIXME - we want resizable by draggable corner handles
                        scope.notifySizeChanged = function() {
                            element.css({
                                width: scope.service.panelsize.sizes[scope.service.panelsize.aktsize].width + 'px'
                            });
                        };

                    }
                };
            }]);
