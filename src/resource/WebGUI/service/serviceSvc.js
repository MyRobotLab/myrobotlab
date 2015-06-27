angular.module('mrlapp.service')
        .service('ServiceSvc', ['mrl', function (mrl) {

                var isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };
                
                //START_Service Instances
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
                //END_Service Instances

                //START_Services
                //TODO: find a way to get notified if a service is started / stopped
                var gateway = mrl.getGateway();
                var runtime = mrl.getRuntime();
                var platform = mrl.getPlatform();
                var registry = mrl.getRegistry();

                var guiData = {};
                var services = {};

                for (var name in registry) {
                    if (registry.hasOwnProperty(name)) {
                        var temp = registry[name];
                        guiData[name] = {};
                        guiData[name].simpleName = temp.simpleName;
                        guiData[name].name = temp.name;
                        guiData[name].type = temp.simpleName.toLowerCase();
                        guiData[name].panelcount = 1;

                        var serviceexp = guiData[name];
                        var service = {
                            simpleName: serviceexp.simpleName,
                            name: serviceexp.name,
                            type: serviceexp.type,
                            panelcount: serviceexp.panelcount,
                            panelindex: 0
                        };
                        services[service.name + '_-_' + service.panelindex + '_-_'] = service;
                    }
                }

                this.getServices = function () {
                    return services;
                };

                this.notifyPanelCountChanged = function (name, oldcount, count) {
                    console.log('notifyPanelCountChanged', name, oldcount, count);
                    guiData[name].panelcount = count;
                    var diff = count - oldcount;

                    if (diff < 0) {
//                        service.name+'_-_'+service.panelindex+'_-_'
//                        var index = serviceInstances.indexOf(name);
//                        if (index != -1) {
//                            serviceInstances.splice(index, 1);
//                        }
                        for (var i = oldcount-1; i > count-1; i++) {
                            var index = services.indexOf(name+'_-_'+i+'_-_');
                            if (index != -1) {
                                services.splice(index, 1);
                            }
                        }
                        for (var i = count-1; i >= 0; i++) {
                            services[name+'_-_'+i+'_-_'].panelcount = count;
                        }
                    } else if (diff > 0) {
                        var serviceexp = guiData[name];
                        for (var i = oldcount; i < count; i++) {
                            var service = {
                                simpleName: serviceexp.simpleName,
                                name: serviceexp.name,
                                type: serviceexp.type,
                                panelcount: serviceexp.panelcount,
                                panelindex: i
                            };
                            services[service.name + '_-_' + service.panelindex + '_-_'] = service;
                        }
                    }
                };
                //END_Services
            }]);