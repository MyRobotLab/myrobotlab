angular.module('mrlapp.service').directive('serviceBody', ['$compile', '$templateCache', 'mrl', function($compile, $templateCache, mrl) {
    return {
        scope: {
            panel: '='
        },
        // controller: 'serviceCtrl',
        link: function(scope, elem, attr) {

            console.log('serviceBodyDirective - link')
            /*
            elem.css({
                'overflow-x': 'auto',
                'overflow-y': 'auto'
            })
            */

            scope.panel.notifySizeYChanged = function(height) {
                elem.css({
                    height: height + 'px'
                })
            }
            

            scope.panel.getCurrentHeight = function() {
                return elem.height()
            }
            

            var isUndefinedOrNull = function(val) {
                return angular.isUndefined(val) || val === null
            }

            var watch = scope.$watch(function() {
                return scope.panel.scope
            }, function() {
                if (!isUndefinedOrNull(scope.panel.scope)) {
                    watch()
                    console.info('================ got scope! using it', scope.panel.name)
                    var newscope = scope.panel.scope
                    newscope.updateServiceData = function() {
                        //get an updated / fresh servicedata & convert it to json
                        var servicedata = mrl.getService(scope.panel.name)
                        newscope.servicedatajson = JSON.stringify(servicedata, null, 2)
                    }

                    newscope.toggleVirtual = function(virtual) {
                        var service = mrl.getService(scope.panel.name)
                        //service.isVirtual = !service.isVirtual
                        mrl.sendTo(scope.panel.name, 'setVirtual', virtual)
                    }

                    newscope.export = function() {
                        mrl.sendTo(scope.panel.name, 'exportAll')
                    }

                    var header = $templateCache.get('service/tab-header.html')
                    var content = $templateCache.get(scope.panel.simpleName + 'Gui.html')
                    var footer = $templateCache.get('service/tab-footer.html')
                    elem.html(header + content + footer).show()
                    // not a bad idea, however it led to performance problems when updating the servo gui during movements
                    // when the html was in a hidden state but all the properties where ng-repeated as part of the dom
                    // newscope.properties = mrl.getProperties(newscope.service)
                    $compile(elem.contents())(newscope)
                }
            })
        }
    }
}
])
