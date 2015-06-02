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
                    executeFunctionByName("pulse", this.services[name].servic);
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

        .controller('MainCtrl', ['$scope', 'ServiceControllerService', function ($scope, ServiceControllerService) {

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
                    $scope.workspaces.splice(index, 1);
                    $scope.workspacesref.splice(index, 1);
                };

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

                //TODO: populate list dynamically & with the services, not states & flags
                $scope.allServices = [{'name': 'Alabama', 'flag': '5/5c/Flag_of_Alabama.svg/45px-Flag_of_Alabama.svg.png'}, {'name': 'Alaska', 'flag': 'e/e6/Flag_of_Alaska.svg/43px-Flag_of_Alaska.svg.png'}, {'name': 'Arizona', 'flag': '9/9d/Flag_of_Arizona.svg/45px-Flag_of_Arizona.svg.png'}, {'name': 'Arkansas', 'flag': '9/9d/Flag_of_Arkansas.svg/45px-Flag_of_Arkansas.svg.png'}, {'name': 'California', 'flag': '0/01/Flag_of_California.svg/45px-Flag_of_California.svg.png'}, {'name': 'Colorado', 'flag': '4/46/Flag_of_Colorado.svg/45px-Flag_of_Colorado.svg.png'}, {'name': 'Connecticut', 'flag': '9/96/Flag_of_Connecticut.svg/39px-Flag_of_Connecticut.svg.png'}, {'name': 'Delaware', 'flag': 'c/c6/Flag_of_Delaware.svg/45px-Flag_of_Delaware.svg.png'}, {'name': 'Florida', 'flag': 'f/f7/Flag_of_Florida.svg/45px-Flag_of_Florida.svg.png'}, {'name': 'Georgia', 'flag': '5/54/Flag_of_Georgia_%28U.S._state%29.svg/46px-Flag_of_Georgia_%28U.S._state%29.svg.png'}, {'name': 'Hawaii', 'flag': 'e/ef/Flag_of_Hawaii.svg/46px-Flag_of_Hawaii.svg.png'}, {'name': 'Idaho', 'flag': 'a/a4/Flag_of_Idaho.svg/38px-Flag_of_Idaho.svg.png'}, {'name': 'Illinois', 'flag': '0/01/Flag_of_Illinois.svg/46px-Flag_of_Illinois.svg.png'}, {'name': 'Indiana', 'flag': 'a/ac/Flag_of_Indiana.svg/45px-Flag_of_Indiana.svg.png'}, {'name': 'Iowa', 'flag': 'a/aa/Flag_of_Iowa.svg/44px-Flag_of_Iowa.svg.png'}, {'name': 'Kansas', 'flag': 'd/da/Flag_of_Kansas.svg/46px-Flag_of_Kansas.svg.png'}, {'name': 'Kentucky', 'flag': '8/8d/Flag_of_Kentucky.svg/46px-Flag_of_Kentucky.svg.png'}, {'name': 'Louisiana', 'flag': 'e/e0/Flag_of_Louisiana.svg/46px-Flag_of_Louisiana.svg.png'}, {'name': 'Maine', 'flag': '3/35/Flag_of_Maine.svg/45px-Flag_of_Maine.svg.png'}, {'name': 'Maryland', 'flag': 'a/a0/Flag_of_Maryland.svg/45px-Flag_of_Maryland.svg.png'}, {'name': 'Massachusetts', 'flag': 'f/f2/Flag_of_Massachusetts.svg/46px-Flag_of_Massachusetts.svg.png'}, {'name': 'Michigan', 'flag': 'b/b5/Flag_of_Michigan.svg/45px-Flag_of_Michigan.svg.png'}, {'name': 'Minnesota', 'flag': 'b/b9/Flag_of_Minnesota.svg/46px-Flag_of_Minnesota.svg.png'}, {'name': 'Mississippi', 'flag': '4/42/Flag_of_Mississippi.svg/45px-Flag_of_Mississippi.svg.png'}, {'name': 'Missouri', 'flag': '5/5a/Flag_of_Missouri.svg/46px-Flag_of_Missouri.svg.png'}, {'name': 'Montana', 'flag': 'c/cb/Flag_of_Montana.svg/45px-Flag_of_Montana.svg.png'}, {'name': 'Nebraska', 'flag': '4/4d/Flag_of_Nebraska.svg/46px-Flag_of_Nebraska.svg.png'}, {'name': 'Nevada', 'flag': 'f/f1/Flag_of_Nevada.svg/45px-Flag_of_Nevada.svg.png'}, {'name': 'New Hampshire', 'flag': '2/28/Flag_of_New_Hampshire.svg/45px-Flag_of_New_Hampshire.svg.png'}, {'name': 'New Jersey', 'flag': '9/92/Flag_of_New_Jersey.svg/45px-Flag_of_New_Jersey.svg.png'}, {'name': 'New Mexico', 'flag': 'c/c3/Flag_of_New_Mexico.svg/45px-Flag_of_New_Mexico.svg.png'}, {'name': 'New York', 'flag': '1/1a/Flag_of_New_York.svg/46px-Flag_of_New_York.svg.png'}, {'name': 'North Carolina', 'flag': 'b/bb/Flag_of_North_Carolina.svg/45px-Flag_of_North_Carolina.svg.png'}, {'name': 'North Dakota', 'flag': 'e/ee/Flag_of_North_Dakota.svg/38px-Flag_of_North_Dakota.svg.png'}, {'name': 'Ohio', 'flag': '4/4c/Flag_of_Ohio.svg/46px-Flag_of_Ohio.svg.png'}, {'name': 'Oklahoma', 'flag': '6/6e/Flag_of_Oklahoma.svg/45px-Flag_of_Oklahoma.svg.png'}, {'name': 'Oregon', 'flag': 'b/b9/Flag_of_Oregon.svg/46px-Flag_of_Oregon.svg.png'}, {'name': 'Pennsylvania', 'flag': 'f/f7/Flag_of_Pennsylvania.svg/45px-Flag_of_Pennsylvania.svg.png'}, {'name': 'Rhode Island', 'flag': 'f/f3/Flag_of_Rhode_Island.svg/32px-Flag_of_Rhode_Island.svg.png'}, {'name': 'South Carolina', 'flag': '6/69/Flag_of_South_Carolina.svg/45px-Flag_of_South_Carolina.svg.png'}, {'name': 'South Dakota', 'flag': '1/1a/Flag_of_South_Dakota.svg/46px-Flag_of_South_Dakota.svg.png'}, {'name': 'Tennessee', 'flag': '9/9e/Flag_of_Tennessee.svg/46px-Flag_of_Tennessee.svg.png'}, {'name': 'Texas', 'flag': 'f/f7/Flag_of_Texas.svg/45px-Flag_of_Texas.svg.png'}, {'name': 'Utah', 'flag': 'f/f6/Flag_of_Utah.svg/45px-Flag_of_Utah.svg.png'}, {'name': 'Vermont', 'flag': '4/49/Flag_of_Vermont.svg/46px-Flag_of_Vermont.svg.png'}, {'name': 'Virginia', 'flag': '4/47/Flag_of_Virginia.svg/44px-Flag_of_Virginia.svg.png'}, {'name': 'Washington', 'flag': '5/54/Flag_of_Washington.svg/46px-Flag_of_Washington.svg.png'}, {'name': 'West Virginia', 'flag': '2/22/Flag_of_West_Virginia.svg/46px-Flag_of_West_Virginia.svg.png'}, {'name': 'Wisconsin', 'flag': '2/22/Flag_of_Wisconsin.svg/45px-Flag_of_Wisconsin.svg.png'}, {'name': 'Wyoming', 'flag': 'b/bc/Flag_of_Wyoming.svg/43px-Flag_of_Wyoming.svg.png'}];

                var Connection = {};
                Connection.transport = 'websocket';
                Connection.socket = null;

                Connection.receivedMessage = function (message) {
                    //TODO
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
                        if (response.state == "unsubscribe") {
                            console.log('Info: ' + Connection.transport + ' closed.');
                        }
                    };

                    request.onTransportFailure = function (errorMsg, request) {
                    	console.log('Error: falling back to long-polling ' + errorMsg);
                        jQuery.atmosphere.info(errorMsg);
                        if (window.EventSource) {
                            request.fallbackTransport = "sse";
                        } else {
                            request.fallbackTransport = 'long-polling';
                        }
                        Connection.transport = request.fallbackTransport;
                    };

                    /**
                     * main Message processor
                     */
                    request.onMessage = function (response) {
                        var message = response.responseBody;
                        var packet;
                        try {
                        	// TODO - from registry (services array?) get the appropriate serviceGUI
                        	// e.g.  services[msg.name].eval(msg.method(msg.data))
                        	// 
                        	
                        	// send to bound service name
                        	//  ServiceGUI.prototype.send = function(method, data) {
                        	//	    var msg = new Message(this.name, method, data);
                        	//  	var json = JSON.stringify(msg);
                        	//   	connection.ws.send(json);
                        	//   };
                        	
                        	
                            packet = eval('(' + message + ')'); //jQuery.parseJSON(message);
                            
                        } catch (e) {
                            console.log('Error Message: ', message);
                            return;
                        }

                        Connection.receivedMessage(packet);
                    };
                    Connection.socket = $.atmosphere.subscribe(request);
                };

                Connection.connect(document.location.origin.toString() + '/api');
                // console.log($)
                //Connection.connect('/api');
            }])

        .controller('TabsChildCtrl', ['$scope', 'ServiceControllerService', 'HelperService', function ($scope, ServiceControllerService, HelperService) {

                console.log("scope,workspaces", $scope.workspaces);
                console.log("scope,index", $scope.index);

                if (!HelperService.isUndefinedOrNull($scope.index)) {
                    $scope.workspace = $scope.workspaces[$scope.index];
                }

                $scope.reftotab = {};
                $scope.reftotab.addDragToList = function (panel) {
                    $scope.list5.push(panel);
                };

                $scope.reftomain.addRefToWorkspace($scope.index, $scope.reftotab);

//                $scope.list1 = [];
//                $scope.list2 = [];
//                $scope.list3 = [];
//                $scope.list4 = [];

                $scope.list5 = [
                    {'name': 'sera', 'drag': true, 'zindex': 1, 'type': 'clock'},
                    {'name': 'serb', 'drag': true, 'zindex': 2, 'type': 'arduino'},
                    {'name': 'serc', 'drag': true, 'zindex': 3, 'type': 'clock'},
                    {'name': 'serd', 'drag': true, 'zindex': 4, 'type': 'arduino'},
                    {'name': 'sere', 'drag': true, 'zindex': 5, 'type': 'clock'},
                    {'name': 'serf', 'drag': true, 'zindex': 6, 'type': 'arduino'},
                    {'name': 'serg', 'drag': true, 'zindex': 7, 'type': 'clock'},
                    {'name': 'serh', 'drag': true, 'zindex': 8, 'type': 'arduino'},
                    {'name': 'seri', 'drag': true, 'zindex': 9, 'type': 'clock'},
                    {'name': 'serj', 'drag': true, 'zindex': 10, 'type': 'arduino'},
                    {'name': 'serk', 'drag': true, 'zindex': 11, 'type': 'clock'},
                    {'name': 'serl', 'drag': true, 'zindex': 12, 'type': 'arduino'},
                    {'name': 'serm', 'drag': true, 'zindex': 13, 'type': 'clock'},
                    {'name': 'sern', 'drag': true, 'zindex': 14, 'type': 'arduino'},
                    {'name': 'sero', 'drag': true, 'zindex': 15, 'type': 'clock'}
                ];
            }]);