/**
 * peer util service
 */

console.info('peer')

angular.module('peer', []).service('peer', function(/*$rootScope, $log*/
) {
    service = {};

    var theList = [];

    service.getPeerType = function(service, key) {
        try {
            return service.serviceType.peers[key].type
        } catch (error) {
            console.error(error);
        }
        return null
    }

    service.isPeerStarted = function(service, key) {
        try {
            return service.serviceType.peers[key].state == 'STARTED'
        } catch (error) {
            console.error(error);
        }
        return false
    }

    service.getActualName = function(service, key) {
        try {
            return service.serviceType.peers[key].actualName
        } catch (error) {
            console.error(error);
        }
        return null
    }

    service.changePeerTab = function(service, key) {
        try {

            if (!tabsViewCtrl) {
                console.error('tabsViewCtrl is null - cannot changeTab')
            } else {
                tabsViewCtrl.changeTab(service.getActualName(service, key))
            }

        } catch (error) {
            console.error(error);
        }
        return null
    }

    return service;
});
