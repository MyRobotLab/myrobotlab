angular.module('mrlapp.service')
        .service('ServiceSvc', ['mrl', function (mrl) {

                var serviceInstances = [];

                this.addServiceInstance = function (name, service) {
                    serviceInstances[name] = service;
                };

                this.getServiceInstance = function (name) {
                    if (mrl.isUndefinedOrNull(serviceInstances[name])) {
                        return null;
                    }
                    return serviceInstances[name].servic;
                };

                this.removeServiceInstance = function (name) {
                    var index = serviceInstances.indexOf(name);
                    if (index != -1) {
                        serviceInstances.splice(index, 1);
                    }
                };
            }]);