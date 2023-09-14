angular.module('mrlapp.service').directive('serviceBody', ['peer', '$compile', '$templateCache', 'mrl', 'modalService', '$state', '$stateParams', function(peer, $compile, $templateCache, mrl, modalService, $state, $stateParams) {
    return {
        scope: {
            panel: '='
        },
        // controller: 'serviceCtrl',
        link: function(scope, elem, attr) {

            console.info('serviceBodyDirective - link')

            if (!scope.panel) {
                // Intro is trying to access panels probably in ng-show="false" state
                // but it will still explode since they are just being registered
                console.info('service directive panel is null')
                return
            }

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
                    console.info('=== creating new scope for service ', scope.panel.name)
                    var newscope = scope.panel.scope
                    newscope.parentPanel = scope.panel

                    newscope.updateServiceData = function() {
                        //get an updated / fresh servicedata & convert it to json
                        var servicedata = mrl.getService(scope.panel.name)
                        newscope.servicedatajson = JSON.stringify(servicedata, null, 2)
                    }

                    newscope.toggleVirtual = function(virtual) {
                        var service = mrl.getService(scope.panel.name)
                        //service.isVirtual = !service.isVirtual
                        mrl.sendTo(scope.panel.name, 'setVirtual', !service.isVirtual)
                    }

                    newscope.showPeers = function(show) {
                        scope.panel.showPeerTable = show
                    }

                    newscope.saveDefault = function() {
                        mrl.sendTo('runtime', 'saveDefault', scope.panel.displayName, scope.panel.simpleName)
                    }

                    newscope.release = function(peerKey) {
                        mrl.sendTo('runtime', 'release', scope.panel.name)
                    }

                    newscope.startPeer = function(peerKey) {
                        mrl.sendTo(scope.panel.name, 'startPeer', peerKey)
                    }

                    newscope.releasePeer = function(peerKey) {
                        mrl.sendTo(scope.panel.name, 'releasePeer', peerKey)
                    }

                    newscope.save = function() {
                        mrl.sendTo('runtime', 'save', scope.panel.displayName)
                    }

                    newscope.apply = function() {
                        mrl.sendTo(scope.panel.displayName, 'apply')
                        mrl.sendTo(scope.panel.displayName, 'broadcastState')
                    }
                        
                    // making peer service/singleton available in drop down scope
                    newscope.peer = peer

                    var header = $templateCache.get('service/tab-header.html')
                    var content = $templateCache.get(scope.panel.simpleName + 'Gui.html')
                    var footer = $templateCache.get('service/tab-footer.html')
                    elem.html(header + content + footer).show()
                    // not a bad idea, however it led to performance problems when updating the servo gui during movements
                    // when the html was in a hidden state but all the properties where ng-repeated as part of the dom
                    // newscope.properties = mrl.getProperties(newscope.service)
                    $compile(elem.contents())(newscope)

                    // default the id if one was not supplied
                    let tab = null
                    if ($stateParams.servicename && $stateParams.servicename.includes('@')) {
                        tab = $stateParams.servicename
                    } else {
                        tab = $stateParams.servicename + '@' + mrl.getRemoteId()
                    }

                    // if we have an exact match swith to that tab
                    if (newscope.name == tab) {
                        // exact match with url e.g. /python@blah
                        mrl.changeTab(tab)
                    }
                }
            })
        }
    }
}
])
