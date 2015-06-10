angular.module('mrlapp', [
    'ui.bootstrap',
    'oc.lazyLoad',
    'mrlapp.wsconn',
    'mrlapp.service'
//    'mrlapp.service.arduinogui',
//    'mrlapp.service.clockgui',
//    'mrlapp.service.runtimegui',
//    'mrlapp.service.webguigui'
])

        .service('InstanceService', [function () {
                //TODO: should solve this different

                var platform;
                var platformId;
                var version;

                this.setPlatform = function (pl) {
                    platform = pl;
                    // lets display this
                    platformId = platform.arch + '.' + platform.bitness + '.' + platform.os;
                    // and this
                    version = platform.mrlVersion;
                };

                this.getPlatform = function () {
                    return platform;
                };

                this.getPlatformId = function () {
                    return platformId;
                };

                this.getVersion = function () {
                    return version;
                };

                var name;

                this.setName = function (nm) {
                    name = nm;
                };

                this.getName = function () {
                    return name;
                };
            }])

        .service('ServiceControllerService', ['HelperService', 'InstanceService', 'wsconnService', function (HelperService, InstanceService, wsconnService) {

                //TODO: sender? - what should be entered there? webgui-instance-name?
                //sendingMethod shouldn't equals to method, or?
                function Message(name, method, params) {
                    this.msgID = new Date().getTime();
                    this.timeStamp = this.msgID;
                    this.name = name;
                    this.sender = InstanceService.getName(); // FIXME - named passed in
                    this.sendingMethod = '';
                    this.historyList;// not necessary = new Array(); // necessary?
                    this.method = method;
                    this.data = params;
                }

                this.services = [];

                function Serv(name, servic) {
                    this.name = name;
                    this.servic = servic;
                }

                this.getServiceInst = function (name) {
                    if (HelperService.isUndefinedOrNull(this.services[name])) {
                        return null;
                    }
                    return this.services[name].servic;
                };

                this.addService = function (name, servic) {
                    this.services[name] = new Serv(name, servic);
                };

                this.removeService = function (name, servic) {
                    //TODO - fill in
                };

                this.test = function (servicename, method) {
                    //TODO: remove
                    executeFunctionByName(method, this.services[servicename].servic.methods);
//                    this.services["clock"].servic["tester"].apply(this);
                };

                //direct support (in service.js)
//                this.send = function (method, data) {
//                };

                this.sendTo = function (name, method, data) {
                    var msg = new Message(name, method, data);
                    msg.sendingMethod = 'send';
//                    console.log('SendTo:', msg);
                    wsconnService.sendMessage(msg);
                };

                //direct support (in service.js)
//                this.subscribe = function (inMethod, outMethod) {
//                };

                this.subscribeTo = function (publisherName, inMethod, outMethod) {
                    this.sendTo(InstanceService.getName(), "subscribe", [publisherName, inMethod, outMethod]);
                };

                function executeFunctionByName(functionName, context /*, args */) {
                    var args = [].slice.call(arguments).splice(2);
                    var namespaces = functionName.split(".");
                    var func = namespaces.pop();
                    for (var i = 0; i < namespaces.length; i++) {
                        context = context[namespaces[i]];
                    }
                    return context[func].apply(this, args);
                }
            }])

        .service('HelperService', [function () {
                this.isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };
            }])

        .directive('serviceComponent', ['$document', function ($document) {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        index: '@index',
                        list: '=list',
                        reftomain: '=reftomain',
                        size: '=size'
                    },
                    templateUrl: 'views/service.html',
                    link: function (scope, element, attr) {
                        var startX = 0, startY = 0, x = 0, y = 0;
                        var offsetY = 200;
                        var headerY = 50;
                        var footerY = 50;

                        var outgenerallist = false;
                        var ingenerallist = false;

                        //maybe change to absolute & layout the panels manually
                        //-> no panel glitch away (when you move another panel)
                        element.css({
                            position: 'relative'
                        });

                        element.on('mousedown', function (event) {
                            outgenerallist = event.pageY < 250;
                            startX = event.pageX - x;
                            startY = event.pageY - y;
                            var height = element.height();
                            if (startY < offsetY + headerY ||
                                    startY > offsetY + height - footerY) {
                                // Prevent default dragging of selected content
                                event.preventDefault();

                                //put panel in foreground
                                //TODO: worky, but sometimes buggy
                                var zindex = scope.list[scope.index].zindex;
                                console.log('zindex', zindex);
//                                var str1 = '';
//                                angular.forEach(scope.list, function (value, key) {
//                                    str1 = str1 + value.zindex + ", ";
//                                });
//                                console.log('zindex3', str1);
                                angular.forEach(scope.list, function (value, key) {
                                    //console.log("zindex2", key, value.zindex);
                                    if (value.zindex > zindex) {
                                        value.zindex--;
                                    }
                                });
                                scope.list[scope.index].zindex = scope.list.length;
//                                var str2 = '';
//                                angular.forEach(scope.list, function (value, key) {
//                                    str2 = str2 + value.zindex + ", ";
//                                });
//                                console.log('zindex3', str2);

                                element.css({
                                    cursor: 'move',
                                    'z-index': zindex
                                });
                                $document.on('mousemove', mousemove);
                                $document.on('mouseup', mouseup);
                            }
                        });

                        function mousemove(event) {
//                            console.log('mousemove', event.pageX, event.pageY);
                            ingenerallist = event.pageY < 250;
                            y = event.pageY - startY;
                            x = event.pageX - startX;
                            element.css({
                                top: y + 'px',
                                left: x + 'px'
                            });
                        }

                        function mouseup() {
                            $document.off('mousemove', mousemove);
                            $document.off('mouseup', mouseup);
                            element.css({
                                cursor: 'auto'
                            });
                            //move panel to different list (if it was moved there)
                            //TODO: worky, but sometimes buggy
                            if (ingenerallist && !outgenerallist) {
                                scope.reftomain.dragInGenerallist(scope.index, scope.list);
                            } else if (!ingenerallist && outgenerallist) {
                                scope.reftomain.dragOutGenerallist(scope.index);
                            }
                        }
                    }
                };
            }])

        .directive('tabComponent', [function () {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        index: '@index',
                        workspaces: '=workspaces',
                        reftomain: '=reftomain'
                    },
                    templateUrl: 'views/tab.html'
                };
            }])

        .filter('reverse', function () {
            return function (items) {
                return items.slice().reverse();
            };
        })

        .controller('MainCtrl', ['$scope', '$location', '$anchorScroll', 'ServiceControllerService', 'wsconnService', 'InstanceService', 'HelperService',
            function ($scope, $location, $anchorScroll, ServiceControllerService, wsconnService, InstanceService, HelperService) {

                //spawn all services in first workspace (also if workspaces with services are deleted)
                var spawnin = 0;

                //START_Status
                $scope.statuslist = [];

                $scope.addStatus = function (status) {
                    $scope.statuslist.push(status);
                };

                $scope.addStatus('And this is my status history!');
                $scope.addStatus('And this is my status history!');
                $scope.addStatus('And this is my status history!');
                $scope.addStatus('I am going to be the new WebUI for MyRobotLab!');
                //END_Status

                $scope.allServices = [];

                $scope.generallist = [];

                //START_Workspaces
                $scope.workspaces = [];
                $scope.workspacesref = [];
                var workspacecounter = 0;

                var setAllInactive = function () {
                    angular.forEach($scope.workspaces, function (workspace) {
                        workspace.active = false;
                    });
                };

                var addNewWorkspace = function () {
                    workspacecounter++;
                    $scope.workspaces.push({
                        name: "Workspace " + workspacecounter,
                        active: true,
                        dropdownisopen: false
                    });
                };

                addNewWorkspace();
                addNewWorkspace();
                setAllInactive();
                $scope.workspaces[0].active = true;

                $scope.addWorkspace = function () {
                    setAllInactive();
                    addNewWorkspace();
                };

                $scope.removeWorkspace = function (index) {
                    console.log('removeworkspace, ', index);
                    if ($scope.workspaces.length > 1) {
                        //recovering services in deleted workspace (if any)
                        var childservicelist = $scope.workspacesref[index].getDragsInList();
                        if (childservicelist.length > 0) {
                            console.log('recovering' + childservicelist.length + 'services form deleted workspace');
                            angular.forEach(childservicelist, function (value, key) {
                                $scope.workspacesref[spawnin].addDragToList(value);
                                //set new workspace-index for search
                                setWorkspaceIndexForSearch(value.name, spawnin);
                            });
                        }
                        //DELETE!
                        $scope.workspaces.splice(index, 1);
                        $scope.workspacesref.splice(index, 1);
                    } else {
                        console.log('cant remove, not enough workspaces left');
                    }
                };
                //END_Workspaces

                //TODO: refactor this
                $scope.reftomain = {};

                $scope.reftomain.addRefToWorkspace = function (index, workspace) {
                    console.log('adding ref to workspace', index, workspace);
                    $scope.workspacesref[index] = workspace;
                };

                $scope.reftomain.dragInGenerallist = function (index, list) {
                    //until I fix the behavior of the (general)list, only able to store service (also adjust zindex(e) then!)
                    if ($scope.generallist.length < 1) {
                        console.log('tab -> general');
                        //MOVE!
                        var service = list[index];
                        var zindex = service.zindex;
                        service.zindex = 1;
                        $scope.generallist.push(service);
                        list.splice(index, 1);
                        //set new workspace-index for search
                        setWorkspaceIndexForSearch(service.name, -1);
                        //adjust zindex for all other services (in service's previous list)
                        angular.forEach(list, function (value, key) {
                            if (value.zindex > zindex) {
                                value.zindex--;
                            }
                        });
                    } else {
                        console.log('not able to move - general is already containing a service!');
                    }
                };

                $scope.reftomain.dragOutGenerallist = function (index) {
                    console.log('general -> tab');
                    var ind = -1;
                    angular.forEach($scope.workspaces, function (value, key) {
                        if (value.active) {
                            ind = key;
                        }
                    });
                    //MOVE!
                    var service = $scope.generallist[index];
                    $scope.generallist.splice(index, 1);
                    $scope.workspacesref[ind].addDragToList(service);
                    //set new workspace-index for search
                    setWorkspaceIndexForSearch(service.name, ind);
                };

                var setWorkspaceIndexForSearch = function (servicename, workspaceindex) {
                    //set new workspace-index for search
                    angular.forEach($scope.allServices, function (value, key) {
                        if (value.name == servicename) {
                            value.workspace = workspaceindex;
                        }
                    });
                };

                //TODO: not final method & location
                $scope.createService = function (name, type, simpletype) {
                    console.log('trying to launch ' + name + ' of ' + type + ' / ' + simpletype);
//                    console.log($scope.workspacesref);
                    if (HelperService.isUndefinedOrNull($scope.workspacesref[0])) {
                        //for Chrome - race condition!
                        //should find a better way
                        console.log('waiting for workspaces to register themselves');
                        var listener = $scope.$watch(function () {
                            return $scope.workspacesref[0];
                        }, function () {
                            console.log('noticed change, checking');
//                                    console.log($scope.workspacesref);
                            if ($scope.workspacesref.length > spawnin) {
                                console.log('done! with waiting');
//                                        console.log($scope.workspacesref);
                                listener();
                                createServiceReally(name, type, simpletype);
                            }
                        });
                    } else {
                        createServiceReally(name, type, simpletype);
                    }
//                    createServiceReally(name, type, simpletype);
                };

                var createServiceReally = function (name, type, simpletype) {
                    $scope.workspacesref[spawnin].addDragToList({
                        'name': name,
                        'type': type,
                        'simpletype': simpletype
                    });
                    $scope.allServices.push({
                        'name': name,
                        'type': type,
                        'simpletype': simpletype,
                        'workspace': spawnin
                    });
                };

                $scope.about = function () {
                    console.log('about');
                };

                var servicecounter = 0;

                $scope.help = function () {
                    console.log('help');
                    var servicetype;
                    var simpletype;
                    if (servicecounter % 2 == 0) {
                        servicetype = 'clock';
                        simpletype = 'Clock';
                    } else {
                        servicetype = 'arduino';
                        simpletype = 'Arduino';
                    }
                    $scope.createService("ser" + servicecounter, servicetype, simpletype);
                    servicecounter++;
                };

                $scope.searchOnSelect = function (item, model, label) {
                    console.log('searchOnSelect');
                    if (item.workspace != -1) {
                        //select the workspace containing the selected service
                        setAllInactive();
                        $scope.workspaces[item.workspace].active = true;
                        //scroll to selected service
                        $location.hash(item.name);
                        $anchorScroll();
                    }
                };

                var onMessage = function (msg) {
                    //TODO: not final location
                    console.log('JeyJeyJeyJeyJeyJeyJeyJeyJeyJey');
                    console.log('Message:', msg);
                    switch (msg.method) {
                        case 'onLocalServices':
                            // if just connected - got a Runtime.getLocalServices msg response
                            // within the msg.data is a Java ServiceEnvironment
                            // it has Platform info & all local running Services
                            // We need to load all JavaScript types of the Running services
                            // and create a dictionary of name : --to--> instance of Service type
                            // the following services var needs to be in a global object or service - reachable by all
                            var services = msg.data[0].serviceDirectory;
                            angular.forEach(services, function (value, key) {
                                var name = value.name;
                                var type = value.simpleName.toLowerCase();
                                var simpletype = value.simpleName;
                                $scope.createService(name, type, simpletype);
                                if (type == 'webgui') {
                                    InstanceService.setName(name);
                                }
                            });
                            $scope.$apply();
//                            for (var serviceName in services) {
//                                service = services[serviceName];
//                                console.log('mrl is currently running a ' + service.name + ' of type ' + service.simpleName);
//                            }
                            InstanceService.setPlatform(msg.data[0].platform);
                            break;
                        case 'onHandleError':
                            // this is an Error in msg form - so its a controlled error sent by mrl to notify
                            // something has gone wrong in the backend
                            console.log('Error onHandleError: ', msg.data[0]);
                            break;
                    }
                };

                wsconnService.subscribeToMessages(onMessage);
                wsconnService.connect(document.location.origin.toString() + '/api/messages');
                //connect to backend
//                ConnectionService.connect(document.location.origin.toString() + '/api/messages');
//                ConnectionService.connect('/api');
            }])

        .controller('TabsChildCtrl', ['$scope', 'ServiceControllerService', 'HelperService', function ($scope, ServiceControllerService, HelperService) {

                console.log("scope,workspaces", $scope.workspaces);
                console.log("scope,index", $scope.index);

                if (!HelperService.isUndefinedOrNull($scope.index)) {
                    $scope.workspace = $scope.workspaces[$scope.index];
                }

                $scope.servicelist = [];

                //TODO: refactor this
                $scope.reftotab = {};
                $scope.reftotab.addDragToList = function (service) {
                    if (service != null) {
                        var max = -1;
                        angular.forEach($scope.servicelist, function (value, key) {
                            if (value.zindex > max) {
                                max = value.zindex;
                            }
                        });
                        service.zindex = max + 1;
                        $scope.servicelist.push(service);
                    }
                };
                $scope.reftotab.getDragsInList = function () {
                    return $scope.servicelist;
                };

                $scope.reftomain.addRefToWorkspace($scope.index, $scope.reftotab);
            }]);
