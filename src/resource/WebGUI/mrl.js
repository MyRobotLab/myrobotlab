 // http://chariotsolutions.com/blog/post/angularjs-corner-using-promises-q-handle-asynchronous-calls/
// TODO - with massive messsages from serial reading a new channel/websocket/webworker would be nice to have
// TODO - possibly use promises (although I don't see the need at the moment)
// aysynch fun
// http://chariotsolutions.com/blog/post/angularjs-corner-using-promises-q-handle-asynchronous-calls/
// https://strongloop.com/strongblog/promises-in-node-js-with-q-an-alternative-to-callbacks/
// http://embed.plnkr.co/EKvjs3izaQYNCMPuur6R/preview
// http://plnkr.co/edit/EKvjs3izaQYNCMPuur6R?p=preview
// https://docs.angularjs.org/api/ng/service/$q
// http://blog.thoughtram.io/angularjs/2015/01/14/exploring-angular-1.3-speed-up-with-applyAsync.html
// https://strongloop.com/strongblog/promises-in-node-js-with-q-an-alternative-to-callbacks/

// communication begins with a synchronous /api/messages/getLocalServices
// the remote application will need to know 
// #1 what service gateway we are attached too & 
// #2 - runtime name

// TODO - gateways should make a hello() method available through their interface
// infomation returned - gateway name - mrl protokey ? & Runtime name

angular
.module('mrlapp.mrl', [])
//.service('mrl', ['$q', function($q) {
.provider('mrl', [function() {
        var _self = this;


        // The name of the gateway I am
        // currently attached to
        // tried to make these private ..
        // too much of a pain :P
        // FIXME - try again...
        this.gateway;
        this.runtime;
        this.platform;
        
        var connected = false;
        
        var instances = {};
        var registry = {};
        
        var transport = 'websocket';
        var socket = null;
        var callbacks = [];

        // framewok level callbacks
        var onOpenCallbacks = [];
        var onCloseCallbacks = [];
        var onStatus = [];
        
        var deferred = null;

        // connectivity related begins
        // required by AtmosphereJS
        // https://github.com/Atmosphere/atmosphere/wiki/jQuery.atmosphere.js-atmosphere.js-API
        this.request = {
            url: document.location.origin.toString() + '/api/messages',
            transport: 'websocket',
            enableProtocol: true,
            // trackMessageLength: true,
            // maxTextMessageSize: 10000000,
            //maxBinaryMessageSize: 10000000,
            logLevel: 'info'
        };

        // connectivity related end 
        var msgCount = 0;

        // map of service names to callbacks
        var nameCallbackMap = {};
        // map of service types to callbacks
        var typeCallbackMap = {};
        // map of method names to callbacks
        var methodCallbackMap = {};
        
        if (typeof String.prototype.startsWith != 'function') {
            // see below for better implementation!
            String.prototype.startsWith = function(str) {
                return this.indexOf(str) === 0;
            };
        }
        
        function capitalize(string) {
            return string.charAt(0).toUpperCase() + string.slice(1);
        }
        
        this.getCallBackName = function(topicMethod) {
            // replacements
            if (topicMethod.startsWith("publish")) {
                return "on" + capitalize(topicMethod.substring(""))
            
            } else if (topicMethod.startsWith("get")) {
                return "get";
            }

            // no replacement - just pefix and capitalize
            // FIXME - subscribe to onMethod --- gets ---> onOnMethod :P
            return "on%s", capitalize(topicMethod);
        }

        // FIXME name would be subscribeToAllMsgs
        this.subscribeToMessages = function(callback) {
            callbacks.push(callback);
        };

        // FIXME CHECK FOR DUPLICATES
        this.subscribeToService = function(callback, serviceName) {
            if (!(serviceName in nameCallbackMap)) {
                nameCallbackMap[serviceName] = [];
            }
            nameCallbackMap[serviceName].push(callback);
        }
        
        this.subscribeToType = function(callback, typeName) {
            if (!(typeName in typeCallbackMap)) {
                typeCallbackMap[typeName] = [];
            }
            typeCallbackMap[typeName].push(callback);
        }
        
        this.subscribeToMethod = function(callback, methodName) {
            if (!(methodName in methodCallbackMap)) {
                methodCallbackMap[methodName] = [];
            }
            methodCallbackMap[methodName].push(callback);
        }
        
        this.sendMessage = function(msg) {
            var json = jQuery.stringifyJSON(msg);
            this.sendRaw(json);
        };
        
        this.sendRaw = function(msg) {
            socket.push(msg);
            console.log('sendRaw: ' + msg);
        };


        // since framework does not have a hello() onHello() defined
        // protocol - we are using Runtime.onLocalServices to do 
        // initial processing of data after a connect
        this.onLocalServices = function(msg) {
            console.log('onLocalServices:');

            // find the gateway we are talking too
            // TODO make full service?
            // _self.gateway = msg.sender;
            var gatewayName = msg.sender;
            _self.platform = msg.data[0].platform;

            // instances update
            // find the name of the runtime
            // not sure if this is "right" - but this is how
            // it is in Java .. the instance map with a null protokey
            // is the "local" instance - dunno even if javascript supports a null/undefined
            // object member name :P - perhaps it should be the protokey of the gateway instead
            // of null
            instances["null"] = msg.data[0];
            sd = msg.data[0].serviceDirectory;

            // registry update
            for (var key in sd) {
                if (sd.hasOwnProperty(key)) {
                    var service = sd[key];
                    console.log("found " + key + " of type " + service.simpleName);
                    registry[key] = {};
                    registry[key] = service;
                    if (service.simpleName == "Runtime") {
                        // the one and only runtime
                        _self.runtime = service;
                        _self.subscribeToService(_self.onRuntimeMsg, service.name);
                    }
                }
            }
            
            _self.gateway = sd[gatewayName];

            // ok now we are connected
            connected = true;
            
            deferred.resolve('connected !');
            return deferred.promise;
        }

        // keeping the registy up to date with
        // new or removed services
        this.onRuntimeMsg = function(msg) {
            switch (msg.method) {
                case 'onRegistered':
                    var newService = msg.data[0];
                    registry[newService.name] = {};
                    registry[newService.name] = newService;
                    break;
                
                case 'onReleased':
                    var name = msg.data[0];
                    // FIXME - unregister from all callbacks
                    delete registry[name];
                    
                    break;
            }
        }

        // onMessage gets all messaging from the Nettophere server
        // all asynchronous callbacks will be routhed here.  All
        // messages will be in a Message strucutre except for the 
        // Atmosphere heartbeat
        
        this.onMessage = function(response) {
            ++msgCount;
            var body = response.responseBody;
            if (body == 'X') {
                console.log("heartbeat:", body);
            } else {
                var msg;
                try {
                    msg = jQuery.parseJSON(body);

                    // process name callbacks - most common
                    if (msg.sender in nameCallbackMap) {
                        cbs = nameCallbackMap[msg.sender];
                        for (var i = 0; i < cbs.length; i++) {
                            cbs[i](msg);
                        }
                    }

                    // TODO - type based callbacks - rare, except for Runtime

                    // process method callbacks - rare - possible collisions
                    // 'onHandleError' might be worthwhile - mrl managed error
                    if (msg.method in methodCallbackMap) {
                        cbs = methodCallbackMap[msg.method];
                        for (var i = 0; i < cbs.length; i++) {
                            cbs[i](msg);
                        }
                    }
                
                } catch (e) {
                    console.log('Error onMessage: ', e, body);
                    return;
                }
            }
        };
        
        this.onTransportFailure = function(errorMsg, request) {
            jQuery.atmosphere.info(errorMsg);
            if (window.EventSource) {
                request.fallbackTransport = "sse";
            } else {
                request.fallbackTransport = 'long-polling';
            }
            transport = request.fallbackTransport;
            console.log('Error: falling back to ' + transport + ' ' + errorMsg);
        };
        
        this.onStatus = function(status) {
            console.log(status);
        }
        
        this.onClose = function(response) {
            this.connected = false;
            console.log('websocket, onclose');
            if (response.state == "unsubscribe") {
                console.log('Info: ' + transport + ' closed.');
            }
            
            for (var i = 0; i < onCloseCallbacks.length; i++) {
                onCloseCallbacks[i]();
            }
        
        };
        // --------- ws end ---------------------

        // TODO createMessage
        this.createMessage = function(inName, inMethod, inParams) {
            // TODO: consider a different way to pass inParams for a no arg method.
            // rather than an array with a single null element.
            if (inParams.length == 1 && inParams[0] === null) {
                var msg = {
                    msgID: new Date().getTime(),
                    name: inName,
                    method: inMethod
                };
                return msg;
            } else {
                var msg = {
                    msgID: new Date().getTime(),
                    name: inName,
                    method: inMethod,
                    data: inParams
                };
                return msg;
            }
        }
        
        this.isUndefinedOrNull = function(val) {
            return angular.isUndefined(val) || val === null;
        }
        
        this.getService = function(name) {
            if (this.isUndefinedOrNull(registry[name])) {
                return null;
            }
            return registry[name];
        };
        
        this.addService = function(service) {
            registry[service.name] = service;
        };
        
        this.removeService = function(name) {
            delete registry[name];
        };
        
        this.sendTo = function(name, method, data) {
            console.log(arguments[0]);
            var args = Array.prototype.slice.call(arguments, 2);
            var msg = _self.createMessage(name, method, args);
            msg.sendingMethod = 'sendTo';
            // console.log('SendTo:', msg);
            _self.sendMessage(msg);
        };

        // the "real" subscribe - this creates a subscription
        // from the Java topicName service, such that every time the
        // topicMethod is invoked a message comes back to the gateway(webgui),
        // from there it is relayed to the Angular app - and will be sent
        // to all the callbacks which have been registered to it
        this.subscribe = function(topicName, topicMethod) {
            _self.sendTo(_self.gateway.name, "subscribe", topicName, topicMethod);
        };
        
        this.invoke = function(functionName, context) {
            var args = [].slice.call(arguments).splice(2);
            var namespaces = functionName.split(".");
            var func = namespaces.pop();
            for (var i = 0; i < namespaces.length; i++) {
                context = context[namespaces[i]];
            }
            return context[func].apply(this, args);
        }

        // FIXME - lazy loading Angular modules -
        // http://stackoverflow.com/questions/18591966/inject-module-dynamically-only-if-required
        // the following method should dynamically load the
        // Angular module
        // if not loaded &
        // instanciate a new instances with the (service)
        // data
        this.register = function(service) {
            registry[service.name] = {};
            registry[service.name] = service;

            // broadcast callback 
            for (var i = 0; i < callbacks.length; i++) {
                callbacks[i](service);
            }
        }
        
        this.registerForServices = function(callback) {
            this.serviceListeners.push(callback);
        }
        
        this.onOpen = function(response) {
            // connected = true;
            // this.connected = true; mrl.isConnected means data
            // was asked and recieved from the backend
            console.log('onOpen: ' + transport + ' connection opened.');
            for (var i = 0; i < onOpenCallbacks.length; i++) {
                onOpenCallbacks[i]();
            }
        // TODO - chain the onLocalServices / hello with defer.resolve 
        // at the end
        };
        
        this.subscribeOnOpen = function(callback) {
            onOpenCallbacks.push(callback);
        }
        
        this.subscribeOnClose = function(callback) {
            onCloseCallbacks.push(callback);
        }

        // injectables go here
        // the special $get method called when
        // a service gets instantiated for the 1st time?
        // it also represents config's view of the provider
        // when we inject our provider into a function by way of the provider name ("mrl"), Angular will call $get to retrieve the object to inject
        this.$get = function($q, $log) {
            
            this.connect = function(url, proxy) {
                
                if (connected) {
                    $log.info("aleady connected");
                    return this;
                }

                // TODO - use proxy for connectionless testing
                if (url != undefined && url != null) {
                    this.url = url;
                }

                // FIXME - make a hello() protocol !!                       
                // setting up initial callback - this possibly will change
                // when the framework creates a "hello()" method
                this.subscribeToMethod(this.onLocalServices, 'onLocalServices');
                
                socket = $.atmosphere.subscribe(this.request);
                
                deferred = $q.defer();
                deferred.promise.then(function(result) {
                    $log.info("connect deferred - result success");
                    var result = result;
                }, function(error) {
                    $log.error("connect deferred - result error");
                    var error = error;
                });
            
            };
            
            this.onError = function(response) {
                log.error('onError, can not connect');
                deferred.reject('onError, can not connect');
                return deferred.promise;
            };

            // the Angular service interface object
            var service = {
                getGateway: function() {
                    return _self.gateway;
                },
                getLocalServices: function() {
                    return instances["null"];
                },
                getPlatform: function() {
                    return _self.platform;
                },
                getRuntime: function() {
                    return _self.runtime;
                },
                getService: function(name) {
                    return _self.getService(name);
                },
                init: function() {
                    if (connected) {
                        return true;
                    }
                    _self.connect();
                    deferred.promise.then(function(result) {
                        var result = result;
                    }, function(error) {
                        var error = error;
                    });
                    return deferred.promise;
                },
                
                isConnected: function() {
                    return connected;
                },
                getRegistry: function() {
                    return registry;
                },
                sendTo: _self.sendTo,
                subscribe: _self.subscribe,
                subscribeToService: _self.subscribeToService,
                subscribeOnClose: _self.subscribeOnClose,
                subscribeOnOpen: _self.subscribeOnOpen,
                subscribeToMethod: _self.subscribeToMethod,
                promise: _self.promise
            /*,
                save: function() {
                    return $http.post(_self.backendUrl + '/users', 
                    {
                        user: service.user
                    });
                }
                */
            }
            
            return service;
        }

        // assign callbacks
        this.request.onOpen = this.onOpen;
        this.request.onClose = this.onClose;
        this.request.onTransportFailure = this.onTransportFailure;
        this.request.onMessage = this.onMessage;
        this.request.onOpen = this.onOpen;
        this.request.onError = this.onError;
    
    
    }]);
