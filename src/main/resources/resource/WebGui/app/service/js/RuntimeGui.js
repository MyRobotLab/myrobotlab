angular.module('mrlapp.service.RuntimeGui', []).controller('RuntimeGuiCtrl', ['$scope', '$log', 'mrl', 'statusSvc', '$timeout', function($scope, $log, mrl, statusSvc, $timeout) {
    console.info('RuntimeGuiCtrl')
    var _self = this
    var msg = this.msg

    var statusMaxSize = 2500

    this.updateState = function(service) {
        $scope.service = service
        service.serviceData.categoryTypes["show all"] = {
            "name": "show all",
            "serviceTypes": []
        }
    }

    $scope.platform = $scope.service.platform
    $scope.status = ""
    $scope.cmd = ""
    $scope.registry = {}
    $scope.connections = {}
    $scope.newName = null
    $scope.newType = ""
    $scope.heartbeatTs = null

    $scope.category = {
        selected: null
    }

    $scope.categoryServiceTypes = null

    $scope.disabled = undefined;
    $scope.person = {};

    var msgKeys = {}

    let categoryServiceTypes = null

    // $scope.categoryServiceTypes = $scope.service.serviceData.categoryTypes[$scope.category.selected].serviceTypes

    $scope.filterServices = function() {
        var result = {};
        // console.debug('$scope.category.selected is ' + $scope.category.selected)
        const entries = Object.entries($scope.service.serviceData.serviceTypes)

        if ($scope.category.selected != null && ($scope.category.selected == 'show all') ) {
            return $scope.service.serviceData.serviceTypes
        }

        for (const [fullTypeName,metaData] of entries) {
            // if (metaData.simpleName.toLowerCase().includes($scope.newType)) {

            if ($scope.category.selected != null) {
                categoryServiceTypes = $scope.service.serviceData.categoryTypes[$scope.category.selected].serviceTypes
            } else {
                categoryServiceTypes = null
            }

            if (/*metaData.simpleName.toLowerCase().includes($scope.newType) && */
            categoryServiceTypes != null && categoryServiceTypes.includes(metaData.name)) {
                result[fullTypeName] = metaData;
            }
        }
        return result;
    }

    // FIXME - should be a mrl service function ???
    $scope.sendToCli = function(cmd) {
        console.log("sendToCli " + cmd)
        // msg.sendBlocking("sendToCli", cmd)
        $scope.cmd = ""
        contextPath = null

        var cliMsg = _self.cliToMsg(contextPath, "runtime@" + mrl.getId(), "runtime@" + mrl.getRemoteId(), cmd)

        ret = mrl.sendBlockingMessage(cliMsg)

        //    ret.then(result=>alert(result), // shows "done!" after 1 second
        //    error=>alert(error)// doesn't run
        //    )

        ret.then(function(result) {
            if ('data'in result) {
                $scope.status = JSON.stringify(result.data[0], null, 2)
            }
            $scope.$apply()
        }).catch(function(error) {
            console.error(error);
        })
    }

    $scope.setServiceType = function(serviceType) {
        $scope.newType = serviceType
    }

    $scope.start = function() {

        if ($scope.newName == null) {
            mrl.error("name of service is required")
            return
        }
        if ($scope.newType == null) {
            mrl.error("type of service is required")
            return
        }

        if (typeof $scope.newType == 'object') {
            $scope.newType = $scope.newType.name
        }
        msg.send('start', $scope.newName, $scope.newType)

        $scope.newName = null;
        $scope.newType = null;
    }

    this.cliToMsg = function(contextPath, from, to, cmd) {
        cmd = cmd.trim()
        var msg = mrl.createMessage(to, "ls", null)
        msg.msgType = 'B'
        // will be a blocking msg

        if (contextPath != null) {
            cmd = contextPath + cmd
        }

        // assume runtime as 'default'
        if (msg.name == null) {
            msg.name = "runtime"
        }

        // two possibilities - either it begins with "/" or it does not
        // if it does begin with "/" its an absolute path to a dir, ls, or invoke
        // if not then its a runtime method

        if (cmd.startsWith("/")) {
            // ABSOLUTE PATH !!!
            parts = cmd.split("/")

            if (parts.length < 3) {
                msg.method = "ls"
                msg.data = ["\"" + cmd + "\""]
                return msg
            }

            // fix me diff from 2 & 3 "/"
            if (parts.length >= 3) {
                msg.name = parts[1]

                if (!msg.name.includes('@')) {
                    msg.name += '@' + $scope.service.id
                }

                // prepare the method
                msg.method = parts[2].trim()

                // FIXME - to encode or not to encode that is the question ...
                if (parts.length > 3) {
                    // WTF ? 0 length array has something in it ?
                    payload = [parts.length - 3]
                    for (var i = 3; i < parts.length; ++i) {
                        payload[i - 3] = parts[i]
                    }
                    msg.data = payload
                }
            }
            return msg
        } else {
            // NOT ABOSLUTE PATH - SIMILAR TO EXECUTING IN THE RUNTIME /usr/bin path
            // (ie runtime methods!)
            // spaces for parameter delimiters ?
            spaces = cmd.split(" ")
            // FIXME - need to deal with double quotes e.g. func A "B and C" D - p0 =
            // "A" p1 = "B and C" p3 = "D"
            msg.method = spaces[0]
            payload = []
            for (var i = 1; i < spaces.length; ++i) {
                // webgui will never use this section of code
                // currently the codepath is only excercised by InProcessCli
                // all of this methods will be "optimized" single commands to runtime (i think)
                // so we are going to error on the side of String parameters - other data types will have problems
                // payload[i - 1] = "\"" + spaces[i] + "\""
                payload[i - 1] = spaces[i]
            }
            msg.data = payload

            return msg
        }

    }

    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            $timeout(function() {
                _self.updateState(inMsg.data[0])
            })
            break
        case 'onLocalServices':
            {
                $scope.registry = inMsg.data[0]
                break
            }
        case 'onServiceTypes':
            {
                $scope.possibleServices = inMsg.data[0]
                mrl.setPossibleServices($scope.possibleServices)
                break
            }
        case 'onRegistered':
            {
                // inMsg.data[0]
                console.log("onRegistered")
                break
            }
        case 'onConnectionHeaders':
            {
                $scope.connections = inMsg.data[0]
                $scope.$apply()
                break
            }
        case 'onStatus':
            {
                $scope.status = inMsg.data[0].name + ' ' + inMsg.data[0].level + ' ' + inMsg.data[0].detail + "\n" + $scope.status
                if ($scope.status.length > 300) {
                    $scope.status = $scope.status.substring(0, statusMaxSize)
                }
                break
            }
        case 'onSendToCli':
            {
                if (inMsg.data[0] != null) {
                    $scope.status = JSON.stringify(inMsg.data[0], null, 2) + "\n" + $scope.status
                    if ($scope.status.length > 300) {
                        $scope.status = $scope.status.substring(0, statusMaxSize)
                    }
                    $scope.$apply()
                } else {
                    $scope.status += "null\n"
                }
                break
            }
        case 'onReleased':
            {
                console.info("runtime - onRelease" + inMsg.data[0])
                break
            }
        case 'onHeartbeat':
            {
                let heartbeat = inMsg.data[0]
                let hb = heartbeat.name + '@' + heartbeat.id + ' sent onHeartbeat - ';
                $scope.heartbeatTs = heartbeat.ts
                $scope.$apply()

                for (let i in heartbeat.serviceList) {
                    let serviceName = heartbeat.serviceList[i].name + '@' + heartbeat.serviceList[i].id
                    hb += serviceName + ' '

                    // FIXME - 'merge' ie remove missing services

                    // FIXME - want to maintain "local" registry ???
                    // currently maintaining JS process registry - should the RuntimeGui also maintain
                    // its 'own' sub-registry ???
                    if (!serviceName in mrl.getRegistry()) {
                        // 
                        console.warn(serviceName + ' not defined in registry - sending registration request');
                    }
                    // else already registered
                }

                console.info(hb)

                // CHECK REGISTRY
                // SYNC SERVICES
                // REQUEST REGISTRATIONS !!!!
                break
            }
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    this.promiseTimeout = function(ms, promise) {
        // Create a promise that rejects in <ms> milliseconds
        let timeout = new Promise((resolve,reject)=>{
            let id = setTimeout(()=>{
                clearTimeout(id)
                reject('Timed out in ' + ms + 'ms.')
            }
            , ms)
        }
        )
        // Returns a race between our timeout and the passed in promise
        return Promise.race([promise, timeout])
    }

    // _self.promiseTimeout(1000, myVar = setTimeout({}, 3000))

    // $scope.possibleServices = Object.values(mrl.getPossibleServices())
    msg.subscribe("getServiceTypes")
    msg.subscribe("getLocalServices")
    msg.subscribe("registered")
    msg.subscribe("getConnectionHeaders")
    msg.subscribe("sendToCli")

    //msg.send("getLocalServices")
    msg.send("getConnectionHeaders")
    msg.send("getServiceTypes")
    msg.subscribe(this)
}
])
