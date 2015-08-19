angular.module('mrlapp.service')
.service('ServiceSvc', ['mrl', '$log', '$ocLazyLoad', function(mrl, $log, $ocLazyLoad) {
    var _self = this;
    
    // FIXME - there is no reason to have
    // 2 object data stores - one should be sufficient
    var services = {};
    var panels = {};
    
    var gateway = mrl.getGateway();
    var runtime = mrl.getRuntime();
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
        angular.forEach(updateSubscribtions, function(value, key) {
            value();
        }
        );
    }
    ;
    //END_update-notification
    
    //START_ServicePanels
    
    this.getPanels = function() {
        //return panels as an object
        return panels;
    }
    ;
    
    this.getPanelsList = function() {
        //return panels as an array (thanks GroG!, found this in your code ; ))
        return Object.keys(panels).map(function(key) {
            return panels[key];
        }
        );
    }
    ;

    this.loadNewPanel = function(temp){
        //create a new service and loads it's template
        var name = temp.name;
        services[temp.name] = {
            simpleName: temp.simpleName,
            name: temp.name,
            type: temp.simpleName.toLowerCase(),
            data: {},
            // deprecate ! - data is non descriptive
            //panelcount: 1,
            panelnames: null ,
            showpanelnames: null ,
            panelsizes: null 
        };


        // FIXME - dynamic loading takes time .. and is asynchronous 
        // the "rest" of framework processing needs to be done AFTER the servicegui has processed
        $log.info('lazy-loading:', services[name].type);
        $ocLazyLoad.load("service/js/" + services[name].type + "gui.js").then(function() {
            $log.info('lazy-loading successful:', services[name].type);
            services[name].templatestatus = 'loaded';
            _self.addPanel(services[name]);
            notifyAllOfUpdate();
        }
        , function(e) {
            $log.warn('lazy-loading wasnt successful:', services[name].type);
            services[name].templatestatus = 'notfound';
            _self.addPanel(services[name]);
            notifyAllOfUpdate();
        }
        );
        
    }
    
    this.addPanel = function(temp) {
        var name = temp.name;
        
        // FIXME -- check if existing
        
    
        
        // FIXME - temporary hack - until services can be removed
        var service = services[temp.name];
        
        
        //creates a new Panel
        //--> for a new service (or)
        //--> another panel for an existing service
        //panelname
        var panelname = service.name;
        var showpanelname = true;
        
        //panelsize
        var panelsize;
        if (isUndefinedOrNull(service.panelsizes)) {
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
                        width: 300
                    },
                    large: {
                        glyphicon: 'glyphicon glyphicon-resize-full',
                        width: 500
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
                        width: 500,
                        freeform: true //allow free-form resizing (width)
                    }
                },
                order: ["free", "full", "large", "small", "tiny"],
                //shows your size-options in this order
                aktsize: 'large'//set this as the start-value
            };
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
        angular.forEach(panels, function(value, key) {
            //$log.info("zindex2", key, value.zindex);
            if (value.zindex > zindex) {
                zindex = value.zindex;
            }
        }
        );
        zindex = zindex + 1;
        //construct panel & add it to list
        panels[service.name] = {
            simpleName: service.simpleName,
            name: service.name,
            type: service.type,
            data: service.data,
            templatestatus: service.templatestatus,
            list: 'main',
            //panelindex: 0,
            panelname: panelname,
            showpanelname: showpanelname,
            panelsize: panelsize,
            subPanels: {},
            // TODO - subPanels
            height: 0,
            posx: 15,
            posy: posy,
            zindex: zindex,
            hide: false
        };
        
        // FIXME - why do we have this?
        notifyAllOfUpdate();
    }
    ;
    
    this.removeService = function(name) {
        $log.info('removing service', name, services);
        delete services[name];
        notifyAllOfUpdate();
    }
    ;
    
    this.notifyPanelSizesChanged = function(name, sizes) {
        //service want's to change the size-options of panels
        $log.info('notifyPanelSizesChanged', name, sizes);
        services[name].panelsizes = sizes;
    }
    ;
    
    this.putPanelZIndexOnTop = function(name, panelname) {
        //panel requests to be put on top of the other panels
        $log.info('putPanelZIndexOnTop', name, panelname);
        var index = -1;
        angular.forEach(panels, function(value, key) {
            if (value.name == name && value.panelname == panelname) {
                index = value.index;
            }
        }
        );
        var zindex = panels[name].zindex;
        var max = 1;
        angular.forEach(panels, function(value, key) {
            if (value.zindex > max) {
                max = value.zindex;
            }
            if (value.zindex > zindex) {
                value.zindex--;
            }
        }
        );
        panels[name].zindex = max;
        angular.forEach(panels, function(value, key) {
            value.notifyZIndexChanged();
        }
        );
    }
    ;
    
    this.movePanelToList = function(name, panelname, list) {
        //move panel to specified list
        $log.info('movePanelToList', name, panelname, list);
        var index = -1;
        angular.forEach(panels, function(value, key) {
            if (value.name == name && value.panelname == panelname) {
                index = value.index;
            }
        }
        );
        panels[name].list = list;
        notifyAllOfUpdate();
    }
    ;
    
    this.showAll = function(show) {
        //hide or show all panels
        $log.info('showAll', show);
        angular.forEach(panels, function(value, key) {
            value.hide = !show;
        }
        );
    }
    ;
    
    //these 3 methods need to be refactored
    //but it is a start in a very good direction
    this.savePanels = function() {
        angular.forEach(panels, function(value, key) {
            _self.savePanel(key);
        }
        );
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
            // FIXME - just send name !
            _self.loadNewPanel(service);
            break;
        
        case 'onReleased':
            var service = msg.data[0];
            _self.removeService(service.name);
            break;
        }
    }
    ;
    
    
    // initialization
    mrl.subscribeToService(this.onMsg, runtime.name);
    
    for (var name in registry) {
        if (registry.hasOwnProperty(name)) {
            _self.loadNewPanel(registry[name]);
        }
    }
}
]);
