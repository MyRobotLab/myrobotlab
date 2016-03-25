angular.module('mrlapp.service')
.service('serviceSvc', ['mrl', '$log', '$http', '$templateCache', '$ocLazyLoad', function(mrl, $log, $http, $templateCache, $ocLazyLoad) {
    var _self = this;
    
    var lastPosY;
    
    var gateway = mrl.getGateway();
    var runtime = mrl.getRuntime();
    var platform = mrl.getPlatform();
    var registry = mrl.getRegistry();
    
    var isUndefinedOrNull = function(val) {
        return angular.isUndefined(val) || val === null ;
    }
    ;
    
    //START_update-notification
    //TODO: think of better way
    var updateSubscribtions = [];
    this.subscribeToUpdates = function(callback) {
        updateSubscribtions.push(callback);
    }
    ;
    this.unsubscribeFromUpdates = function(callback) {
        var index = updateSubscribtions.indexOf(callback);
        if (index != -1) {
            updateSubscribtions.splice(index, 1);
        }
    }
    ;
    var notifyAllOfUpdate = function() {
        var panellist = _self.getPanelsList();
        angular.forEach(updateSubscribtions, function(value, key) {
            value(panellist);
        });
    }
    ;
    //END_update-notification
    
    //START_ServicePanels
    var services = {};
    var panels = {};
    
    this.getPanels = function() {
        //return panels as an object
        return panels;
    }
    ;
    
    this.getPanelsList = function() {
        //return panels as an array (thanks GroG!, found this in your code ; ))
        return Object.keys(panels).map(function(key) {
            return panels[key];
        });
    }
    ;
    
    var addPanel = function(service, panelnumber) {
        //creates a new Panel
        //--> for a new service (or)
        //--> another panel for an existing service
        //panelname
        var panelname;
        if (isUndefinedOrNull(service.panelnames) || 
        isUndefinedOrNull(service.panelnames[panelnumber])) {
            panelname = 'panel' + panelnumber.toString();
        } else {
            panelname = service.panelnames[panelnumber];
        }
        //showpanelname
        var showpanelname;
        if (isUndefinedOrNull(service.showpanelnames) || 
        isUndefinedOrNull(service.showpanelnames[panelname])) {
            showpanelname = false;
        } else {
            showpanelname = service.showpanelnames[panelname];
        }
        //panelsize
        var panelsize;
        if (isUndefinedOrNull(service.panelsizes) || 
        isUndefinedOrNull(service.panelsizes[panelname])) {
            //explanation in service/js/_templategui.js
            panelsize = {
                sizes: {
                    //size-options, these will be shown as a option to select from
                    //(and can be applied)
                    tiny: {
                        glyphicon: 'glyphicon glyphicon-minus',
                        //define a glyphicon to show
                        width: 200,
                        //width of this size-setting
                        body: 'collapse',
                        //means that the body-section of the panel won't be shown
                        footer: 'collapse'//don't show footer-section of panel
                    },
                    small: {
                        glyphicon: 'glyphicon glyphicon-resize-small',
                        width: 400
                    },
                    large: {
                        glyphicon: 'glyphicon glyphicon-resize-full',
                        width: 800
                    },
                    full: {
                        glyphicon: 'glyphicon glyphicon-fullscreen',
                        width: 0,
                        fullscreen: true,
                        //show fullscreen (modal)
                        body: 'collapse',
                        footer: 'collapse'
                    },
                    free: {
                        glyphicon: 'glyphicon glyphicon-resize-horizontal',
                        width: 800,
                        freeform: true //allow free-form resizing (width)
                    }
                },
                order: ["free", "full", "large", "small", "tiny"],
                //shows your size-options in this order
                aktsize: 'large'//set this as the start-value
            };
        } else {
            panelsize = service.panelsizes[panelname];
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
        panelsize.oldsize = null ;
        //                    $log.info('serviceSvc-panelsize', panelsize);
        //posy
        //TODO - come back here
        //liked this more, but ...
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
        //... this is working better atm <-WRONG!!! NoWorky in chrome !!!
        //                    lastPosY += 30;
        //                    var posy = lastPosY;
        //zindex
        var zindex = 1;
        angular.forEach(panels, function(value, key) {
            //$log.info("zindex2", key, value.zindex);
            if (value.zindex > zindex) {
                zindex = value.zindex;
            }
        });
        zindex = zindex + 1;
        //construct panel & add it to list
        panels[service.name + '/' + panelname] = {
            simpleName: service.simpleName,
            name: service.name,
            templatestatus: service.templatestatus,
            list: 'main',
            panelname: panelname,
            showpanelname: showpanelname,
            panelsize: panelsize,
            height: 0,
            posx: 15,
            posy: posy,
            zindex: zindex,
            hide: false
        };
        service.panels[panelname] = panels[service.name + '/' + panelname];
    }
    ;
    
    this.addService = function(name, temp) {
        //create a new service and load it's template (and create it's panels)
        services[name] = {
            simpleName: temp.simpleName,
            name: temp.name,
            panelcount: 1,
            panelnames: null ,
            showpanelnames: null ,
            panelsizes: null ,
            panels: {},
            logLevel: "debug"
        };
        $log.info('lazy-loading:', services[name].simpleName);
        $ocLazyLoad.load('service/js/' + services[name].simpleName + 'Gui.js').then(function() {
            $log.info('lazy-loading successful:', services[name].simpleName);
            $http.get('service/views/' + services[name].simpleName + 'Gui.html').
            then(function(response) {
                $templateCache.put(services[name].simpleName + 'Gui.html', response.data);
                services[name].templatestatus = 'loaded';
                addPanel(services[name], 0);
                notifyAllOfUpdate();
            }, function(response) {
                services[name].templatestatus = 'notfound';
                addPanel(services[name], 0);
                notifyAllOfUpdate();
            });
        }, function(e) {
            $log.warn('lazy-loading wasnt successful:', services[name].simpleName);
            services[name].templatestatus = 'notfound';
            addPanel(services[name], 0);
            notifyAllOfUpdate();
        });
    }
    ;
    
    this.removeService = function(name) {
        //remove a service and it's panels
        $log.info('removing service', name);
        for (var panel in services[name].panels) {
            delete panels[name + '/' + panel];
        }
        delete services[name];
        notifyAllOfUpdate();
    }
    ;
    
    this.controllerscope = function(name, scope) {
        $log.info('registering controllers scope', name, scope);
        services[name].scope = scope;
        for (var panel in services[name].panels) {
            panels[name + '/' + panel].scope = scope;
        }
    }
    ;
    
    this.notifyPanelCountChanged = function(name, count) {
        //service want's to change the amount of panels
        var oldcount = services[name].panelcount;
        $log.info('notifyPanelCountChanged', name, oldcount, count);
        if (oldcount != count) {
            services[name].panelcount = count;
            var diff = count - oldcount;
            
            if (diff < 0) {
                for (var i = oldcount - 1; i > count - 1; i++) {
                    var panelname;
                    if (isUndefinedOrNull(services[name].panelnames) || isUndefinedOrNull(services[name].panelnames[i])) {
                        panelname = 'panel' + i.toString();
                    } else {
                        panelname = services[name].panelnames[i];
                    }
                    delete panels[name + '/' + panelname];
                }
            } else if (diff > 0) {
                var serviceexp = services[name];
                for (var i = oldcount; i < count; i++) {
                    addPanel(serviceexp, i);
                }
            }
            notifyAllOfUpdate();
        }
    }
    ;
    
    this.notifyPanelNamesChanged = function(name, names) {
        //service want's to change the panel-names
        $log.info('notifyPanelNamesChanged', name, names);
        var counter = 0;
        for (var panel in services[name].panels) {
            panels[name + '/' + names[counter]] = panels[name + '/' + panel];
            panels[name + '/' + names[counter]].panelname = names[counter];
            counter++;
        }
        services[name].panelnames = names;
    }
    ;
    
    this.notifyPanelShowNamesChanged = function(name, show) {
        //service want's to change which panel-names should be shown
        $log.info('notifyPanelShowNamesChanged', name, show);
        services[name].showpanelnames = show;
        for (var panel in services[name].panels) {
            panels[name + '/' + panel].showpanelname = show[panel];
        }
    }
    ;
    
    this.notifyPanelSizesChanged = function(name, sizes) {
        //service want's to change the size-options of panels
        $log.info('notifyPanelSizesChanged', name, sizes);
        services[name].panelsizes = sizes;
        for (var panel in services[name].panels) {
            panels[name + '/' + panel].panelsize = sizes[panel];
        }
    }
    ;
    
    this.putPanelZIndexOnTop = function(name, panelname) {
        //panel requests to be put on top of the other panels
        $log.info('putPanelZIndexOnTop', name, panelname);
        var zindex = panels[name + '/' + panelname].zindex;
        var max = 1;
        angular.forEach(panels, function(value, key) {
            if (value.zindex > max) {
                max = value.zindex;
            }
            if (value.zindex > zindex) {
                value.zindex--;
            }
        });
        panels[name + '/' + panelname].zindex = max;
        angular.forEach(panels, function(value, key) {
            value.notifyZIndexChanged();
        });
    }
    ;
    
    /**
    * set the panels location
    */
    this.set = function(panelPos) {
        // FIXME - the names should be refactored
        // - could be a root panel, but the 'real' issue
        // is that is heirachial not flat
        // you can express a heirarchy with a flat keys - but its more
        // than just incrementing (GroG)
        panelName = panelPos.name;
        panels[panelName + '/panel0'].posx = panelPos.x;
        panels[panelName + '/panel0'].posy = panelPos.y;
        panels[panelName + '/panel0'].zindex = panelPos.z;
        panels[panelName + '/panel0'].notifyPositionChanged();
    }
    ;
    
    this.movePanelToList = function(name, panelname, list) {
        //move panel to specified list
        $log.info('movePanelToList', name, panelname, list);
        panels[name + '/' + panelname].list = list;
        notifyAllOfUpdate();
    }
    ;
    
    this.show = function(panelName) {
        // FIXME - the names should be refactored
        // - could be a root panel, but the 'real' issue
        // is that is heirachial not flat
        // you can express a heirarchy with a flat keys - but its more
        // than just incrementing (GroG)
        panels[panelName + '/panel0'].hide = false;
        notifyAllOfUpdate();
    }
    ;
    
    this.hide = function(panelName) {
        // FIXME - the names should be refactored
        // - could be a root panel, but the 'real' issue
        // is that is heirachial not flat
        // you can express a heirarchy with a flat keys - but its more
        // than just incrementing (GroG)
        panels[panelName + '/panel0'].hide = true;
        notifyAllOfUpdate();
    }
    ;
    
    this.showAll = function(show) {
        //hide or show all panels
        $log.info('showAll', show);
        angular.forEach(panels, function(value, key) {
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
    }
    ;
    
    //these 3 methods need to be refactored
    //but it is a start in a very good direction
    this.savePanels = function() {
        angular.forEach(panels, function(value, key) {
            _self.savePanel(key);
        });
    }
    ;
    
    //save a panel to the WebGUI - it will keep the object in memory allowing 
    //it to be loaded back into the correct size, position, state, etc
    this.savePanel = function(name) {
        mrl.sendTo(gateway.name, "savePanel", name, getPanel(name));
    }
    ;
    
    //load a panel from the WebGUI
    this.loadPanel = function(name) {
        mrl.sendTo(gateway.name, "loadPanel", getPanel(name));
    }
    ;
    //END_ServicePanels
    
    this.onMsg = function(msg) {
        switch (msg.method) {
        case 'onRegistered':
            var service = msg.data[0];
            _self.addService(service.name, service);
            break;
        
        case 'onReleased':
            $log.info('release service', msg);
            _self.removeService(msg.data[0].name);
            break;
        }
    }
    ;
    mrl.subscribeToService(this.onMsg, runtime.name);
    
    for (var name in registry) {
        if (registry.hasOwnProperty(name)) {
            this.addService(name, registry[name]);
        }
    }
}
]);
