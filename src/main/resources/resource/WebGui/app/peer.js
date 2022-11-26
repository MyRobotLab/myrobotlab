/**
 * peer util service
 */

console.info('peer')

angular.module('peer', []).service('peer', function( mrl /*$rootScope, $log*/
) {
    service = {};

    var theList = [];

    service.getPeerType = function(service, key) {
        try {
            return service.config.peers[key].type
        } catch (error) {
            console.error(error);
        }
        return null
    }

    service.isPeerStarted = function(service, key) {

        try {
        if (service && service.config && service.config.peers){
            return mrl.getService(service.config.peers[key].name) != null
        }
        } catch (error) {
            console.error(error);
        }
        return false
    }

    service.getActualName = function(service, key) {
        try {
            return service.config.peers[key].name
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
