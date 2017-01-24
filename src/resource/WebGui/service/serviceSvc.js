angular.module('mrlapp.service').service('serviceSvc', ['mrl', '$log', '$http', '$templateCache', '$timeout', '$ocLazyLoad', '$q', function(mrl, $log, $http, $templateCache, $timeout, $ocLazyLoad, $q) {
    var _self = this;
    // all the gui info regarding a panel
    _self.panels = {};
    var ready = false;
    var deferred;
    //check if mrl.js is already connected and wait for it if it is not
    if (!mrl.isConnected()) {
        $log.info('wait for mrl.js to become connected ...');
        var subscribeFunction = function(connected) {
            $log.info('mrl.js seems to be ready now ...', connected, mrl.isConnected());
            if (connected) {
                mrl.unsubscribeConnected(subscribeFunction);
                run();
            }
        };
        $log.info('serviceSvc: mrl.js', mrl);
        mrl.subscribeConnected(subscribeFunction);
    } else {
        run();
    }
    this.isReady = function() {
        return ready;
    }
    ;
    this.waitToBecomeReady = function() {
        $log.info('serviceSvc.waitToBecomeReady()');
        deferred = $q.defer();
        return deferred.promise;
    }
    ;
    var run = function() {
        $log.info('initalizing serviceSvc');
        var services = {};
        var lastPosY = -40;
        //store the y-position of the last added panel
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
        _self.savePanels = function() {
            $log.info("here");
        }
        _self.getPanelsList = function() {
            return Object.keys(_self.panels).map(function(key) {
                return _self.panels[key];
            });
        }
        ;
        var addPanel = function(service) {
            //var panelname = 'main';
            if (_self.panels.hasOwnProperty(service.name)) {
                $log.warn(service.name + ' already has panel');
                return;
            }
            lastPosY += 40;
            var posy = lastPosY;
            //zindex
            var zindex = 1;
            angular.forEach(_self.panels, function(value, key) {
                if (value.zindex > zindex) {
                    zindex = value.zindex;
                }
            });
            zindex = zindex + 1;
            //construct panel & add it to list
            _self.panels[service.name] = {
                simpleName: service.simpleName,
                //serviceType (e.g. Runtime, Python, ...)
                name: service.name,
                //name of the service instance (e.g. runtime, python, rt, pyt, ...)
                templatestatus: service.templatestatus,
                //the state the loading of the template is in (loading, loaded, notfound)
                list: 'main',
                //the list this panel belongs to (e.g. main, min, ...)
                panelname: 'main',
                size: 'free',
                height: 0,
                //the height of this panel
                width: 800,
                // TODO - getPreferredWidth
                posx: 15,
                // TODO - load() posx ?
                //the x-position of this panel
                posy: posy,
                // TODO - load posy
                //the y-position of this panel
                zindex: zindex,
                // TODO - load posy
                //the zindex of this panel
                hide: false //if this panel should be hidden // TODO -load hide...
            };
            service.panels['main'] = _self.panels[service.name];
        };
        _self.addService = function(name, temp) {
            //create a new service and load it's template (and create it's panels)
            //name -> the name of the service instance
            //temp -> the current service state (object)
            services[name] = {
                simpleName: temp.simpleName,
                //serviceType (e.g. Runtime, Python, ...)
                name: temp.name,
                //name of the service instance (e.g. runtime, python, rt, pyt, ...)
                templatestatus: 'loading',
                //the state the loading of the template is in (loading, loaded, notfound)
                panelcount: 1,
                //number of panels this service owns
                //panelnames: null,
                //which panels should show their name?
                //                            panelsizes: null, //what sizes do the panels have
                panels: {},
                //all panels _self service owns
                logLevel: "debug"//FIMXE where is this used & what it is used for?
            };
            //first load & parse the controller,    //js
            //then load and save the template       //html
            $log.info('lazy-loading:', services[name].simpleName);
            $ocLazyLoad.load('service/js/' + services[name].simpleName + 'Gui.js').then(function() {
                $log.info('lazy-loading successful:', services[name].simpleName);
                $http.get('service/views/' + services[name].simpleName + 'Gui.html').then(function(response) {
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
        _self.removeService = function(name) {
            //remove a service and it's panels
            $log.info('removing service', name);
            //remove panels
            for (var panel in services[name].panels) {
                delete panels[name];
            }
            //remove service
            delete services[name];
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
            services[name].scope = scope;
            for (var panel in services[name].panels) {
                _self.panels[name].scope = scope;
            }
        }
        ;
        _self.putPanelZIndexOnTop = function(name, panelname) {
            //panel requests to be put on top of the other panels
            $log.info('putPanelZIndexOnTop', name, panelname);
            var zindex = _self.panels[name].zindex;
            var max = 1;
            angular.forEach(_self.panels, function(value, key) {
                if (value.zindex > max) {
                    max = value.zindex;
                }
                if (value.zindex > zindex) {
                    value.zindex--;
                }
            });
            _self.panels[name].zindex = max;
            angular.forEach(_self.panels, function(value, key) {
                value.notifyZIndexChanged();
            });
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
                     * set the panels location
                     */
        _self.set = function(panelPos) {
            // FIXME - the names should be refactored
            // - could be a root panel, but the 'real' issue
            // is that is heirachial not flat
            // you can express a heirarchy with a flat keys - but its more
            // than just incrementing (GroG)
            var panelName = panelPos.name;
            _self.panels[panelName + '/panel0'].posx = panelPos.x;
            _self.panels[panelName + '/panel0'].posy = panelPos.y;
            _self.panels[panelName + '/panel0'].zindex = panelPos.z;
            //may break some stuff, ..., user is responsible ...
            _self.panels[panelName + '/panel0'].notifyPositionChanged();
            _self.panels[panelName + '/panel0'].notifyZIndexChanged();
            //FIXME All _self will only work on default panel names & only with the first panel ...
        }
        ;
        //TODO maybe consolidate _self two functions into one with boolean switch - like showAll(boolean) ?
        _self.show = function(panelName) {
            // FIXME - the names should be refactored
            // - could be a root panel, but the 'real' issue
            // is that is heirachial not flat
            // you can express a heirarchy with a flat keys - but its more
            // than just incrementing (GroG)
            $timeout(function() {
                _self.panels[panelName + '/panel0'].hide = false;
            });
            //FIXME All _self will only work on default panel names & only with the first panel ...
        }
        ;
        _self.hide = function(panelName) {
            // FIXME - the names should be refactored
            // - could be a root panel, but the 'real' issue
            // is that is heirachial not flat
            // you can express a heirarchy with a flat keys - but its more
            // than just incrementing (GroG)
            $timeout(function() {
                _self.panels[panelName + '/panel0'].hide = true;
            });
            //FIXME All _self will only work on default panel names & only with the first panel ...
        }
        ;
        _self.showAll = function(show) {
            //hide or show all panels
            $log.info('showAll', show);
            angular.forEach(_self.panels, function(value, key) {
                value.hide = !show;
            });
        }
        ;
        //END_ServicePanels
        //add & remove panels for started & stopped services
        _self.onMsg = function(msg) {
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
        mrl.subscribeToService(_self.onMsg, runtime.name);
        //add all existing services
        for (var name in registry) {
            if (registry.hasOwnProperty(name)) {
                _self.addService(name, registry[name]);
            }
        }
        ready = true;
       
    }; // end of function run()
}
]);
