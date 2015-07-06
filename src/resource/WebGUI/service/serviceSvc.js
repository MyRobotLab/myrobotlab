angular.module('mrlapp.service')
        .service('ServiceSvc', ['mrl', function (mrl) {
                var _self = this;

                var gateway = mrl.getGateway();
                var runtime = mrl.getRuntime();
                var platform = mrl.getPlatform();
                var registry = mrl.getRegistry();

                var isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };

                //START_update-notification
                //TODO: think of better way
                var updateSubscribtions = [];
                this.subscribeToUpdates = function (callback) {
                    updateSubscribtions.push(callback);
                };
                this.unsubscribeFromUpdates = function (callback) {
                    var index = updateSubscribtions.indexOf(callback);
                    if (index != -1) {
                        updateSubscribtions.splice(index, 1);
                    }
                };
                var notifyAllOfUpdate = function () {
                    angular.forEach(updateSubscribtions, function (value, key) {
                        value();
                    });
                };
                //END_update-notification

                //START_Service Instances
                var serviceInstances = [];

                this.addServiceInstance = function (name) {
                    console.log('creating service-instance', name);
                    serviceInstances[name] = {};
                    serviceInstances[name].gui = {};
                    serviceInstances[name].service = mrl.getService(name);
                };

                this.getServiceInstance = function (name) {
                    if (isUndefinedOrNull(serviceInstances[name])) {
                        return null;
                    }
                    return serviceInstances[name];
                };

                this.removeServiceInstance = function (name) {
                    var index = serviceInstances.indexOf(name);
                    if (index != -1) {
                        serviceInstances.splice(index, 1);
                    }
                };
                //END_Service Instances

                //START_Services
                var guiData = {};
                var services = [];

                var addPanel = function (service, panelindex) {
                    var panelname;
                    if (isUndefinedOrNull(service.panelnames) ||
                            isUndefinedOrNull(service.panelnames[panelindex])) {
                        panelname = 'panel' + panelindex.toString();
                    } else {
                        panelname = service.panelnames[panelindex];
                    }
                    var showpanelname;
                    if (isUndefinedOrNull(service.showpanelnames) ||
                            isUndefinedOrNull(service.showpanelnames[panelindex])) {
                        showpanelname = false;
                    } else {
                        showpanelname = service.showpanelnames[panelindex];
                    }
                    var panelsize;
                    if (isUndefinedOrNull(service.panelsizes) ||
                            isUndefinedOrNull(service.panelsizes[panelindex])) {
                        panelsize = {
                            sizes: {
                                tiny: {
                                    glyphicon: 'glyphicon glyphicon-minus',
                                    width: 200,
                                    body: 'collapse'
                                },
                                small: {
                                    glyphicon: 'glyphicon glyphicon-resize-small',
                                    width: 300
                                },
                                large: {
                                    glyphicon: 'glyphicon glyphicon-resize-full',
                                    width: 500
                                },
                                full: {
                                    glyphicon: 'glyphicon glyphicon-fullscreen',
                                    width: 0,
                                    body: 'collapse'
                                }},
                            aktsize: 'large',
                            oldsize: null};
                    } else {
                        panelsize = service.panelsizes[panelindex];
                        if (isUndefinedOrNull(panelsize.aktsize)) {
                            console.log('ERROR_no current size defined');
                        }
                        panelsize.oldsize = null;
                    }
                    console.log('ServiceSvc-panelsize', panelsize);
                    services.push({
                        simpleName: service.simpleName,
                        name: service.name,
                        type: service.type,
                        panelcount: service.panelcount,
                        panelindex: panelindex,
                        panelname: panelname,
                        showpanelname: showpanelname,
                        panelsize: panelsize,
                        forcesize: false
                    });
                };

                this.addService = function (name, temp) {
                    console.log('adding:', name, guiData);
                    if (isUndefinedOrNull(guiData[name])) {
                        console.log('adding#2:', name);
                        guiData[name] = {};
                        guiData[name].simpleName = temp.simpleName;
                        guiData[name].name = temp.name;
                        guiData[name].type = temp.simpleName.toLowerCase();
                        guiData[name].panelcount = 1;
                        guiData[name].panelnames = null;
                        guiData[name].showpanelnames = null;
                        guiData[name].panelsizes = null;

                        var serviceexp = guiData[name];
                        addPanel(serviceexp, 0);
                    }
                    console.log('adding#3:', name, guiData, services);
                    notifyAllOfUpdate();
                };

                this.removeService = function (name) {
                    console.log('removing service', name, guiData);
//                    var panelcount = guiData[name].panelcount;
                    delete guiData[name];
                    var removelist = [];
                    angular.forEach(services, function (value, key) {
                        if (value.name == name) {
                            removelist.push(key);
                        }
                    });
                    angular.forEach(removelist, function (value, key) {
                        services.splice(value, 1);
                    });
//                    for (var i = 0; i < panelcount; i++) {
//                        delete services[name + '_-_' + i + '_-_'];
//                    }
                    notifyAllOfUpdate();
                };

                this.getServices = function () {
                    return services;
                };

                this.notifyPanelCountChanged = function (name, oldcount, count) {
                    console.log('notifyPanelCountChanged', name, oldcount, count);
                    guiData[name].panelcount = count;
                    var diff = count - oldcount;

                    if (diff < 0) {
                        for (var i = oldcount - 1; i > count - 1; i++) {
                            var remove = -1;
                            angular.forEach(services, function (value, key) {
                                if (value.name == name && value.panelindex == i) {
                                    remove = key;
                                }
                            });
                            services.splice(remove, 1);
//                            delete services[name + '_-_' + i + '_-_'];
                        }
                        angular.forEach(services, function (value, key) {
                            if (value.name == name) {
                                value.panelcount = count;
                            }
                        });
//                        for (var i = count - 1; i >= 0; i++) {
//                            services[name + '_-_' + i + '_-_'].panelcount = count;
//                        }
                    } else if (diff > 0) {
                        var serviceexp = guiData[name];
                        for (var i = oldcount; i < count; i++) {
                            addPanel(serviceexp, i);
                        }
                    }
                };

                this.notifyPanelNamesChanged = function (name, names) {
                    console.log('notifyPanelNamesChanged', name, names);
                    guiData[name].panelnames = names;
                    angular.forEach(services, function (value, key) {
                        if (value.name == name) {
                            value.panelname = names[value.panelindex];
                        }
                    });
                };

                this.notifyPanelShowNamesChanged = function (name, show) {
                    console.log('notifyPanelShowNamesChanged', name, show);
                    guiData[name].showpanelnames = show;
                    angular.forEach(services, function (value, key) {
                        if (value.name == name) {
                            value.showpanelname = show[value.panelindex];
                        }
                    });
                };

                this.notifyPanelSizesChanged = function (name, sizes) {
                    console.log('notifyPanelSizesChanged', name, sizes);
                    guiData[name].panelsizes = sizes;
                    angular.forEach(services, function (value, key) {
                        if (value.name == name) {
                            value.panelsize = sizes[value.panelindex];
                        }
                    });
                };
                //END_Services

                this.onMsg = function (msg) {
                    switch (msg.method) {
                        case 'onRegistered':
                            var newService = msg.data[0];
                            _self.addServiceInstance(newService.name);
                            _self.addService(newService.name, newService);
                            break;

                        case 'onReleased':
                            var service = msg.data[0];
                            _self.removeService(service.name);
                            _self.removeServiceInstance(service.name);
                            break;
                    }
                };
                console.log('ServiceSvc-Runtime', runtime, mrl.getRuntime());
                mrl.subscribeToService(this.onMsg, runtime.name);

                for (var name in registry) {
                    if (registry.hasOwnProperty(name)) {
                        this.addServiceInstance(name);
                        this.addService(name, registry[name]);
                    }
                }
            }]);
