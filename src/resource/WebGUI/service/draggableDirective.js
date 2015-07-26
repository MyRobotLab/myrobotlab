/*
 simple draggable directive using html 5 draggable attribute
*/
angular.module('mrlapp.service')
.directive('draggable', ['$log','serviceSvc',function($log, serviceSvc) {
    return {
        restrict: 'A',
        link: function(scope, elem, attr, ctrl) {
            elem.draggable();
            
            elem.bind('mousedown', function(e) {
                e.stopPropagation();
                src = e.currentTarget.id;
                //elem.css('z-index', '1000');
                $log.info(elem);
                //var src = angular.element(e.srcElement);
                $log.info('clicked on directive ', src);
                elem.css('z-index', serviceSvc.getNextZIndex(src));
            }
            );
            /*
            angular.element(document).bind('mousedown', function() {
                console.log('clicked on document');
            }
            );
            */
        }
    };
}
]);
