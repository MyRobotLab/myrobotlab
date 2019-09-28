angular.module('mrlapp.service').directive('serviceBody', ['$compile', '$templateCache', '$log', 'mrl', function($compile, $templateCache, $log, mrl) {
    return {
        scope: {
            panel: '='
        },
        link: function(scope, elem, attr) {

            elem.css({
                'overflow-x': 'auto',
                'overflow-y': 'auto'
            });

            scope.panel.notifySizeYChanged = function(height) {
                elem.css({
                    height: height + 'px'
                });
            }
            ;

            scope.panel.getCurrentHeight = function() {
                return elem.height();
            }
            ;

            var isUndefinedOrNull = function(val) {
                return angular.isUndefined(val) || val === null;
            };

            var watch = scope.$watch(function() {
                return scope.panel.scope;
            }, function() {
                if (!isUndefinedOrNull(scope.panel.scope)) {
                    watch();
                    $log.info('got scope! using it', scope.panel.name);
                    var newscope = scope.panel.scope;
                    newscope.updateServiceData = function() {
                        //get an updated / fresh servicedata & convert it to json
                        var servicedata = mrl.getService(scope.panel.name);
                        newscope.servicedatajson = JSON.stringify(servicedata, null, 2);
                    }
                    var header = $templateCache.get('service/tab-header.html');
                    var content = $templateCache.get(scope.panel.simpleName + 'Gui.html');
                    // var footer = $templateCache.get('service/tab-footer.html');
                    elem.html(header + content /*+ footer*/).show();
                    $compile(elem.contents())(newscope);
                }
            });
        }
    };
}
]);
