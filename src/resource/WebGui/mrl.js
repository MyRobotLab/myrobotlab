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
angular.module('mrlapp.mrl', []).provider('mrl', [function() {
    console.log('mrl.js');
    var _self = this;
    // The name of the gateway I am
    // currently attached to
    // tried to make these private ..
    // too much of a pain :P
    // FIXME - try again... {}
    this.gateway;
    this.runtime;
    this.platform;
    var connected = false;
    var environments = {};
    var myEnv = {};
    var registry = {};
    var transport = 'websocket';
    var socket = null;
    var callbacks = [];
    // framewok level callbacks
    var onOpenCallbacks = [];
    var onCloseCallbacks = [];
    var onStatus = [];
    var connectedCallbacks = [];
    var deferred = null;
    var msgInterfaces = {};
    // https://github.com/Atmosphere/atmosphere/wiki/jQuery.atmosphere.js-atmosphere.js-API
    // See the following link for all websocket configuration 
    // https://raw.githubusercontent.com/Atmosphere/atmosphere-javascript/master/modules/javascript/src/main/webapp/javascript/atmosphere.js
    this.request = {
        url: document.location.origin.toString() + '/api/messages',
        transport: 'websocket',
        maxRequest: 100,
        enableProtocol: true,
        fallbackTransport: 'long-polling',
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
    // specific name & method callback 
    // will be used by framework
    var nameMethodCallbackMap = {};
    if (typeof String.prototype.startsWith != 'function') {
        // see below for better implementation!
        String.prototype.startsWith = function(str) {
            return this.indexOf(str) === 0;
        }
        ;
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
        return "on%s",
        capitalize(topicMethod);
    }
    ;
    // FIXME name would be subscribeToAllMsgs
    this.subscribeToMessages = function(callback) {
        callbacks.push(callback);
    }
    ;
    // FIXME CHECK FOR DUPLICATES
    this.subscribeToService = function(callback, serviceName) {
        if (!(serviceName in nameCallbackMap)) {
            nameCallbackMap[serviceName] = [];
        }
        nameCallbackMap[serviceName].push(callback);
    }
    ;
    // NEW !!! - subscribe to a specific instance.method callback
    // will be used by the framework
    this.subscribeToServiceMethod = function(callback, serviceName, methodName) {
        var key = serviceName + "." + _self.getCallBackName(methodName);
        if (!(key in nameMethodCallbackMap)) {
            nameMethodCallbackMap[key] = [];
        }
        nameMethodCallbackMap[key].push(callback);
    }
    ;
    this.subscribeToType = function(callback, typeName) {
        if (!(typeName in typeCallbackMap)) {
            typeCallbackMap[typeName] = [];
        }
        typeCallbackMap[typeName].push(callback);
    }
    ;
    this.subscribeToMethod = function(callback, methodName) {
        if (!(methodName in methodCallbackMap)) {
            methodCallbackMap[methodName] = [];
        }
        methodCallbackMap[methodName].push(callback);
    }
    ;
    this.sendMessage = function(msg) {
        var cleanJsonData = [];
        if (msg.data != null && msg.data.length > 0) {
            // reverse encoding - pop off undefined
            // to shrink paramter length
            // js implementation - 
            var pos = msg.data.length - 1;
            for (i = pos; i > -1; --i) {
                // WTF? - why do this ? - it's a bug for overloaded method
                // ProgramAB.getResponse(null, 'hello') --> resolves to --> ProgramAB.getResponse(null);
                if (typeof msg.data[i] == 'undefined') {// msg.data.pop(); RECENTLY CHANGED 2016-01-21 - popping changes signature !!!! - changing to NOOP
                } else {
                    msg.data[i] = JSON.stringify(msg.data[i]);
                }
            }
        }
        //var json = jQuery.stringifyJSON(msg); <-- from atmosphere
        // now encode the container & contents
        var json = JSON.stringify(msg);
        // <-- native STILL DOES NOT ENCODE QUOTES :P !
        this.sendRaw(json);
    }
    ;
    this.sendRaw = function(msg) {
        socket.push(msg);
        // console.log('sendRaw: ' + msg);
    }
    ;
    // since framework does not have a hello() onHello() defined
    // protocol - we are using Runtime.onLocalServices to do 
    // initial processing of data after a connect
    this.onLocalServices = function(msg) {
        console.log('getEnvironments:');
        // find the gateway we are talking too
        // TODO make full service?
        // _self.gateway = msg.sender;
        var gatewayName = msg.sender;
        _self.environments = msg.data[0];
        _self.myEnv = _self.environments["null"];
        _self.platform = _self.myEnv.platform;
        // environments update
        // find the name of the runtime
        // not sure if this is "right" - but this is how
        // it is in Java .. the instance map with a null protokey
        // is the "local" instance - dunno even if javascript supports a null/undefined
        // object member name :P - perhaps it should be the protokey of the gateway instead
        // of null
        var sd = _self.myEnv.serviceDirectory;
        // registry update
        for (var uri in _self.environments) {
            if (_self.environments.hasOwnProperty(uri)) {
                sd = _self.environments[uri].serviceDirectory;
                for (var key in sd) {
                    if (sd.hasOwnProperty(key)) {
                        var service = sd[key];
                        //console.log("found " + key + " of type " + service.simpleName);
                        registry[key] = {};
                        registry[key] = service;
                        console.log("registry found", key);
                        if (service.simpleName == "Runtime" && uri == "null") {
                            // the one and only local runtime
                            _self.runtime = service;
                            _self.subscribeToService(_self.onRuntimeMsg, service.name);
                        }
                    }
                }
            }
        }
        _self.gateway = _self.myEnv.serviceDirectory[gatewayName];
        // ok now we are connected
        connected = true;
        angular.forEach(connectedCallbacks, function(value, key) {
            value(connected);
        });
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
                // THE CENTER OF ALL CALLBACKS
                // process name callbacks - most common
                // console.log('nameCallbackMap');
                if (nameCallbackMap.hasOwnProperty(msg.sender)) {
                    cbs = nameCallbackMap[msg.sender];
                    for (var i = 0; i < cbs.length; i++) {
                        cbs[i](msg);
                    }
                }
                // serviceName.methodName callback    
                // framework subscribes to (name).onMethodMap to build all
                // underlying structured methods based on Java reflected descriptions        
                // console.log('nameMethodCallbackMap');
                var key = msg.sender + '.' + msg.method;
                if (nameMethodCallbackMap.hasOwnProperty(key)) {
                    cbs = nameMethodCallbackMap[key];
                    for (var i = 0; i < cbs.length; i++) {
                        cbs[i](msg);
                    }
                }
                // TODO - type based callbacks - rare, except for Runtime
                // process method callbacks - rare - possible collisions
                // 'onHandleError' might be worthwhile - mrl managed error
                // console.log('methodCallbackMap');
                if (methodCallbackMap.hasOwnProperty(msg.method)) {
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
    }
    ;
    this.onTransportFailure = function(errorMsg, request) {
        if (window.EventSource) {
            request.fallbackTransport = "sse";
        } else {
            request.fallbackTransport = 'long-polling';
        }
        transport = request.fallbackTransport;
        console.log('Error: falling back to ' + transport + ' ' + errorMsg);
    }
    ;
    this.onStatus = function(status) {
        console.log(status);
    }
    this.onClose = function(response) {
        connected = false;
        console.log('websocket, onclose');
        // I doubt the following is correct or needed
        // just because the connection to the WebGui service fails
        // does not mean callbacks should be removed ...

        if (response.state == "unsubscribe") {
            console.log('Info: ' + transport + ' closed.');
        }
        for (var i = 0; i < onCloseCallbacks.length; i++) {
            onCloseCallbacks[i]();
        }
        angular.forEach(connectedCallbacks, function(value, key) {
            value(connected);
        });
    }
    ;
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
    this.getServicesFromInterface = function(interface) {
        var ret = [];
        for (var name in registry) {
            var service = registry[name];
            // see if a service has the same input interface
            if (!angular.isUndefined(service.interfaceSet[interface])) {
                ret.push(registry[name]);
            }
        }
        return ret;
    }
    this.getService = function(name) {
        if (this.isUndefinedOrNull(registry[name])) {
            return null;
        }
        return registry[name];
    }
    ;
    this.addService = function(service) {
        registry[service.name] = service;
    }
    ;
    this.removeService = function(name) {
        delete registry[name];
    }
    ;
    this.capitalize = function(string) {
        return string.charAt(0).toUpperCase() + string.slice(1);
    }
    this.getCallBackName = function(topicMethod) {
        // replacements
        if (topicMethod.startsWith("publish")) {
            return 'on' + _self.capitalize(topicMethod.substring("publish".length));
        } else if (topicMethod.startsWith("get")) {
            return 'on' + _self.capitalize(topicMethod.substring("get".length));
        }
        // no replacement - just pefix and capitalize
        // FIXME - subscribe to onMethod --- gets ---> onOnMethod :P
        return 'on' + capitalize(topicMethod);
    }
    this.sendTo = function(name, method, data) {
        //console.log(arguments[0]);
        var args = Array.prototype.slice.call(arguments, 2);
        var msg = _self.createMessage(name, method, args);
        msg.sendingMethod = 'sendTo';
        // console.log('SendTo:', msg);
        _self.sendMessage(msg);
    }
    ;
    /*
        The "real" subscribe - this creates a subscription
        from the Java topicName service, such that every time the
        topicMethod is invoked a message comes back to the gateway(webgui),
        from there it is relayed to the Angular app - and will be sent
        to all the callbacks which have been registered to it
        topicName.topicMethod ---> webgui gateway --> angular callback
    */
    this.subscribe = function(topicName, topicMethod) {
        _self.sendTo(_self.gateway.name, "subscribe", topicName, topicMethod);
    }
    ;
    this.unsubscribe = function(topicName, topicMethod) {
        _self.sendTo(_self.gateway.name, "unsubscribe", topicName, topicMethod);
    }
    ;
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
    // instanciate a new environments with the (service)
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
        console.log('mrl.onOpen: ' + transport + ' connection opened.');
        for (var i = 0; i < onOpenCallbacks.length; i++) {
            onOpenCallbacks[i]();
        }
        // TODO - chain the onLocalServices / hello with defer.resolve 
        // at the end
    }
    ;
    var getSimpleName = function(fullname) {
        return ( fullname.substring(fullname.lastIndexOf(".") + 1)) ;
    };
    this.subscribeOnOpen = function(callback) {
        onOpenCallbacks.push(callback);
    }
    ;
    this.unsubscribeOnOpen = function(callback) {
        var index = onOpenCallbacks.indexOf(callback);
        if (index != -1) {
            onOpenCallbacks.splice(index, 1);
        }
    }
    ;
    this.subscribeOnClose = function(callback) {
        onCloseCallbacks.push(callback);
    }
    ;
    this.unsubscribeOnClose = function(callback) {
        var index = onCloseCallbacks.indexOf(callback);
        if (index != -1) {
            onCloseCallbacks.splice(index, 1);
        }
    }
    ;
    this.subscribeConnected = function(callback) {
        connectedCallbacks.push(callback);
    }
    ;
    this.unsubscribeConnected = function(callback) {
        var index = connectedCallbacks.indexOf(callback);
        if (index != -1) {
            connectedCallbacks.splice(index, 1);
        }
    }
    ;
    // injectables go here
    // the special $get method called when
    // a service gets instantiated for the 1st time?
    // it also represents config's view of the provider
    // when we inject our provider into a function by way of the provider name ("mrl"), Angular will call $get to 
    // retrieve the object to inject
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
            // FIXME - optimize and subscribe on {gatewayName}.onLocalService ???
            this.subscribeToMethod(this.onLocalServices, 'onLocalServices');
            socket = atmosphere.subscribe(this.request);
            deferred = $q.defer();
            deferred.promise.then(function(result) {
                $log.info("connect deferred - result success");
                var result = result;
            }, function(error) {
                $log.error("connect deferred - result error");
                var error = error;
            });

            // - not needed ? - just mrl.init() - return deferred.promise; // added to resolve in ui-route
        }
        ;
        this.onError = function(response) {
            $log.error('onError, can not connect');
            deferred.reject('onError, can not connect');
            return deferred.promise;
        }
        ;
        // the Angular service interface object
        var service = {
            getGateway: function() {
                return _self.gateway;
            },
            getLocalServices: function() {
                return environments["null"];
            },
            createMsgInterface: function(name) {
                //TODO - clean up here !!!
                //left kind of a mess here (e.g. temp and mod below), MaVo
                var deferred = $q.defer();
                if (!msgInterfaces.hasOwnProperty(name)) {
                    //console.log(name + ' getMsgInterface ');               
                    msgInterfaces[name] = {
                        "name": name,
                        "temp": {},
                        send: function(method, data) {
                            var args = Array.prototype.slice.call(arguments, 1);
                            var msg = _self.createMessage(name, method, args);
                            msg.sendingMethod = 'sendTo';
                            _self.sendMessage(msg);
                        },
                        /**
                         *   sendArgs will be called by the dynamically generated code interface
                         */
                        sendArgs: function(method, obj) {
                            data = [];
                            for (var key in obj) {
                                if (obj.hasOwnProperty(key)) {
                                    data.push(obj[key]);
                                }
                            }
                            var msg = _self.createMessage(name, method, data);
                            msg.sendingMethod = 'sendTo';
                            _self.sendMessage(msg);
                        },
                        // framework routed callbacks come here
                        onMsg: function(msg) {
                            // webgui.onMethodMap gets processed here
                            // console.log("framework callback " + msg.name + "." + msg.method);
                            switch (msg.method) {
                                // FIXME - bury it ?
                            case 'onState':
                                _self.updateState(msg.data[0]);
                                //                                $scope.$apply(); scope is context related !!!
                                break;
                            case 'onMethodMap':
                                // console.log('onMethodMap Yay !!');
                                // method maps are dynamically created binding functions
                                // created to allow direct access from html views to a msg.{method}
                                // bound to a service 
                                try {
                                    var methodMap = msg.data[0];
                                    for (var method in methodMap) {
                                        if (methodMap.hasOwnProperty(method)) {
                                            var m = methodMap[method];
                                            // do stuff
                                            // $log.info(method);
                                            // build interface method
                                            var dynaFn = "(function (";
                                            var argList = "";
                                            for (i = 0; i < m.parameterTypeNames.length; ++i) {
                                                if (i != 0) {
                                                    argList += ',';
                                                }
                                                argList += "arg" + i;
                                            }
                                            dynaFn += argList + "){";
                                            //dynaFn += "console.log(this);";
                                            /*
                                            if (argList.length > 0) {
                                                dynaFn += "this._interface.send('" + m.name + "'," + argList + ");";
                                            } else {
                                                dynaFn += "this._interface.send('" + m.name + "');";
                                            }
                                            */
                                            dynaFn += "this._interface.sendArgs('" + m.name + "', arguments);";
                                            dynaFn += "})";
                                            //console.log("msg." + m.name + " = " + dynaFn);
                                            msgInterfaces[msg.sender].temp.msg[m.name] = eval(dynaFn);
                                        }
                                    }
                                    msgInterfaces[msg.sender].temp.methodMap = methodMap;
                                } catch (e) {
                                    $log.error("onMethodMap blew up - " + e);
                                }
                                deferred.resolve(msgInterfaces[msg.sender]);
                                break;
                            default:
                                console.log("ERROR - unhandled method " + msg.method);
                                break;
                            }
                            // end switch
                        },
                        subscribe: function(data) {
                            if ((typeof arguments[0]) == "string") {
                                // regular subscribe when used - e.g. msg.subscribe('publishData')
                                /* we could handle var args this way ...

                                var args = Array.prototype.slice.call(arguments, 0);
                                _self.sendTo(_self.gateway.name, "subscribe", name, args);
                                but subscribe is a frozen interface of  either 1 or 4 args
                                */
                                if (arguments.length == 1) {
                                    _self.sendTo(_self.gateway.name, "subscribe", name, arguments[0]);
                                } else if (arguments.length == 4) {
                                    _self.sendTo(_self.gateway.name, "subscribe", name, arguments[0], arguments[1], arguments[2]);
                                }
                            } else {
                                // controller registering for framework subscriptions
                                //                                console.log("here");
                                // expected 'framework' level subscriptions - we should at a minimum
                                // be interested in state and status changes of the services
                                _self.sendTo(_self.gateway.name, "subscribe", name, 'publishStatus');
                                _self.sendTo(_self.gateway.name, "subscribe", name, 'publishState');
                                _self.sendTo(_self.gateway.name, "subscribe", name, 'getMethodMap');
                                _self.sendTo(name, "broadcastState");
                                // below we subscribe to the Angular callbacks - where anything sent
                                // back from the webgui with our service's name on the message - send 
                                // it to our onMsg method
                                var controller = arguments[0];
                                //console.log(this);
                                _self.subscribeToService(controller.onMsg, name);
                                // this is an 'optimization' - rather than subscribing to all service callbacks
                                // framework is only subscribing to one {name}.getMethodMap
                                _self.subscribeToServiceMethod(this.onMsg, name, 'getMethodMap');
                                // TODO - a method subscription who's callback is assigned here
                                // get methodMap
                                msgInterfaces[name].getMethodMap();
                                //                                console.log('here');
                            }
                        },
                        getMethodMap: function() {
                            _self.sendTo(name, "getMethodMap");
                        }
                    };
                }
                // Yikes ! circular reference ...
                // you can do sh*t like this in Js !
                // the point of this is to make a msg interface like
                // structure in the scope similar to the msg interface
                // we created for the controller - they start the same
                msgInterfaces[name].temp.msg = {};
                msgInterfaces[name].temp.msg._interface = msgInterfaces[name];
                //FIXME - hacked here @GroG: please look at this
                _self.sendTo(_self.gateway.name, "subscribe", name, 'getMethodMap');
                _self.subscribeToServiceMethod(msgInterfaces[name].onMsg, name, 'getMethodMap');
                msgInterfaces[name].getMethodMap();
                return deferred.promise;
            },
            getPlatform: function() {
                return _self.platform;
            },
            getPossibleServices: function() {
                var possibleServices = [];
                for (var property in _self.runtime.serviceData.serviceTypes) {
                    if (_self.runtime.serviceData.serviceTypes.hasOwnProperty(property)) {
                        var serviceType = _self.runtime.serviceData.serviceTypes[property];
                        if (serviceType.available) {
                            var model = {};
                            model.name = getSimpleName(property);
                            model.img = model.name + '.png';
                            model.alt = serviceType.description;
                            possibleServices.push(model);
                        }
                    }
                }
                return possibleServices;
            },
            getRuntime: function() {
                return _self.runtime;
            },
            getServicesFromInterface: function(name) {
                return _self.getServicesFromInterface(name);
            },
            getService: function(name) {
                return _self.getService(name);
            },
            updateState: function(service) {
                _self.addService(service);
            },
            init: function() {
                console.log('mrl.init()')
                if (connected) {
                    return true;
                }
                _self.connect();        
                return deferred.promise;
            },
            isConnected: function() {
                return connected;
            },
            isUndefinedOrNull: function(val) {
                return angular.isUndefined(val) || val === null;
            },
            noWorky: function(userId) {
                $log.info('mrl-noWorky', userId);
                _self.sendTo(_self.runtime.name, "noWorky", userId);
            },
            getRegistry: function() {
                return registry;
            },
            getServices: function() {
                var arrayOfServices = Object.keys(registry).map(function(key) {
                    return registry[key]
                });
                return arrayOfServices;
            },
            sendTo: _self.sendTo,
            subscribe: _self.subscribe,
            unsubscribe: _self.unsubscribe,
            subscribeToService: _self.subscribeToService,
            subscribeOnClose: _self.subscribeOnClose,
            unsubscribeOnClose: _self.unsubscribeOnClose,
            subscribeOnOpen: _self.subscribeOnOpen,
            unsubscribeOnOpen: _self.unsubscribeOnOpen,
            subscribeConnected: _self.subscribeConnected,
            unsubscribeConnected: _self.unsubscribeConnected,
            subscribeToMethod: _self.subscribeToMethod,
            subscribeToServiceMethod: _self.subscribeToServiceMethod,
            promise: _self.promise // FIXME - no sql like interface
            // put/get value to and from webgui service            
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
}
]);
