/**
* The initial javascript service created to display MyRobotLab services.
* This webgui should be considered equivalent to a mrl instance - with a single javascript service.
* Its purpose is to display controls and published data from other services mostly from other processes.
* This UI is a reflection of all the services which have been 'registered' with this javascript service.
* The javascript service is effectively named "runtime@webgui-client-1234-5678" - although this will soon changee
* to become more unique
*/

angular.module('mrlapp.mrl', []).provider('mrl', [function() {
    console.log('mrl.js')
    var _self = this

    // FIXME - make all instance variables and make a true singleton - half and half is a mess

    this.generateId = function() {
        // one id to rule them all !
        return 'webgui-client-' + 1234 + '-' + 5678
        /*
        let var1 = ("0000" + Math.floor(Math.random() * 10000)).slice(-4)
        let var2 = ("0000" + Math.floor(Math.random() * 10000)).slice(-4)
        return 'webgui-client-' + var1 + '-' + var2
        */
    }

    // The name of the gateway I am
    // currently attached to
    // tried to make these private ..
    // too much of a pain :P
    // FIXME - try again... {}
    this.gateway

    // FIXME - THIS HAS THE WRONG CONCEPT - PREVIOUSLY IT WAS THE MRL JAVA RUNTIME - IT NEEDS TO BE THE JS RUNTIME !!!
    // FIXME - IT WILL BE OVERWRITTEN ! - THAT NEEDS TO BE FIXED
    this.runtime = {
        name: "runtime",
        id: _self.generateId()
    }

    // add Panel function to be set by PanelSvc for callback ability 
    // in this service to addPanels
    this.addServicePanel = null

    this.id = null

    // FIXME - must be "multiple" remoteIds... - although there is only 1 connected runtime
    this.remoteId = null

    this.blockingKeyList = {}

    // TODO - get 'real' platform info - browser type - node version - etc
    this.platform = {
        os: "chrome",
        lang: "javascript",
        bitness: 64,
        mrlVersion: "unknown"
    }

    var connected = false

    // FIXMME - serviceTypes is 'type' information - and this will be
    // retrieved from the Java server.  It will become the defintion of what
    // possible services we can support - IT SHOULD NOT COME FROM THE JAVA SERVER
    // instead it should be defined by the {service}Gui.js and {service}View.js we have !
    // FIXME - load this with a request NOT from runtime
    // var serviceTypes = []
    var serviceTypes = {}

    var registry = {}
    var ids = {}

    var methodCache = {}
    var transport = 'websocket'
    var socket = null
    var callbacks = []

    var onStatus = []
    var connectedCallbacks = []
    var deferred = null
    var msgInterfaces = {}

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
        // maxBinaryMessageSize: 10000000,
        logLevel: 'info'
    }

    // connectivity related end
    var msgCount = 0

    // FIXME - clean up maps and target vs source runtime
    // msg map of js runtime 
    var jsRuntimeMethodMap = {}
    // map of 'full' service names to callbacks
    var nameCallbackMap = {}
    // map of service types to callbacks
    var typeCallbackMap = {}
    // map of method names to callbacks
    var methodCallbackMap = {}
    // specific name & method callback
    // will be used by framework
    var nameMethodCallbackMap = {}

    var jsRuntimeMethodCallbackMap = {}

    function capitalize(string) {
        return string.charAt(0).toUpperCase() + string.slice(1)
    }

    this.getCallBackName = function(topicMethod) {
        // replacements
        if (topicMethod.startsWith("publish")) {
            return "on" + capitalize(topicMethod.substring(""))
        } else if (topicMethod.startsWith("get")) {
            return "get"
        }
        // no replacement - just pefix and capitalize
        // FIXME - subscribe to onMethod --- gets ---> onOnMethod :P
        return "on%s",
        capitalize(topicMethod)
    }

    // FIXME name would be subscribeToAllMsgs
    this.subscribeToMessages = function(callback) {
        callbacks.push(callback)
    }

    // FIXME CHECK FOR DUPLICATES
    this.subscribeToService = function(callback, inName) {
        let serviceName = _self.getFullName(inName)
        if (!(serviceName in nameCallbackMap)) {
            nameCallbackMap[serviceName] = []
        }
        nameCallbackMap[serviceName].push(callback)
    }

    // NEW !!! - subscribe to a specific instance.method callback
    // will be used by the framework
    this.subscribeToServiceMethod = function(callback, serviceName, methodName) {
        var key = _self.getFullName(serviceName) + "." + _self.getCallBackName(methodName)
        if (!(key in nameMethodCallbackMap)) {
            nameMethodCallbackMap[key] = []
        }
        nameMethodCallbackMap[key].push(callback)
    }

    this.subscribeToType = function(callback, typeName) {
        if (!(typeName in typeCallbackMap)) {
            typeCallbackMap[typeName] = []
        }
        typeCallbackMap[typeName].push(callback)
    }

    this.subscribeToMethod = function(callback, methodName) {
        if (!(methodName in methodCallbackMap)) {
            methodCallbackMap[methodName] = []
        }
        methodCallbackMap[methodName].push(callback)
    }

  
    /**
     * FIXME - use a callback method like addServicePanel
     * registered is called when the subscribed remote runtime.registered is published and this
     * callback is invoked
     */
    this.onReleased = function(msg) {
        var service = msg.data[0]

        // FIXME - unregister from all callbacks
        delete registry[_self.getFullName(service)]
    }

    this.sendBlockingMessage = function(msg) {

        let promise = new Promise(function(resolve, reject) {

            // timer to check at interval
            (function theLoop(msg, i) {
                setTimeout(function() {
                    console.log("blocking timer looking for " + msg.msgId + " in blockingKeyList " + JSON.stringify(msg))
                    // if > 0 ...
                    if (i--) {
                        if (msg.msgId in _self.blockingKeyList) {
                            console.log("found blocking message return")
                            resolve(_self.blockingKeyList[msg.msgId])
                            delete _self.blockingKeyList[msg.msgId]
                        }
                        if (i == 0) {
                            // else error it !
                            console.log("timer exceeded")
                            // TODO - don't throw error - and log error in .then().catch()
                            reject(new Error("timeout for blocking msg " + JSON.stringify(msg)))
                        }
                        // If i > 0, keep going
                        theLoop(msg, i)
                        // Call the loop again
                    }
                }, 1000)
            }
            )(msg, 20)
            // you can add arbitrary amount of parameters
        }
        )

        _self.sendMessage(msg)
        return promise
    }

    this.sendMessage = function(msg) {
        var cleanJsonData = []
        if (msg.data != null && msg.data.length > 0) {
            // reverse encoding - pop off undefined
            // to shrink paramter length
            // js implementation -
            var pos = msg.data.length - 1
            for (i = pos; i > -1; --i) {
                // WTF? - why do this ? - it's a bug for overloaded method
                // ProgramAB.getResponse(null, 'hello') --> resolves to --> ProgramAB.getResponse(null)
                if (typeof msg.data[i] == 'undefined') {// msg.data.pop() RECENTLY CHANGED 2016-01-21 - popping changes signature !!!! - changing to NOOP
                } else {
                    msg.data[i] = JSON.stringify(msg.data[i])
                }
            }
        }

        var json = JSON.stringify(msg)
        _self.sendRaw(json)
    }

    this.setAddServicePanel = function(addPanelFunction) {
        _self.addServicePanel = addPanelFunction
    }

    this.sendRaw = function(msg) {
        socket.push(msg)
    }

    this.onHelloResponse = function(response) {
        console.log('onHelloResponse:')
        console.log(response)
    }

    /**
     *
     */
    this.setServiceTypes = function(msg) {
        const values = Object.values(msg.data[0].serviceTypes)
        for (const v of values) {
            serviceTypes[v.simpleName] = v
        }
        deferred.resolve('connected !')
    }

    this.onRegistered = function(msg) {
        
        let registration = msg.data[0]
        let fullname = registration.name + '@' + registration.id
        console.log("onRegistered " + fullname)
  
        let simpleTypeName = getSimpleName(registration.typeKey)

        // FIXME - what the hell its expecting a img - is this needed ????
        registration['img'] = simpleTypeName + '.png'
        // model.alt = serviceType.description - do not have that info at the moment                                                       
        serviceTypes[simpleTypeName] = registration.type

        // initial de-serialization of state
        let service = JSON.parse(registration.state)
        registry[fullname] = service

        // now use the panelSvc to add a panel - with the function it registered
        _self.addServicePanel(service)
    }

    /**
     * The first message recieved from remote webgui process.  After a connection is negotiated, a "hello" msg is
     * sent by both processes. The processes then respond to each other and begin the dialog of asking for details of
     * services and continuing additional setup.  The hello has a summarized list of services which only include name,
     * and type. If this process can provide a panelSvc UI panel it will wait until the foreign process sends a onRegistered
     * with details of state info
     */
    this.getHelloResponse = function(request) {
        console.log('getHelloResponse:')
        let hello = JSON.parse(request.data[1])

        // FIXME - remove this - there aren't 1 remoteId there are many !
        _self.remoteId = hello.id

        // once we have our mrl instance's id - we are
        // ready to ask more questions

        // FIXME - git list of current services (name and types)
        // FIXME - iterate through list request getService on each -or publishState
        _self.platform.mrlVersion = hello.platform.mrlVersion
        // FIXME - Wrong !  - multiple platforms !

        // suscribe to future registration requests
        
        let fullname = 'runtime@' + hello.id
        jsRuntimeMethodCallbackMap[fullname + '.onRegistered'] = _self.onRegistered
        jsRuntimeMethodCallbackMap[fullname + '.onReleased'] = _self.onReleased

        _self.subscribe(fullname, 'registered')
        _self.subscribe(fullname, 'released')

        // FIXME - move this to onConnect
        connected = true

        // FIXME - remove the over-complicated promise
        deferred.resolve('connected !')
    }

    this.getLocalName = function(fullname) {
        let str = fullname
        var n = str.indexOf("@")
        if (n > 0) {
            str = str.substring(0, n)
        }
        return str
    }

    // onMessage gets all messaging from the Nettophere server
    // all asynchronous callbacks will be routed here.  All
    // messages will be in a Message strucutre except for the
    // Atmosphere heartbeat
    this.onMessage = function(response) {
        ++msgCount
        var body = response.responseBody
        if (body == 'X') {
            console.log("heartbeat:", body)
        } else {
            var msg
            try {
                msg = jQuery.parseJSON(body)

                if (msg == null) {
                    console.log('msg null')
                    return
                }

                //console.log('--> ' + msg.msgId + ' ' + msg.msgType)
                console.log('---> ' + msg.msgId + ' ' + msg.name + '.' + msg.method)

                // handle blocking 'R'eturn msgs here - FIXME - timer to clean old errored msg ?
                // the blocking call removes any msg resolved
                if (msg.msgType == 'R') {
                    _self.blockingKeyList[msg.msgId] = msg
                }

        
                // TODO - msg "to" the jsRuntime .. TODO - all all methods of this class
                // HIDDEN single javascript service -> runtime@webgui-client-1234-5678
                // handles all delegation of incoming msgs and initial registrations
                // e.g. : runtime@remote-robot.onRegistered
                // FIXME - this is wrong - its handling callbacks from the connected Runtime - there can be
                // multiple connected runtimes.  This "should" handle all msg.name == js runtime (not sender)
                key = msg.sender + '.' + msg.method
                if (jsRuntimeMethodCallbackMap.hasOwnProperty(key)) {
                    let cbs = jsRuntimeMethodCallbackMap[key]
                    cbs(msg)
                }

                // THE CENTER OF ALL CALLBACKS
                // process name callbacks - most common
                // console.log('nameCallbackMap')
                let senderFullName = _self.getFullName(msg.sender)
                if (nameCallbackMap.hasOwnProperty(senderFullName) && msg.method != 'onMethodMap') {
                    let cbs = nameCallbackMap[senderFullName];
                    for (var i = 0; i < cbs.length; i++) {
                        cbs[i](msg)
                    }
                }
                // serviceName.methodName callback
                // framework subscribes to (name).onMethodMap to build all
                // underlying structured methods based on Java reflected descriptions
                // console.log('nameMethodCallbackMap')
                key = _self.getFullName(msg.sender) + '.' + msg.method
                if (nameMethodCallbackMap.hasOwnProperty(key)) {
                    let cbs = nameMethodCallbackMap[key]
                    for (var i = 0; i < cbs.length; i++) {
                        cbs[i](msg)
                    }
                }
                // TODO - type based callbacks - rare, except for Runtime
                // process method callbacks - rare - possible collisions
                // 'onHandleError' might be worthwhile - mrl managed error
                // console.log('methodCallbackMap');
                if (methodCallbackMap.hasOwnProperty(msg.method)) {
                    let cbs = methodCallbackMap[msg.method]
                    for (var i = 0; i < cbs.length; i++) {
                        cbs[i](msg)
                    }
                }
            } catch (e) {
                console.log('Error onMessage: ', e, body)
                return
            }
        }
    }

    this.onTransportFailure = function(errorMsg, request) {
        if (window.EventSource) {
            request.fallbackTransport = "sse"
        } else {
            request.fallbackTransport = 'long-polling'
        }
        transport = request.fallbackTransport
        console.log('Error: falling back to ' + transport + ' ' + errorMsg)
    }

    this.onStatus = function(status) {
        console.log(status)
    }

    this.onClose = function(response) {
        connected = false
        console.log('websocket, onclose')
        // I doubt the following is correct or needed
        // just because the connection to the WebGui service fails
        // does not mean callbacks should be removed ...

        if (response.state == "unsubscribe") {
            console.log('Info: ' + transport + ' closed.')
        }

        angular.forEach(connectedCallbacks, function(value, key) {
            value(connected)
        })
    }

    this.getShortName = function(name) {
        if (name.includes('@')) {
            name.substring(0, name.indexOf("@"))
        } else {
            return name
        }
    }

    this.getFullName = function(service) {

        if ((typeof service) == "string") {
            if (service.includes('@')) {
                return service
            } else {
                // is string - and is short name - check registry first
                if (_self.remoteId != null) {
                    console.error('string supplied name did not have remoteId - this will be a problem !')
                    return service + '@' + _self.remoteId
                } else {
                    return service
                }
            }
        } else {

            if (service.name.includes('@')) {
                return service.name
            } else {
                return service.name + '@' + service.id
            }
        }
    }

    // --------- ws end ---------------------
    // TODO createMessage
    this.createMessage = function(inName, inMethod, inParams) {
        // TODO: consider a different way to pass inParams for a no arg method.
        // rather than an array with a single null element.
        let rSuffix = (_self.remoteId == null || inName.includes('@')) ? "" : "@" + _self.remoteId

        var msg = {
            msgId: new Date().getTime(),
            name: _self.getFullName(inName),
            method: inMethod,
            sender: "runtime@" + _self.id,
            sendingMethod: null
        }

        if (inParams == null || (inParams.length == 1 && inParams[0] === null)) {

            return msg
        } else {
            msg["data"] = inParams
            return msg
        }
    }

    this.isUndefinedOrNull = function(val) {
        return angular.isUndefined(val) || val === null
    }

    this.getServicesFromInterface = function(interface) {
        var ret = []
        for (var name in registry) {
            var service = registry[name]
            // see if a service has the same input interface
            if (!angular.isUndefined(service.interfaceSet) && !angular.isUndefined(service.interfaceSet[interface])) {
                ret.push(registry[name])
            }
        }
        return ret
    }

    this.getService = function(name) {

        if (this.isUndefinedOrNull(registry[_self.getFullName(name)])) {
            return null
        }
        return registry[_self.getFullName(name)]
    }

    this.addService = function(service) {
        registry[_self.getFullName(service.name)] = service
    }

    this.removeService = function(name) {
        delete registry[_self.getFullName(name)]
    }

    this.capitalize = function(string) {
        return string.charAt(0).toUpperCase() + string.slice(1)
    }

    this.getCallBackName = function(topicMethod) {
        // replacements
        if (topicMethod.startsWith("publish")) {
            return 'on' + _self.capitalize(topicMethod.substring("publish".length))
        } else if (topicMethod.startsWith("get")) {
            return 'on' + _self.capitalize(topicMethod.substring("get".length))
        }
        // no replacement - just pefix and capitalize
        // FIXME - subscribe to onMethod --- gets ---> onOnMethod :P
        return 'on' + capitalize(topicMethod)
    }
    this.sendTo = function(name, method, data) {
        var args = Array.prototype.slice.call(arguments, 2)
        var msg = _self.createMessage(name, method, args)
        msg.sendingMethod = 'sendTo'
        _self.sendMessage(msg)
    }

    this.sendToBlocking = function(name, method, data) {
        var args = Array.prototype.slice.call(arguments, 2)
        var msg = _self.createMessage(name, method, args)
        msg.msgType = "B"
        if (msg.sendingMethod == null) {
            msg.sendingMethod = 'sendToBlocking'
        }
        // console.log('SendTo:', msg)
        _self.sendBlockingMessage(msg)
    }

    /*
        FIXME - these are probably add and remove listeners for the js runtime service - NOT NEEDED
        - add the callbacks - remove these methods

        The "real" subscribe - this creates a subscription
        from the Java topicName service, such that every time the
        topicMethod is invoked a message comes back to the gateway(webgui),
        from there it is relayed to the Angular app - and will be sent
        to all the callbacks which have been registered to it
        topicName.topicMethod ---> webgui gateway --> angular callback
    */
    this.subscribe = function(name, method) {
        _self.sendTo(name, "addListener", method, 'runtime@' + _self.id)
    }

    this.unsubscribe = function(name, method) {
        _self.sendTo(name, "removeListener", method, 'runtime@' + _self.id)
    }

    this.invoke = function(functionName, context) {
        var args = [].slice.call(arguments).splice(2)
        var namespaces = functionName.split(".")
        var func = namespaces.pop()
        for (var i = 0; i < namespaces.length; i++) {
            context = context[namespaces[i]]
        }
        return context[func].apply(this, args)
    }
    // FIXME - lazy loading Angular modules -
    // http://stackoverflow.com/questions/18591966/inject-module-dynamically-only-if-required
    // the following method should dynamically load the
    // Angular module
    // if not loaded &
    // instanciate a new environments with the (service)
    // data
    this.register = function(service) {
        let fullname = service.name + '@' + service.id
        registry[fullname] = {}
        registry[fullname] = service
        // broadcast callback
        for (var i = 0; i < callbacks.length; i++) {
            callbacks[i](service)
        }
    }
    this.registerForServices = function(callback) {
        this.serviceListeners.push(callback)
    }
    this.onOpen = function(response) {
        // connected = true
        // this.connected = true mrl.isConnected means data
        // was asked and recieved from the backend
        console.log('mrl.onOpen: ' + transport + ' connection opened.')

        let hello = {
            id: _self.id,
            uuid: _self.uuid,
            platform: _self.platform
        }

        // blocking in the sense it will take the return data switch sender & destination - place an 'R'
        // and effectively return to sender without a subscription
        // _self.sendToBlocking('runtime', "getHelloResponse", "fill-uuid", hello)
        // FIXME - this is not full address - but its being sent to a remote runtime :()
        _self.sendTo('runtime', "getHelloResponse", "fill-uuid", hello)

    }

    var getSimpleName = function(fullname) {
        return (fullname.substring(fullname.lastIndexOf(".") + 1))
    }

    this.subscribeConnected = function(callback) {
        connectedCallbacks.push(callback)
    }

    // injectables go here
    // the special $get method called when
    // a service gets instantiated for the 1st time?
    // it also represents config's view of the provider
    // when we inject our provider into a function by way of the provider name ("mrl"), Angular will call $get to
    // retrieve the object to inject
    this.$get = function($q, $log, $http) {
        this.connect = function(url, proxy) {
            if (connected) {
                $log.info("aleady connected")
                return this
            }
            // TODO - use proxy for connectionless testing
            if (url != undefined && url != null) {
                this.url = url
            }

            socket = atmosphere.subscribe(this.request)
            deferred = $q.defer()
            deferred.promise.then(function(result) {
                $log.info("connect deferred - result success")
                var result = result
            }, function(error) {
                $log.error("connect deferred - result error")
                var error = error
            })

            // - not needed ? - just mrl.init() - return deferred.promise // added to resolve in ui-route
        }

        this.onError = function(response) {
            $log.error('onError, can not connect')
            deferred.reject('onError, can not connect')
            return deferred.promise
        }

        // the Angular service interface object
        var service = {
            getGateway: function() {
                return _self.gateway
            },
            /** 
             * creating a message interface unique for each service, and dynamically building out methods
             * based on information from getMessageMap
             */
            createMsgInterface: function(name) {
                //TODO - clean up here !!!
                //left kind of a mess here (e.g. temp and mod below), MaVo
                var deferred = $q.defer()
                if (!msgInterfaces.hasOwnProperty(name)) {
                    //console.log(name + ' getMsgInterface ')
                    msgInterfaces[name] = {
                        "name": name,
                        "temp": {},
                        send: function(method, data) {
                            var args = Array.prototype.slice.call(arguments, 1)
                            var msg = _self.createMessage(name, method, args)
                            msg.sendingMethod = 'sendTo'
                            // FIXME - not very useful
                            _self.sendMessage(msg)
                        },
                        sendBlocking: function(method, data) {
                            var args = Array.prototype.slice.call(arguments, 1)
                            var msg = _self.createMessage(name, method, args)
                            msg.sendingMethod = 'sendTo'
                            msg.msgType = 'B'
                            // FIXME - not very useful
                            _self.sendMessage(msg)
                        },
                        sendTo: function(toName, method, data) {
                            var args = Array.prototype.slice.call(arguments, 2)
                            var msg = _self.createMessage(toName, method, args)
                            msg.sendingMethod = 'sendTo'
                            _self.sendMessage(msg)
                        },
                        /**
                         *   sendArgs will be called by the dynamically generated code interface
                         */
                        sendArgs: function(method, obj) {
                            data = []
                            for (var key in obj) {
                                if (obj.hasOwnProperty(key)) {
                                    data.push(obj[key])
                                }
                            }
                            var msg = _self.createMessage(name, method, data)
                            msg.sendingMethod = 'sendTo'
                            // FIXME - not very useful
                            _self.sendMessage(msg)
                        },
                        // framework routed callbacks come here
                        onMsg: function(msg) {
                            // webgui.onMethodMap gets processed here
                            // console.log("framework callback " + msg.name + "." + msg.method)
                            switch (msg.method) {
                                // FIXME - bury it ?
                            case 'onState':
                                _self.updateState(msg.data[0])
                                //                                $scope.$apply() scope is context related !!!
                                break
                            case 'onMethodMap':
                                // console.log('onMethodMap Yay !!')
                                // method maps are dynamically created binding functions
                                // created to allow direct access from html views to a msg.{method}
                                // bound to a service
                                try {

                                    var methodMap = msg.data[0]
                                    for (var method in methodMap) {
                                        if (methodMap.hasOwnProperty(method)) {
                                            var m = methodMap[method]
                                            // do stuff
                                            // $log.info(method)
                                            // build interface method
                                            var dynaFn = "(function ("
                                            var argList = ""
                                            for (i = 0; i < m.parameterTypeNames.length; ++i) {
                                                if (i != 0) {
                                                    argList += ','
                                                }
                                                argList += "arg" + i
                                            }
                                            dynaFn += argList + "){"
                                            //dynaFn += "console.log(this)"
                                            /*
                                            if (argList.length > 0) {
                                                dynaFn += "this._interface.send('" + m.name + "'," + argList + ")"
                                            } else {
                                                dynaFn += "this._interface.send('" + m.name + "')"
                                            }
                                            */
                                            dynaFn += "this._interface.sendArgs('" + m.name + "', arguments);";
                                            dynaFn += "})"
                                            //console.log("msg." + m.name + " = " + dynaFn)
                                            msgInterfaces[msg.sender].temp.msg[m.name] = eval(dynaFn)
                                        }
                                    }
                                    msgInterfaces[msg.sender].temp.methodMap = methodMap
                                } catch (e) {
                                    $log.error("onMethodMap blew up - " + e)
                                }
                                deferred.resolve(msgInterfaces[msg.sender])
                                break
                            default:
                                console.log("ERROR - unhandled method " + msg.method)
                                break
                            }
                            // end switch
                        },
                        subscribeToMethod: function(callback, methodName) {
                            _self.subscribeToMethod(callback, methodName)
                        },
                        subscribeTo: function(controller, serviceName, methodName) {
                            _self.subscribeToServiceMethod(controller.onMsg, serviceName, methodName)
                        },
                        unsubscribe: function(data) {
                            if ((typeof arguments[0]) == "string") {
                                // only handle string argument
                                if (arguments.length != 1) {
                                    console.log("ERROR - unsubscribe expecting 1 arg got " + arguments.length)
                                    return
                                }
                                _self.sendTo(name, "removeListener", arguments[0], 'runtime@' + _self.id)
                            } else {
                                console.error("ERROR - unsubscribe non string arg")
                            }
                        },

                        subscribe: function(data) {
                            if ((typeof arguments[0]) == "string") {
                                // regular subscribe when used - e.g. msg.subscribe('publishData')
                                if (arguments.length != 1) {
                                    console.log("subscribe(string) expecting single argument!")
                                    return
                                }
                                _self.sendTo(name, "addListener", arguments[0], 'runtime@' + _self.id)
                            } else {
                                // controller registering for framework subscriptions
                                //                                console.log("here")
                                // expected 'framework' level subscriptions - we should at a minimum
                                // be interested in state and status changes of the services
                                _self.sendTo(name, "addListener", "publishStatus", 'runtime@' + _self.id)
                                _self.sendTo(name, "addListener", "publishState", 'runtime@' + _self.id)
                                _self.sendTo(name, "addListener", "getMethodMap", 'runtime@' + _self.id)

                                _self.sendTo(name, "broadcastState")
                                // below we subscribe to the Angular callbacks - where anything sent
                                // back from the webgui with our service's name on the message - send
                                // it to our onMsg method
                                var controller = arguments[0]
                                //console.log(this)
                                _self.subscribeToService(controller.onMsg, name)
                                // this is an 'optimization' - rather than subscribing to all service callbacks
                                // framework is only subscribing to one {name}.getMethodMap
                                _self.subscribeToServiceMethod(this.onMsg, name, 'getMethodMap')
                                // TODO - a method subscription who's callback is assigned here
                                // get methodMap
                                msgInterfaces[name].getMethodMap()
                                //                                console.log('here')
                            }
                        },
                        getMethodMap: function() {
                            _self.sendTo(name, "getMethodMap")
                        }
                    }
                }
                // Yikes ! circular reference ...
                // you can do sh*t like this in Js !
                // the point of this is to make a msg interface like
                // structure in the scope similar to the msg interface
                // we created for the controller - they start the same
                msgInterfaces[name].temp.msg = {}
                msgInterfaces[name].temp.msg._interface = msgInterfaces[name]
                //FIXME - hacked here @GroG: please look at this
                // _self.sendTo(_self.gateway.name, "subscribe", name, 'getMethodMap')
                _self.sendTo(name, "addListener", "getMethodMap", 'runtime@' + _self.id)
                // - name + '@' + _self.id)
                _self.subscribeToServiceMethod(msgInterfaces[name].onMsg, name, 'getMethodMap')
                msgInterfaces[name].getMethodMap()
                return deferred.promise
            },
            getPlatform: function() {
                return _self.platform
            },
            getRemoteId: function() {
                return _self.remoteId
            },
            getId: function() {
                return _self.id
            },
            getPossibleServices: function() {
                return serviceTypes;
            },
            getRuntime: function() {
                // FIXME - this is wrong mrl.js is a js runtime service - this is just the runtime its connected to
                return _self.runtime
            },
            getServicesFromInterface: function(name) {
                return _self.getServicesFromInterface(name)
            },
            getService: function(name) {
                return _self.getService(name)
            },
            updateState: function(service) {
                _self.addService(service)
            },
            init: function() {
                console.log('mrl.init()')
                if (connected) {
                    return true
                }
                _self.connect()
                return deferred.promise
            },
            isConnected: function() {
                return connected
            },
            isUndefinedOrNull: function(val) {
                return angular.isUndefined(val) || val === null
            },
            noWorky: function(userId) {
                $log.info('mrl-noWorky', userId)
                _self.sendTo(_self.runtime.name, "noWorky", userId)
            },
            getRegistry: function() {
                return registry
            },
            getServices: function() {
                var arrayOfServices = Object.keys(registry).map(function(key) {
                    return registry[key]
                })
                return arrayOfServices
            },

            sendTo: _self.sendTo,
            getShortName: _self.getShortName,
            subscribe: _self.subscribe,
            unsubscribe: _self.unsubscribe,
            subscribeToService: _self.subscribeToService,
            getFullName: _self.getFullName,
            subscribeOnOpen: _self.subscribeOnOpen,
            sendBlockingMessage: _self.sendBlockingMessage,
            subscribeConnected: _self.subscribeConnected,
            subscribeToMethod: _self.subscribeToMethod,
            subscribeToServiceMethod: _self.subscribeToServiceMethod,
            sendRaw: self.sendRaw,
            sendMessage: _self.sendMessage,
            setAddServicePanel: _self.setAddServicePanel,
            createMessage: _self.createMessage,
            promise: _self.promise // FIXME - no sql like interface
            // put/get value to and from webgui service
        }
        return service
    }

    // assign callbacks
    this.request.onOpen = this.onOpen
    this.request.onClose = this.onClose
    this.request.onTransportFailure = this.onTransportFailure
    this.request.onMessage = this.onMessage
    this.request.onOpen = this.onOpen
    this.request.onError = this.onError
    this.id = this.generateId()

    // correct way to put in callbacks for js runtime instance
    jsRuntimeMethodMap["runtime@" + this.id + ".onRegistered"] = _self.onRegistered
    jsRuntimeMethodMap["runtime@" + this.id + ".setServiceTypes"] = _self.setServiceTypes

    // FIXME - not sure if this callback map/notify entry will have multiple recievers - but
    // it was standardized with the others to do so
    methodCallbackMap['getHelloResponse'] = []
    methodCallbackMap['getHelloResponse'].push(_self.getHelloResponse)
    methodCallbackMap['onHelloResponse'] = []
    methodCallbackMap['onHelloResponse'].push(_self.onHelloResponse)

    // set callback for subscribeNameMethod["runtime@webgui-client-1234-5678"] = _self.onRegistered
}
])
