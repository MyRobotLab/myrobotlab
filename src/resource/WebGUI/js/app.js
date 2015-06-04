angular.module('mrlapp', [
    'ui.bootstrap',
    'mrlapp.service',
    'mrlapp.service.arduinogui',
    'mrlapp.service.clockgui'
])

        .service('ServiceControllerService', ['HelperService', function (HelperService) {

                /*function Message() {
                 this.msgID;
                 this.timestamp;
                 this.name;
                 this.sender;
                 this.sendingMethod;
                 this.historyList;
                 this.security;
                 this.status;
                 this.msgType;
                 this.method;
                 this.data;
                 }*/

                /*function Message(name, method, params) {
                 this.msgID = new Date().getTime();
                 this.timeStamp = this.msgID;
                 this.name = name;
                 this.sender = webguiName; // FIXME - named passed in
                 this.sendingMethod = method;
                 this.historyList = new Array(); // necessary?
                 this.method = method;
                 this.data = params;
                 }*/

                this.services = [];

                function Serv(name, servic) {
                    this.name = name;
                    this.servic = servic;
                    this.listener = [];
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

                this.addListener = function (name, listener, listener2) {
                    //TODO: right name?
                    //TODO - need to work on this
                    this.services[name].listener.push(listener);
                };

                this.removeListener = function () {
                    //TODO: right name?
                    //TODO - fill in
                };

                this.notify = function () {
                    //TODO: what did I want to do with this function?
                    //TODO - fill in
                };

                this.test = function (name) {
                    //TODO: remove
                    executeFunctionByName("pulse", this.services[name].servic.methods);
//                    this.services["clock"].servic["tester"].apply(this);
                };

//                TODO - delete
//                this.observers = [];
//
//                this.registerObserver = function (observer) {
//                    this.observers.push(observer);
//                };
//
//                this.notifyAll = function () {
//                    for (var index = 0; index < this.observers.length; ++index)
//                        this.observers[index].notify();
//                };
//
//                this.myData = ["some", "list", "of", "data"];
//
//                this.setData = function (newData) {
//                    myData = newData;
//                    notifyAll();
//                };
//
//                this.getData = function () {
//                    return myData;
//                };

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

        .directive('dropComponent', [function () {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        list: '=list',
                        size: '@size'
                    },
                    templateUrl: 'views/drop.html'
                };
            }])

        .filter('reverse', function () {
            return function (items) {
                return items.slice().reverse();
            };
        })

        .controller('MainCtrl', ['$scope', 'ServiceControllerService', function ($scope, ServiceControllerService) {

//                $scope.statusakt = 'I am going to be the new WebUI for MyRobotLab!';
                $scope.statuslist = [];

                $scope.addStatus = function (status) {
                    $scope.statuslist.push(status);
                };

                $scope.addStatus('And this is my status history!');
                $scope.addStatus('And this is my status history!');
                $scope.addStatus('And this is my status history!');
                $scope.addStatus('I am going to be the new WebUI for MyRobotLab!');

                $scope.allServices = [];

                $scope.generallist = [];

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

                $scope.workspaces = [];
                $scope.workspacesref = [];
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
                        $scope.workspaces.splice(index, 1);
                        $scope.workspacesref.splice(index, 1);
                    } else {
                        console.log('cant remove, not enough workspaces left');
                    }
                };

                //TODO: refactor this
                $scope.reftomain = {};

                $scope.reftomain.addRefToWorkspace = function (index, workspace) {
                    $scope.workspacesref[index] = workspace;
                };

                $scope.reftomain.dragInGenerallist = function (index, list) {
                    console.log('tab -> general');
                    $scope.generallist.push(list[index]);
                    list.splice(index, 1);
                };

                $scope.reftomain.dragOutGenerallist = function (index) {
                    console.log('general -> tab');
                    var ind = -1;
                    angular.forEach($scope.workspaces, function (value, key) {
                        if (value.active) {
                            ind = key;
                        }
                    });
                    $scope.workspacesref[ind].addDragToList($scope.generallist[index]);
                    $scope.generallist.splice(index, 1);
                };

                //TODO: not final method & location
                $scope.createService = function (name, type) {
                    //spawn service in first workspace
                    var spawnin = 0;
                    $scope.workspacesref[spawnin].addDragToList({
                        'name': name,
                        'type': type
                    });
                    $scope.allServices.push({
                        'name': name,
                        'type': type,
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
                    if (servicecounter % 2 == 0) {
                        servicetype = 'clock';
                    } else {
                        servicetype = 'arduino';
                    }
                    $scope.createService("ser" + servicecounter, servicetype);
                    servicecounter++;
                };

                $scope.searchOnSelect = function (item, model, label) {
                    console.log('searchOnSelect');
                    //select the workspace containing the selected service
                    setAllInactive();
                    $scope.workspaces[item.workspace].active = true;
                    //TODO: scroll to selected service
                };

                var Connection = {};
                Connection.transport = 'websocket';
                Connection.socket = null;

                Connection.receivedMessage = function (message) {
                    console.log('Received Message: ', message);
                    //TODO - process message (& probably forward it to ServiceControllerService (in most cases))
//                    var packet = message;
//                    switch (packet.type) {
//                            case 'update':
//                                for (var i = 0; i < packet.data.length; i++) {
//                                    Game.updateSnake(packet.data[i].id, packet.data[i].body);
//                                }
//                                break;
//                            case 'join':
//                                for (var j = 0; j < packet.data.length; j++) {
//                                    Game.addSnake(packet.data[j].id, packet.data[j].color);
//                                }
//                                break;
//                            case 'leave':
//                                Game.removeSnake(packet.id);
//                                break;
//                            case 'dead':
//                                Console.log('Info: Your snake is dead, bad luck!');
//                                Game.direction = 'none';
//                                break;
//                            case 'kill':
//                                Console.log('Info: Head shot!');
//                                break;
//                        }
                };

                Connection.sendMessage = function (message) {
                    Connection.socket.push(message);
                    console.log('Sent message: ' + message);
                };

                Connection.connect = function (host) {
                    var request = {url: host,
                        transport: 'websocket',
                        enableProtocol: true,
                        trackMessageLength: true,
                        logLevel: 'debug'};

                    request.onOpen = function (response) {
                        // Socket open ...
                        console.log('Info: ' + Connection.transport + ' connection opened.');
                    };

                    request.onClose = function (response) {
                        console.log('websocket, onclose');
                        if (response.state == "unsubscribe") {
                            console.log('Info: ' + Connection.transport + ' closed.');
                        }
                    };

                    request.onTransportFailure = function (errorMsg, request) {
                        jQuery.atmosphere.info(errorMsg);
                        if (window.EventSource) {
                            request.fallbackTransport = "sse";
                        } else {
                            request.fallbackTransport = 'long-polling';
                        }
                        Connection.transport = request.fallbackTransport;

                        console.log('Error: falling back to ' + Connection.transport + ' ' + errorMsg);
                    };

                    request.onMessage = function (response) {
                        var message = response.responseBody;
                        var packet;
                        try {
                            packet = eval('(' + message + ')'); //jQuery.parseJSON(message);
                        } catch (e) {
                            if (message == 'X') {
                                console.log('heartbeat:', message);
                            } else {
                                console.log('Error Message: ', message);
                            }
                            return;
                        }

                        Connection.receivedMessage(packet);
                    };
                    Connection.socket = $.atmosphere.subscribe(request);
                };

                Connection.connect(document.location.origin.toString() + '/api');
//                Connection.connect(document.location.origin.toString() + '/api');
//                console.log($);
//                Connection.connect('/api');
            }])

        .controller('TabsChildCtrl', ['$scope', 'ServiceControllerService', 'HelperService', function ($scope, ServiceControllerService, HelperService) {

                console.log("scope,workspaces", $scope.workspaces);
                console.log("scope,index", $scope.index);

                if (!HelperService.isUndefinedOrNull($scope.index)) {
                    $scope.workspace = $scope.workspaces[$scope.index];
                }

                //TODO: refactor this
                $scope.reftotab = {};
                $scope.reftotab.addDragToList = function (panel) {
                    angular.forEach($scope.servicelist, function (value, key) {
                        value.zindex++;
                    });
                    panel.zindex = 1;
                    $scope.servicelist.push(panel);
                };

                $scope.reftomain.addRefToWorkspace($scope.index, $scope.reftotab);

                $scope.servicelist = [
//                    {'name': 'sera', 'drag': true, 'zindex': 1, 'type': 'clock'},
//                    {'name': 'serb', 'drag': true, 'zindex': 2, 'type': 'arduino'},
//                    {'name': 'serc', 'drag': true, 'zindex': 3, 'type': 'clock'},
//                    {'name': 'serd', 'drag': true, 'zindex': 4, 'type': 'arduino'},
//                    {'name': 'sere', 'drag': true, 'zindex': 5, 'type': 'clock'},
//                    {'name': 'serf', 'drag': true, 'zindex': 6, 'type': 'arduino'},
//                    {'name': 'serg', 'drag': true, 'zindex': 7, 'type': 'clock'},
//                    {'name': 'serh', 'drag': true, 'zindex': 8, 'type': 'arduino'},
//                    {'name': 'seri', 'drag': true, 'zindex': 9, 'type': 'clock'},
//                    {'name': 'serj', 'drag': true, 'zindex': 10, 'type': 'arduino'},
//                    {'name': 'serk', 'drag': true, 'zindex': 11, 'type': 'clock'},
//                    {'name': 'serl', 'drag': true, 'zindex': 12, 'type': 'arduino'},
//                    {'name': 'serm', 'drag': true, 'zindex': 13, 'type': 'clock'},
//                    {'name': 'sern', 'drag': true, 'zindex': 14, 'type': 'arduino'},
//                    {'name': 'sero', 'drag': true, 'zindex': 15, 'type': 'clock'}
                ];
            }]);