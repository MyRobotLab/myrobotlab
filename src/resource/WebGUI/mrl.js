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
.service('mrl', [function() {

        // The name of the gateway I am
        // currently attached to
        // tried to make these private ..
        // too much of a pain :P
        this.gateway;
        this.runtime;
        this.platform;
        
        var self = this;
        var instances = {};
        var registry = {};
        
        var transport = 'websocket';
        var socket = null;
        var callbacks = [];
        
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
            console.log('Sent msg: ' + msg);
        };

        // enternal onConnect event
        // happens when first connect with backend
        // currently its recieving and processing the
        // Runtime.getLocalServices method - but
        // this might change in the future
        
        var onConnect = function(inData) {
        
        }

        // since framework does not have a hello() onHello() defined
        // protocol - we are using Runtime.onLocalServices to do 
        // initial processing of data after a connect
        this.onLocalServices = function(msg) {
            console.log('onLocalServices:');

            // find the gateway we are talking too
            // TODO make full service?
            // self.gateway = msg.sender;
            var gatewayName = msg.sender;
            self.platform = msg.data[0].platform;

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
                    // do stuff
                    var service = sd[key];
                    console.log("found " + key + " of type " + service.simpleName);
                    registry[key] = {};
                    registry[key] = service;
                    if (service.simpleName == "Runtime"){
                            // the one and only runtime
                            self.runtime = service;
                    }
                }
            }

            self.gateway = sd[gatewayName];
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
                    if (msg.name in nameCallbackMap) {
                        cbs = nameCallbackMap[msg.name];
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
        
        
        this.onClose = function(response) {
            console.log('websocket, onclose');
            if (response.state == "unsubscribe") {
                console.log('Info: ' + transport + ' closed.');
            }
        };
        
        this.onOpen = function(response) {
            console.log('Info: ' + transport + ' connection opened.');
        };

        // initial connect - purpose of this function is to establish
        // a connection with Atmosphere with the Nettophere backend.
        // It creates a request structure and assings mrl service methods
        // to that structure. request object is just a proxy for Atmosphere.
        // Configuration (different proxy) should allow
        this.connect = function(url, proxy) {
            var request = {
                url: url,
                transport: 'websocket',
                enableProtocol: true,
                trackMessageLength: true,
                logLevel: 'debug'
            };
            
            request.onOpen = this.onOpen;
            request.onClose = this.onClose;
            request.onTransportFailure = this.onTransportFailure;
            request.onMessage = this.onMessage;

            // setting up initial callback - this possibly will change
            // when the framework creates a "hello()" method
            this.subscribeToMethod(this.onLocalServices, 'onLocalServices');
            
            socket = $.atmosphere.subscribe(request);
        };
        // --------- ws end ---------------------

        // TODO createMessage
        this.createMessage = function(inName, inMethod, inParams) {
            var msg = {
                msgID: new Date().getTime(),
                name: inName,
                method: inMethod,
                data: inParams
            };
            return msg;
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
            var msg = this.createMessage(name, method, args);
            msg.sendingMethod = 'send';
            // console.log('SendTo:', msg);
            this.sendMessage(msg);
        };

        // IMPORTANT - subscribe subscribes 
        this.subscribe = function(topicName, topicMethod) {
            this.sendTo(InstanceService.getName(), 
            "subscribe", [publisherName, inMethod, 
                outMethod]);
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

    // ws.subscribeToMessages(this.onMessage);
    
    }]);
