angular.module('mrlapp.service')
        .service('ServiceSvc', ['mrl', '$log', function (mrl, $log) {
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
                var serviceData = {};

                this.addServiceData = function (name) {
                    $log.info('creating service-instance', name);
                    serviceData[name] = {};
                };

                this.getServiceData = function (name) {
                    if (isUndefinedOrNull(serviceData[name])) {
                        return null;
                    }
                    return serviceData[name];
                };

                this.removeServiceData = function (name) {
                    delete serviceData[name];
                };
                //END_Service Instances

                //START_Services
                var services = {};
                var panels = {};

                this.getPanels = function () {
                    //return panels as an object
                    return panels;
                };

                this.getPanelsList = function () {
                    //return panels as an array (thanks GroG!, found this in your code ; ))
                    return Object.keys(panels).map(function (key) {
                        return panels[key];
                    });
                };

                var addPanel = function (service, panelindex) {
                    //creates a new Panel
                    //--> for a new service (or)
                    //--> another panel for an existing service
                    //panelname
                    var panelname;
                    if (isUndefinedOrNull(service.panelnames) ||
                            isUndefinedOrNull(service.panelnames[panelindex])) {
                        panelname = 'panel' + panelindex.toString();
                    } else {
                        panelname = service.panelnames[panelindex];
                    }
                    //showpanelname
                    var showpanelname;
                    if (isUndefinedOrNull(service.showpanelnames) ||
                            isUndefinedOrNull(service.showpanelnames[panelindex])) {
                        showpanelname = false;
                    } else {
                        showpanelname = service.showpanelnames[panelindex];
                    }
                    //panelsize
                    var panelsize;
                    if (isUndefinedOrNull(service.panelsizes) ||
                            isUndefinedOrNull(service.panelsizes[panelindex])) {
                        //explanation in service/js/_templategui.js
                        panelsize = {
                            sizes: {
                                //size-options, these will be shown as a option to select from
                                //(and can be applied)
                                tiny: {
                                    glyphicon: 'glyphicon glyphicon-minus', //define a glyphicon to show
                                    width: 200, //width of this size-setting
                                    body: 'collapse', //means that the body-section of the panel won't be shown
                                    footer: 'collapse' //don't show footer-section of panel
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
                                    fullscreen: true, //show fullscreen (modal)
                                    body: 'collapse',
                                    footer: 'collapse'
                                },
                                free: {
                                    glyphicon: 'glyphicon glyphicon-resize-horizontal',
                                    width: 500,
                                    freeform: true //allow free-form resizing (width)
                                }
                            },
                            order: ["free", "full", "large", "small", "tiny"], //shows your size-options in this order
                            aktsize: 'large' //set this as the start-value
                        };
                    } else {
                        panelsize = service.panelsizes[panelindex];
                        if (isUndefinedOrNull(panelsize.aktsize)) {
                            $log.error('ERROR_no current size defined');
                        }
                    }
                    panelsize.sizes['min'] = {
                        glyphicon: 'glyphicon glyphicon-eye-close',
                        width: 200,
                        body: 'collapse',
                        footer: 'collapse',
                        forcesize: true,
                        denyMoving: true
                    };
                    panelsize.order.push('min');
                    panelsize.oldsize = null;
                    $log.info('ServiceSvc-panelsize', panelsize);
                    //posy
                    //TODO - refactor this !!! (and make it work better)
                    var panelsarray = _self.getPanelsList();
                    var posy = 0;
                    for (var i = 0; i < panelsarray.length; i++) {
                        var value = panelsarray[i];
                        var height = 300;
                        var spacing = 30;
                        var comp1 = value.posy;
                        var comp2 = value.posy + value.height;
                        if (posy <= comp1 && posy + height >= comp1) {
                            posy = comp2 + spacing;
                            i = 0;
                        } else if (posy <= comp2 && posy + height >= comp2) {
                            posy = comp2 + spacing;
                            i = 0;
                        } else if (posy >= comp1 && posy <= comp2) {
                            posy = comp2 + spacing;
                            i = 0;
                        } else if (posy + height >= comp1 && posy + height <= comp2) {
                            posy = comp2 + spacing;
                            i = 0;
                        }
                    }
                    //zindex
                    var zindex = 1;
                    angular.forEach(panels, function (value, key) {
                        //$log.info("zindex2", key, value.zindex);
                        if (value.zindex > zindex) {
                            zindex = value.zindex;
                        }
                    });
                    zindex = zindex + 1;
                    //construct panel & add it to list
                    panels[service.name + '/' + panelindex] = {
                        simpleName: service.simpleName,
                        name: service.name,
                        type: service.type,
                        list: 'main',
                        panelindex: panelindex,
                        panelname: panelname,
                        showpanelname: showpanelname,
                        panelsize: panelsize,
                        height: 0,
                        posx: 15,
                        posy: posy,
                        zindex: zindex,
                        hide: false
                    };
                };

                this.addService = function (name, temp) {
                    //create a new service (and a panel of the new service (addPanel))
                    $log.info('adding:', name, services);
                    if (isUndefinedOrNull(services[name])) {
                        $log.info('adding#2:', name);
                        services[name] = {
                            simpleName: temp.simpleName,
                            name: temp.name,
                            type: temp.simpleName.toLowerCase(),
                            panelcount: 1,
                            panelnames: null,
                            showpanelnames: null,
                            panelsizes: null
                        };

                        addPanel(services[name], 0);
                    }
                    $log.info('adding#3:', name, services, panels);
                    notifyAllOfUpdate();
                };

                this.removeService = function (name) {
                    //remove a service and it's panels
                    $log.info('removing service', name, services);
                    var panelcount = services[name].panelcount;
                    delete services[name];
                    for (var i = 0; i < panelcount; i++) {
                        delete panels[name + '/' + i];
                    }
                    notifyAllOfUpdate();
                };

                this.notifyPanelCountChanged = function (name, count) {
                    //service want's to change the amount of panels
                    var oldcount = services[name].panelcount;
                    $log.info('notifyPanelCountChanged', name, oldcount, count);
                    if (oldcount != count) {
                        services[name].panelcount = count;
                        var diff = count - oldcount;

                        if (diff < 0) {
                            for (var i = oldcount - 1; i > count - 1; i++) {
                                delete panels[name + '/' + i];
                            }
                        } else if (diff > 0) {
                            var serviceexp = services[name];
                            for (var i = oldcount; i < count; i++) {
                                addPanel(serviceexp, i);
                            }
                        }
                        notifyAllOfUpdate();
                    }
                };

                this.notifyPanelNamesChanged = function (name, names) {
                    //service want's to change the panel-names
                    $log.info('notifyPanelNamesChanged', name, names);
                    services[name].panelnames = names;
                    var panelcount = services[name].panelcount;
                    for (var i = 0; i < panelcount; i++) {
                        panels[name + '/' + i].panelname = names[i];
                    }
                };

                this.notifyPanelShowNamesChanged = function (name, show) {
                    //service want's to change which panel-names should be shown
                    $log.info('notifyPanelShowNamesChanged', name, show);
                    services[name].showpanelnames = show;
                    var panelcount = services[name].panelcount;
                    for (var i = 0; i < panelcount; i++) {
                        panels[name + '/' + i].showpanelname = show[i];
                    }
                };

                this.notifyPanelSizesChanged = function (name, sizes) {
                    //service want's to change the size-options of panels
                    $log.info('notifyPanelSizesChanged', name, sizes);
                    services[name].panelsizes = sizes;
                    var panelcount = services[name].panelcount;
                    for (var i = 0; i < panelcount; i++) {
                        panels[name + '/' + i].panelsize = sizes[i];
                    }
                };

                this.putPanelZIndexOnTop = function (name, panelname) {
                    //panel requests to be put on top of the other panels
                    $log.info('putPanelZIndexOnTop', name, panelname);
                    var panelindex = -1;
                    angular.forEach(panels, function (value, key) {
                        if (value.name == name && value.panelname == panelname) {
                            panelindex = value.panelindex;
                        }
                    });
                    var zindex = panels[name + '/' + panelindex].zindex;
                    var max = 1;
                    angular.forEach(panels, function (value, key) {
                        if (value.zindex > max) {
                            max = value.zindex;
                        }
                        if (value.zindex > zindex) {
                            value.zindex--;
                        }
                    });
                    panels[name + '/' + panelindex].zindex = max;
                    angular.forEach(panels, function (value, key) {
                        value.notifyZIndexChanged();
                    });
                };

                this.movePanelToList = function (name, panelname, list) {
                    //move panel to specified list
                    $log.info('movePanelToList', name, panelname, list);
                    var panelindex = -1;
                    angular.forEach(panels, function (value, key) {
                        if (value.name == name && value.panelname == panelname) {
                            panelindex = value.panelindex;
                        }
                    });
                    panels[name + '/' + panelindex].list = list;
                    notifyAllOfUpdate();
                };

                this.showAll = function (show) {
                    //hide or show all panels
                    $log.info('showAll', show);
                    angular.forEach(panels, function (value, key) {
                        value.hide = !show;
                    });
//                    //minimize / expand all panels
//                    $log.info('showAll', show);
//                    if (!show) {
//                        //minimize
//                        angular.forEach(panels, function (value, key) {
//                            value.panelsize.oldsize = value.panelsize.aktsize;
//                            value.panelsize.aktsize = 'min';
//                            value.list = 'min';
//                        });
//                    } else {
//                        angular.forEach(panels, function (value, key) {
//                            value.panelsize.aktsize = value.panelsize.oldsize;
//                            value.list = 'main';
//                        });
//                    }
//                    notifyAllOfUpdate();
                };
                //END_Services

                this.onMsg = function (msg) {
                    switch (msg.method) {
                        case 'onRegistered':
                            var service = msg.data[0];
                            _self.addServiceData(service.name);
                            _self.addService(service.name, service);
                            break;

                        case 'onReleased':
                            var service = msg.data[0];
                            _self.removeService(service.name);
                            _self.removeServiceData(service.name);
                            break;
                    }
                };
                $log.info('ServiceSvc-Runtime', runtime, mrl.getRuntime());
                mrl.subscribeToService(this.onMsg, runtime.name);

                for (var name in registry) {
                    if (registry.hasOwnProperty(name)) {
                        this.addServiceData(name);
                        this.addService(name, registry[name]);
                    }
                }
            }]);
