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

angular
.module('mrlapp.mrl', [])
.service('mrl', ['$rootScope', function($rootScope) {
        
        var self = this;
        var instances = {};
        var registry = {};
        
        var transport = 'websocket';
        var socket = null;
        var callbacks = [];

        // map of service names to callbacks
        var nameCallbackMap = {};
        // map of service types to callbacks
        var typeCallbackMap = {};
        // map of method names to callbacks
        var methodCallbackMap = {};

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
        
        this.sendMessage = function(message) {
            var json = jQuery.stringifyJSON(message);
            this.sendDirectMessage(json);
        };
        
        this.sendDirectMessage = function(message) {
            socket.push(message);
            console.log('Sent message: ' + message);
        };
        
        this.connect = function(host) {
            var request = {
                url: host,
                transport: 'websocket',
                enableProtocol: true,
                trackMessageLength: true,
                logLevel: 'debug'
            };
            
            request.onOpen = function(response) {
                // Socket open ...
                console.log('Info: ' + transport 
                + ' connection opened.');
            };
            
            request.onClose = function(response) {
                console.log('websocket, onclose');
                if (response.state == "unsubscribe") {
                    console.log('Info: ' + transport 
                    + ' closed.');
                }
            };
            
            request.onTransportFailure = function(errorMsg, request) {
                jQuery.atmosphere.info(errorMsg);
                if (window.EventSource) {
                    request.fallbackTransport = "sse";
                } else {
                    request.fallbackTransport = 'long-polling';
                }
                transport = request.fallbackTransport;
                console.log('Error: falling back to ' + transport + ' ' + errorMsg);
            };
            
            request.onMessage = function(response) {
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

                        // process method callbacks - rare - possible collisions
                        if (msg.method in methodCallbackMap) {
                            cbs = methodCallbackMap[msg.method];
                            for (var i = 0; i < cbs.length; i++) {
                                cbs[i](msg);
                            }
                        }

                        // TODO - type based callbacks - rare, except for Runtime

                    /*
                        console.log('Message:', msg);
                        switch (msg.method) {
                            case 'onLocalServices':
                                // if just connected - got aRuntime.getLocalServices msg response within the msg.data is a Java
                                // ServiceEnvironment, it has Platform info & alllocal runningServicesWe need to load 
                                // all JavaScript types of the Running services and 
                                // create a dictionary of name : --to--> instance ofService typethe following services var needs to be in 
                                // a global objector service - reachable by all 
                                // var services = msg.data[0].serviceDirectory;this.instances["my"] =msg.data[0].serviceDirectory;
                                // lets not do instances yet
                                var sd = msg.data[0].serviceDirectory;
                                for (name in sd) {
                                    var service = sd[name];
                                    self.register(sd[name]);
                                }
                                
                                break;
                            case 'onHandleError':
                                // this is an Error in msg form
                                // - so its a
                                // controlled error
                                // sent by mrl to notify
                                // something has gone wrong in
                                // the backend
                                console
                                .log(
                                'Error onHandleError: ', 
                                msg.data[0]);
                                break;
                        }

                        */
                    
                    } catch (e) {
                        console.log('Error onMessage: ', e, 
                        body);
                        return;
                    }
                /*
                    angular.forEach(callbacks, function(
                    value, key) {
                        value(msg);
                    });
                    */
                }
            };
            
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
            var msg = new Message(name, method, data);
            msg.sendingMethod = 'send';
            // console.log('SendTo:', msg);
            this.sendMessage(msg);
        };
        
        this.subscribeTo = function(publisherName, 
        inMethod, outMethod) {
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

        // broadcast callback angular way
        // $rootScope
        // .$broadcast('Runtime.onLocalServices');
        
        }
        
        this.registerForServices = function(callback) {
            this.serviceListeners.push(callback);
        }

    // ws.subscribeToMessages(this.onMessage);
    
    }]);
