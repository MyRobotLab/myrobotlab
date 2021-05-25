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
            return service.serviceType.peers[key].state == 'started'
        } catch (error) {
            console.error(error);
        }
        return false
    }

    return service;
});
