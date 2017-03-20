/**
 * The panelSvc is responsible for maintaining all the display information regarding a 
 * service's panel(s).  This includes but is not limited to x, y, zIndex, hidden, etc..
 * The data itself is in a "Panel" - the data of this functional Panel can be retrieved through
 * getPanelData() - It returns a PanelData object which can be serialized to and from the
 * WebGui service.
 *
 * Its dependencies include the mrl service - which needs to be 'connected' & 'ready'
 *
 */
angular.module('mrlapp.service').service('panelSvc', ['mrl', '$log', '$http', '$templateCache', '$timeout', '$ocLazyLoad', '$q', function(mrl, $log, $http, $templateCache, $timeout, $ocLazyLoad, $q) {
    $log.info('panelSvc');
    var _self = this;
    // object containing all panels
    _self.panels = {};
    // global zIndex
    _self.zIndex = 0;
    var ready = false;
    // TODO - refactor
    var deferred;
    //check if mrl.js is already connected and wait for it if it is not

/*
    if (!mrl.isConnected()) {
        $log.info('wait for mrl.js to become connected ...');
        var subscribeFunction = function(connected) {
            $log.info('mrl.js seems to be ready now ...', connected, mrl.isConnected());
            if (connected) {
                mrl.unsubscribeConnected(subscribeFunction);
                run();
            }
        };
        mrl.subscribeConnected(subscribeFunction);
    } else {
        run();
    }
*/    


    // mrl.waitUntilReady();
    
    this.isReady = function() {
        return ready;
    }
    ;
    this.waitToBecomeReady = function() {
        $log.info('panelSvc.waitToBecomeReady()');
        deferred = $q.defer();
        return deferred.promise;
    }
    ;
    var run = function() {
        $log.info('initalizing panelSvc');
        var lastPosY = -40;
        var gateway = mrl.getGateway();
        var runtime = mrl.getRuntime();
        var platform = mrl.getPlatform();
        var registry = mrl.getRegistry();
        var isUndefinedOrNull = function(val) {
            return angular.isUndefined(val) || val === null;
        };
        //START_update-notification
        //notify all list-displays (e.g. main or min) that a panel was added or removed
        //TODO: think of better way
        //-> not top priority, works quite well
        var updateSubscribtions = [];
        _self.subscribeToUpdates = function(callback) {
            updateSubscribtions.push(callback);
        }
        ;
        _self.unsubscribeFromUpdates = function(callback) {
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
        };
        
        //END_update-notification
        _self.getPanels = function() {
            //return panels as an object
            return _self.panels;
        }
        ;
        // TODO - implement
        _self.savePanels = function() {
            $log.info("here");
        }
        /**
         * panelSvc (PanelData) --to--> MRL
         * saves panel data from panelSvc to MRL
         */
        _self.savePanel = function(name) {
            mrl.sendTo(mrl.getGateway().name, "savePanel", _self.getPanelData(name));
        }
        _self.getPanelsList = function() {
            return Object.keys(_self.panels).map(function(key) {
                return _self.panels[key];
            });
        }
        ;
        var addPanel = function(service) {
            if (_self.panels.hasOwnProperty(service.name)) {
                $log.warn(service.name + ' already has panel');
                return _self.panels[service.name];
            }
            lastPosY += 40;
            var posY = lastPosY;
            _self.zIndex++;
            //construct panel & add it to list
            _self.panels[service.name] = {
                simpleName: service.simpleName,
                //serviceType (e.g. Runtime, Python, ...)
                name: service.name,
                //name of the service instance (e.g. runtime, python, rt, pyt, ...)
                templatestatus: service.templatestatus,
                //the state the loading of the template is in (loading, loaded, notfound)
                list: 'main',
                // ???
                //the list this panel belongs to (e.g. main, min, ...)
                // panelname: 'main',
                // TODO - rename as 'panelType'
                size: 'free',
                // TODO - rename as 'panelType'
                height: 0,
                //the height of this panel
                width: 800,
                // TODO - getPreferredWidth
                posX: 15,
                posY: posY,
                zIndex: _self.zIndex,
                hide: false,
                //if this panel should be hidden // TODO -load hide...
                // a reference to the panelSvc
                svc: _self,
                hide: function() {
                    hide = true;
                }
            };
            return _self.panels[service.name];
        };
        _self.addService = function(service) {
            var name = service.name;
            var type = service.simpleName;
            //first load & parse the controller,    //js
            //then load and save the template       //html
            $log.info('lazy-loading:', type);
            $ocLazyLoad.load('service/js/' + type + 'Gui.js').then(function() {
                $log.info('lazy-loading successful:', type);
                $http.get('service/views/' + type + 'Gui.html').then(function(response) {
                    $templateCache.put(type + 'Gui.html', response.data);                    
                    var newPanel = addPanel(service);
                    newPanel.templatestatus = 'loaded';                                  
                    notifyAllOfUpdate();
                }, function(response) {
                    addPanel(name).templatestatus = 'notfound';
                    notifyAllOfUpdate();
                });
            }, function(e) {
                // http template failure
                type = "No"; // becomes NoGui
                $log.warn('lazy-loading wasnt successful:', type);
                addPanel(name).templatestatus = 'notfound';
                notifyAllOfUpdate();
            });
        }
        ;
        // TODO - releasePanel
        _self.releasePanel = function(name) {
            //remove a service and it's panels
            $log.info('removing service', name);
            //remove panels
            if (name in _self.panels) {
                delete _self.panels[name];
            }
        
            //update !
            notifyAllOfUpdate();
        }
        ;
        // TODO remove it then - if it will be abused ... 
        _self.controllerscope = function(name, scope) {
            //puts a reference to the scope of a service
            //in the service & it's panels
            //WARNING: DO NOT ABUSE THIS !!!
            //->it's needed to bring controller & template together
            //->and should otherwise only be used in VERY SPECIAL cases !!!
            $log.info('registering controllers scope', name, scope);
            if ('scope'in _self.panels[name]) {
                $log.warn('replacing an existing scope for ' + name);
            }
            _self.panels[name].scope = scope;
        }
        ;
        _self.putPanelZIndexOnTop = function(name) {
            //panel requests to be put on top of the other panels
            $log.info('putPanelZIndexOnTop', name);
            _self.zIndex++;
            _self.panels[name].zIndex = _self.zIndex;
            _self.panels[name].notifyZIndexChanged();
        }
        ;
        _self.movePanelToList = function(name, panelname, list) {
            //move panel to specified list
            $log.info('movePanelToList', name, panelname, list);
            _self.panels[name].list = list;
            notifyAllOfUpdate();
        }
        ;
        /**
         * MRL panelData ----to----> UI
         * setPanel takes panelData from a foriegn source
         * and notifies the scope so the gui panels arrange and positioned properly
         */
        _self.setPanel = function(newPanel) {
            
            if (!(newPanel.name in _self.panels)) {
                $log.info('service ' + newPanel.name + ' currently does not exist');
                return;
            }
            
            _self.panels[newPanel.name].name = newPanel.name;
            if (newPanel.simpleName) {
                _self.panels[newPanel.name].simpleName = newPanel.simpleName;
            }
            _self.panels[newPanel.name].posY = newPanel.posY;
            _self.panels[newPanel.name].posX = newPanel.posX;
            _self.panels[newPanel.name].width = newPanel.width;
            _self.panels[newPanel.name].height = newPanel.height;
            _self.zIndex = (newPanel.zIndex > _self.zIndex) ? (newPanel.zIndex + 1) : _self.zIndex;
            _self.panels[newPanel.name].zIndex = newPanel.zIndex;
            _self.panels[newPanel.name].hide = newPanel.hide;
            // data has been updated - now proccess the changes
            _self.panels[newPanel.name].notifyPositionChanged();
            _self.panels[newPanel.name].notifyZIndexChanged();
            _self.panels[newPanel.name].notifySizeChanged();
            notifyAllOfUpdate();
            // <-- WTF is this?
        }
        /**
         * getPanelData - input is a panels name
         * output is a panelData object which which will serialize into a 
         * WebGui's PanelData object - we have to create a data object from the
         * angular "panel" since the angular panels cannot be serialized due to
         * circular references and other contraints
         */
        _self.getPanelData = function(panelName) {
            return {
                "name": _self.panels[panelName].name,
                "simpleName": _self.panels[panelName].simpleName,
                "posX": _self.panels[panelName].posX,
                "posY": _self.panels[panelName].posY,
                "zIndex": _self.panels[panelName].zIndex,
                "width": _self.panels[panelName].width,
                "height": _self.panels[panelName].height,
                "hide": _self.panels[panelName].hide
            };
        }
        ;
        _self.show = function(panelName) {
            _self.panels[panelName].hide = false;
        }
        ;
        _self.hide = function(name) {
            _self.panels[name].hide = true;
            _self.savePanel(name);
        }
        ;
        _self.showAll = function(show) {
            //hide or show all panels
            $log.info('showAll', show);
            angular.forEach(_self.panels, function(value, key) {
                value.hide = !show;
                _self.savePanel(key);
            });
        }
        ;
        //END_ServicePanels
        //add & remove panels for started & stopped services
        _self.onMsg = function(msg) {
            switch (msg.method) {
            case 'onRegistered':
                var service = msg.data[0];
                _self.addService(service);
                break;
            case 'onReleased':
                $log.info('release service', msg);
                _self.releasePanel(msg.data[0].name);
                break;
            }
        }
        ;
        mrl.subscribeToService(_self.onMsg, runtime.name);
        //add all existing services
        for (var name in registry) {
            if (registry.hasOwnProperty(name)) {
                _self.addService(registry[name]);
            }
        }
        ready = true;
    };
    // end of function run()

    run();
   // run();
}
]);