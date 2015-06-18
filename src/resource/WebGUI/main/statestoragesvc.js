angular.module('mrlapp.main.statestoragesvc', ['mrlapp.mrl'])
        .service('StateStorageSvc', ['mrl', function (mrl) {

                var services = [];

                this.addService = function (name, service) {
                    services[name] = service;
                };

                this.getService = function (name) {
                    if (mrl.isUndefinedOrNull(services[name])) {
                        return null;
                    }
                    return services[name].servic;
                };
            }]);