angular.module('mrlapp', [
    'ngDragDrop',
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

        .directive('serviceComponent', [function () {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        index: '@index',
                        list: '=list',
                        size: '=size',
                        gtc: '=gtc'
                    },
                    templateUrl: 'views/service.html',
                    link: function (scope, elem, attrs) {
                        scope.$watch(function () {
                            scope.list[scope.index].height = elem.height();
                            //updateheight
                            console.log("hey!, updateheight");
                            var sum = 0;
                            angular.forEach(scope.list, function (value, key) {
                                console.log("hey2!", value.height);
                                sum = sum + value;
                            });
                            sum = sum + 100;
                            console.log(sum);
                            scope.gtc.height = sum;
                        });
                    }
                };
            }])

        .directive('tabComponent', [function () {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        index: '@index',
                        workspaces: '=workspaces'
                    },
                    templateUrl: 'views/main.html'
                };
            }])

        .directive('dropComponent', [function () {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                        list: '=list',
                        size: '@size',
                        gtc: '=gtc'
                    },
                    link: function (scope, elem, attrs) {
                        scope.$watch(function () {
                            elem.attr('style', 'height: ' + (scope.gtc.height) + 'px !important');
                        });
                    },
                    templateUrl: 'views/drop.html'
                };
            }])

        .directive('dropSecComponent', [function () {
                return {
                    link: function (scope, elem, attrs) {
                        scope.$watch(function () {
                            elem.attr('style', 'height: ' + (scope.gtc.height) + 'px !important');
                        });
                    }
                };
            }])

        .controller('MainCtrl', ['$scope', 'ServiceControllerService', function ($scope, ServiceControllerService) {

                $scope.generallist = [];

                var setAllInactive = function () {
                    angular.forEach($scope.workspaces, function (workspace) {
                        workspace.active = false;
                    });
                };

                var addNewWorkspace = function () {
                    var id = $scope.workspaces.length + 1;
                    $scope.workspaces.push({
                        id: id,
                        name: "Workspace " + id,
                        active: true
                    });
                };

                $scope.workspaces =
                        [
                            {id: 1, name: "Workspace 1", active: true},
                            {id: 2, name: "Workspace 2", active: false}
                        ];

                $scope.addWorkspace = function () {
                    setAllInactive();
                    addNewWorkspace();
                };

                $scope.generallistgtc = {};
                $scope.generallistgtc.height = 280;
            }])

        .controller('TabsChildCtrl', ['$scope', 'ServiceControllerService', 'HelperService', function ($scope, ServiceControllerService, HelperService) {

                console.log("scope,workspaces", $scope.workspaces);
                console.log("scope,index", $scope.index);

                if (!HelperService.isUndefinedOrNull($scope.index)) {
                    $scope.workspace = $scope.workspaces[$scope.index];
//                    $scope.workspace.id = $scope.workspaces[$scope.index].id;
//                    $scope.workspace.name = $scope.workspaces[$scope.index].name;
                }

                $scope.list1 = [];
                $scope.list2 = [];
                $scope.list3 = [];
                $scope.list4 = [];

                $scope.list5 = [
                    {'name': 'sera', 'title': 'Item 1', 'drag': true, 'height': 30, 'type': 'clock'},
                    {'name': 'serb', 'title': 'Item 2', 'drag': true, 'height': 30, 'type': 'arduino'},
                    {'name': 'serc', 'title': 'Item 3', 'drag': true, 'height': 30, 'type': 'clock'},
                    {'name': 'serd', 'title': 'Item 4', 'drag': true, 'height': 30, 'type': 'arduino'},
                    {'name': 'sere', 'title': 'Item 5', 'drag': true, 'height': 30, 'type': 'clock'},
                    {'name': 'serf', 'title': 'Item 6', 'drag': true, 'height': 30, 'type': 'arduino'},
                    {'name': 'serg', 'title': 'Item 7', 'drag': true, 'height': 30, 'type': 'clock'},
                    {'name': 'serh', 'title': 'Item 8', 'drag': true, 'height': 30, 'type': 'arduino'},
                    {'name': 'seri', 'title': 'Item 9', 'drag': true, 'height': 30, 'type': 'clock'},
                    {'name': 'serj', 'title': 'Item 10', 'drag': true, 'height': 30, 'type': 'arduino'},
                    {'name': 'serk', 'title': 'Item 11', 'drag': true, 'height': 30, 'type': 'clock'},
                    {'name': 'serl', 'title': 'Item 12', 'drag': true, 'height': 30, 'type': 'arduino'},
                    {'name': 'serm', 'title': 'Item 13', 'drag': true, 'height': 30, 'type': 'clock'},
                    {'name': 'sern', 'title': 'Item 14', 'drag': true, 'height': 30, 'type': 'arduino'},
                    {'name': 'sero', 'title': 'Item 15', 'drag': true, 'height': 30, 'type': 'clock'}
                ];

                $scope.list1gtc = {};
                $scope.list1gtc.height = 280;

                $scope.list2gtc = {};
                $scope.list2gtc.height = 280;

                $scope.list3gtc = {};
                $scope.list3gtc.height = 280;

                $scope.list4gtc = {};
                $scope.list4gtc.height = 280;

                $scope.list5gtc = {};
                $scope.list5gtc.height = 280;

                // Limit items to be dropped in list1
                $scope.optionsList1 = {
                    accept: function (dragEl) {
                        if ($scope.list1.length >= 2) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                };
            }]);