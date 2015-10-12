/*
  responsible for: loading {{serviceType}}controller
  keeps all servicePanel specific infomation (state, position, size, etc)
  can save all this information to the WebGui service

  creates the initial service panels

  registers with Runtime to dynamically add a service panel for any new service and
  remove a panel for any released service

  data: 
*/
angular.module('mrlapp.service')
.service('serviceSvc', ['$log', '$compile', '$rootScope', '$http', 'mrl', function($log, $compile, $rootScope, $http, mrl) {
    $log.info('serviceSvc');
    
    var _self = this;
    var servicePanels = {};
    var zIndexMax = 0;
    
    var isUndefinedOrNull = function(val) {
        return angular.isUndefined(val) || val === null ;
    }
    ;
    
    this.getNextZIndex = function(name) {
        ++zIndexMax;
        servicePanels[name].zIndex = zIndexMax;
        return zIndexMax;
    }
    ;
    
    this.setPosition = function(name, x, y) {
        servicePanels[name].x = x;
        servicePanels[name].y = y;
    }
    ;
    
    this.setDimensions = function(name, width, height) {
        servicePanels[name].width = width;
        servicePanels[name].height = height;
    }
    ;
    
    
    // returns map/object of panels
    // similar to mrl.getRegistry()
    this.getPanels = function() {
        return servicePanels;
    }
    ;
    
    
    // returns list of panels
    // similar to mrl.getServices()
    this.getPanelList = function() {
        var arrayOfPanels = Object.keys(servicePanels).map(function(key) {
            return servicePanels[key]
        }
        );
        return arrayOfPanels;
    }
    ;
    
    this.hideAll = function() {
        for (var name in servicePanels) {
            if (servicePanels.hasOwnProperty(name)) {
                var panel = servicePanels[name];
                panel.setShow(false);
            }
        }
    }
    ;
    
    this.setPosition = function(name, x, y) {
        servicePanels[name].setShow(true);
        servicePanels[name].setPosition(x, y);
    }
    ;
    
    // TODO - method to take name and hide/show
    this.showAll = function() {
        for (var name in servicePanels) {
            if (servicePanels.hasOwnProperty(name)) {
                var panel = servicePanels[name];
                panel.setShow(true);
            }
        }
    }
    ;
    
    this.movePanelToList = function(name, panelname, list) {
        //move panel to specified list
        $log.info('movePanelToList', name, panelname, list);
        var panelindex = -1;
        angular.forEach(servicePanels, function(value, key) {
            if (value.name == name && value.panelname == panelname) {
                panelindex = value.panelindex;
            }
        }
        );
        servicePanels[name].list = list;
        notifyAllOfUpdate();
    }
    ;
    
    this.getServicePanel = function(name) {
        $log.info('serviceSvc.getServicePanel', name);
        if (isUndefinedOrNull(servicePanels[name])) {
            $log.error("could not get panel for ", name);
            return null ;
        }
        return servicePanels[name];
    }
    ;
    
    this.removeServicePanel = function(name) {
        $log.info('serviceSvc.removeServicePanel', name);
        delete servicePanels[name];
    }
    ;
    
    this.onMsg = function(msg) {
        switch (msg.method) {
        case 'onRegistered':
            var newService = msg.data[0];
            _self.addServicePanel(newService.name);
            //_self.addService(newService.name);
            break;
        
        case 'onReleased':
            var service = msg.data[0];
            _self.removeServicePanel(service.name);
            break;
        }
    }
    ;
    
    this.addServicePanel = function(name) {
        $log.info('serviceSvc.addServicePanel', name);
        var service = mrl.getService(name);
        
        if (!isUndefinedOrNull(servicePanels[name])) 
        {
            $log.error(name, " panel already created");
            return;
        }
        
        var service = mrl.getService(name);
        
        
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
                        width: 900
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
                        width: 900,
                        freeform: true //allow free-form resizing (width)
                    }
                },
                order: ["free", "full", "large", "small", "tiny"],
                //shows your size-options in this order
                aktsize: 'large'//set this as the start-value
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
        panelsize.oldsize = null ;
        $log.info('ServiceSvc-panelsize', panelsize);
        //posy
        //TODO - refactor this !!! (and make it work better)
        var panelsarray = _self.getPanelList();
        var posy = 0;
        /* - simply don't do it :)
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
        */
        
        //zindex
        var zindex = 1;
        angular.forEach(servicePanels, function(value, key) {
            //$log.info("zindex2", key, value.zindex);
            if (value.zindex > zindex) {
                zindex = value.zindex;
            }
        }
        );
        zindex = zindex + 1;
        
        
        // creating new PANEL !!!
        var panel = {
            name: name,
            type: service.simpleName,
            simpleName: service.simpleName,
            panelsize: panelsize,
            panelindex: 0,
            x: 0,
            y: 0,
            posx: 0,
            posy: posy,
            width: 0,
            height: 0,
            zindex: zindex
        };
        
        // adding it to our map of panels
        servicePanels[name] = panel;
    
    }
    ;
    
    var addPanel = function(service, panelindex) {
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
                        width: 900
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
                        width: 900,
                        freeform: true //allow free-form resizing (width)
                    }
                },
                order: ["free", "full", "large", "small", "tiny"],
                //shows your size-options in this order
                aktsize: 'large'//set this as the start-value
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
        panels[service.name + '_-_' + panelindex + '_-_'] = {
            simpleName: service.simpleName,
            name: service.name,
            type: service.type,
            list: 'main',
            panelindex: panelindex,
            panelname: panelname,
            showpanelname: showpanelname,
            show:true,
            panelsize: panelsize,
            height: 0,
            posx: 15,
            posy: posy,
            zindex: zindex
        };
    }
    ;
    
    // FIXME - if your interested in putting a "non-service" panel on top - it needs a unique name
    this.putPanelZIndexOnTop = function(name) {
        //panel requests to be put on top of the other panels
        $log.info('putPanelZIndexOnTop', name);
        var zindex = servicePanels[name].zindex;
        var max = 1;
        angular.forEach(servicePanels, function(value, key) {
            if (value.zindex > max) {
                max = value.zindex;
            }
            if (value.zindex > zindex) {
                value.zindex--;
            }
        }
        );
        servicePanels[name].zindex = max;
        angular.forEach(servicePanels, function(value, key) {
            value.notifyZIndexChanged();
        }
        );
    }
    ;
    
    this.savePanels = function() {
        angular.forEach(servicePanels, function(value, key) {
            _self.savePanel(key);
        }
        );
    }
    
    
    /**
    * save a panel to the WebGui - it will keep the object in memory allowing 
    * it to be loaded back into the correct size, position, state, etc
    */
    this.savePanel = function(name) {
        var gateway = mrl.getGateway();
        mrl.sendTo(gateway.name, "savePanel", name, getPanel(name));
    }
    
    /**
    * load a panel from the WebGui
    */
    this.loadPanel = function(name) {
        var gateway = mrl.getGateway();
        mrl.sendTo(gateway.name, "loadPanel", getPanel(name));
    }
    
    
    // why are all services subscribing to runtime?
    mrl.subscribeToService(this.onMsg, mrl.getRuntime().name);
    
    var registry = mrl.getRegistry();
    for (var name in registry) {
        if (registry.hasOwnProperty(name)) {
            this.addServicePanel(name);
        }
    }
}
]);
