/*
 simple draggable directive using html 5 draggable attribute
TODO - FIXME - is this used / will be used somewhere?
 */
angular.module('mrlapp.service')
        .directive('draggable', ['$log', 'serviceSvc', function ($log, serviceSvc) {
                return {
                    restrict: 'A',
                    link: function (scope, elem, attr, ctrl) {
                        elem.parent().draggable();

                        elem.parent().bind('mousedown', function (e) {
                            e.stopPropagation();
                            src = e.currentTarget.id;
                            //elem.css('z-index', '1000');
                            $log.info(elem);
                            //var src = angular.element(e.srcElement);
                            $log.info('mousedown ', src);
                            elem.css('z-index', serviceSvc.getNextZIndex(src));
                            elem.css('position', 'relative');
                            //elem.css('position', 'absolute');
                            //elem.css('top', 40);
                            //elem.css('left', 40);
                            //elem.css('position', 'static');
                            //elem.css('position', 'fixed');
                            //elem.css('position', 'absolute');
                            //elem.css('position', 'relative');
                        }
                        );

                        elem.parent().bind('mouseup', function (e) {
                            e.stopPropagation();
                            src = e.currentTarget.id;
                            //elem.css('z-index', '1000');
                            $log.info(elem);
                            //var src = angular.element(e.srcElement);
                            $log.info('mouseup ', src, ' ', elem.css('position'), ' ', elem.offset());
                            //serviceSvc.setPos(src)
                            //elem.css('z-index', serviceSvc.getNextZIndex(src));
                            //elem.css('position', 'absolute');
                            //elem.css('top', 40);
                            //elem.css('left', 40);
                            //elem.css('position', 'static');
                            //elem.css('position', 'fixed');
                            //elem.css('position', 'absolute');
                            //elem.css('position', 'relative');
                        }
                        );
                        /*
                         angular.element(document).bind('mousedown', function() {
                         $log.info('clicked on document');
                         }
                         );
                         */
                    }
                };
            }
        ]);
