angular.module('mrlapp.service')
        .service('ServiceSvc', [function () {

                var isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };

                var serviceInstances = [];

                this.addServiceInstance = function (name, service) {
                    serviceInstances[name] = service;
                };

                this.getServiceInstance = function (name) {
                    if (isUndefinedOrNull(serviceInstances[name])) {
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