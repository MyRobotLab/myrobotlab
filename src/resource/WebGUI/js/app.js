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
                
                //TODO: populate list dynamically & with the services, not states & flages
                $scope.allServices = [{'name':'Alabama','flag':'5/5c/Flag_of_Alabama.svg/45px-Flag_of_Alabama.svg.png'},{'name':'Alaska','flag':'e/e6/Flag_of_Alaska.svg/43px-Flag_of_Alaska.svg.png'},{'name':'Arizona','flag':'9/9d/Flag_of_Arizona.svg/45px-Flag_of_Arizona.svg.png'},{'name':'Arkansas','flag':'9/9d/Flag_of_Arkansas.svg/45px-Flag_of_Arkansas.svg.png'},{'name':'California','flag':'0/01/Flag_of_California.svg/45px-Flag_of_California.svg.png'},{'name':'Colorado','flag':'4/46/Flag_of_Colorado.svg/45px-Flag_of_Colorado.svg.png'},{'name':'Connecticut','flag':'9/96/Flag_of_Connecticut.svg/39px-Flag_of_Connecticut.svg.png'},{'name':'Delaware','flag':'c/c6/Flag_of_Delaware.svg/45px-Flag_of_Delaware.svg.png'},{'name':'Florida','flag':'f/f7/Flag_of_Florida.svg/45px-Flag_of_Florida.svg.png'},{'name':'Georgia','flag':'5/54/Flag_of_Georgia_%28U.S._state%29.svg/46px-Flag_of_Georgia_%28U.S._state%29.svg.png'},{'name':'Hawaii','flag':'e/ef/Flag_of_Hawaii.svg/46px-Flag_of_Hawaii.svg.png'},{'name':'Idaho','flag':'a/a4/Flag_of_Idaho.svg/38px-Flag_of_Idaho.svg.png'},{'name':'Illinois','flag':'0/01/Flag_of_Illinois.svg/46px-Flag_of_Illinois.svg.png'},{'name':'Indiana','flag':'a/ac/Flag_of_Indiana.svg/45px-Flag_of_Indiana.svg.png'},{'name':'Iowa','flag':'a/aa/Flag_of_Iowa.svg/44px-Flag_of_Iowa.svg.png'},{'name':'Kansas','flag':'d/da/Flag_of_Kansas.svg/46px-Flag_of_Kansas.svg.png'},{'name':'Kentucky','flag':'8/8d/Flag_of_Kentucky.svg/46px-Flag_of_Kentucky.svg.png'},{'name':'Louisiana','flag':'e/e0/Flag_of_Louisiana.svg/46px-Flag_of_Louisiana.svg.png'},{'name':'Maine','flag':'3/35/Flag_of_Maine.svg/45px-Flag_of_Maine.svg.png'},{'name':'Maryland','flag':'a/a0/Flag_of_Maryland.svg/45px-Flag_of_Maryland.svg.png'},{'name':'Massachusetts','flag':'f/f2/Flag_of_Massachusetts.svg/46px-Flag_of_Massachusetts.svg.png'},{'name':'Michigan','flag':'b/b5/Flag_of_Michigan.svg/45px-Flag_of_Michigan.svg.png'},{'name':'Minnesota','flag':'b/b9/Flag_of_Minnesota.svg/46px-Flag_of_Minnesota.svg.png'},{'name':'Mississippi','flag':'4/42/Flag_of_Mississippi.svg/45px-Flag_of_Mississippi.svg.png'},{'name':'Missouri','flag':'5/5a/Flag_of_Missouri.svg/46px-Flag_of_Missouri.svg.png'},{'name':'Montana','flag':'c/cb/Flag_of_Montana.svg/45px-Flag_of_Montana.svg.png'},{'name':'Nebraska','flag':'4/4d/Flag_of_Nebraska.svg/46px-Flag_of_Nebraska.svg.png'},{'name':'Nevada','flag':'f/f1/Flag_of_Nevada.svg/45px-Flag_of_Nevada.svg.png'},{'name':'New Hampshire','flag':'2/28/Flag_of_New_Hampshire.svg/45px-Flag_of_New_Hampshire.svg.png'},{'name':'New Jersey','flag':'9/92/Flag_of_New_Jersey.svg/45px-Flag_of_New_Jersey.svg.png'},{'name':'New Mexico','flag':'c/c3/Flag_of_New_Mexico.svg/45px-Flag_of_New_Mexico.svg.png'},{'name':'New York','flag':'1/1a/Flag_of_New_York.svg/46px-Flag_of_New_York.svg.png'},{'name':'North Carolina','flag':'b/bb/Flag_of_North_Carolina.svg/45px-Flag_of_North_Carolina.svg.png'},{'name':'North Dakota','flag':'e/ee/Flag_of_North_Dakota.svg/38px-Flag_of_North_Dakota.svg.png'},{'name':'Ohio','flag':'4/4c/Flag_of_Ohio.svg/46px-Flag_of_Ohio.svg.png'},{'name':'Oklahoma','flag':'6/6e/Flag_of_Oklahoma.svg/45px-Flag_of_Oklahoma.svg.png'},{'name':'Oregon','flag':'b/b9/Flag_of_Oregon.svg/46px-Flag_of_Oregon.svg.png'},{'name':'Pennsylvania','flag':'f/f7/Flag_of_Pennsylvania.svg/45px-Flag_of_Pennsylvania.svg.png'},{'name':'Rhode Island','flag':'f/f3/Flag_of_Rhode_Island.svg/32px-Flag_of_Rhode_Island.svg.png'},{'name':'South Carolina','flag':'6/69/Flag_of_South_Carolina.svg/45px-Flag_of_South_Carolina.svg.png'},{'name':'South Dakota','flag':'1/1a/Flag_of_South_Dakota.svg/46px-Flag_of_South_Dakota.svg.png'},{'name':'Tennessee','flag':'9/9e/Flag_of_Tennessee.svg/46px-Flag_of_Tennessee.svg.png'},{'name':'Texas','flag':'f/f7/Flag_of_Texas.svg/45px-Flag_of_Texas.svg.png'},{'name':'Utah','flag':'f/f6/Flag_of_Utah.svg/45px-Flag_of_Utah.svg.png'},{'name':'Vermont','flag':'4/49/Flag_of_Vermont.svg/46px-Flag_of_Vermont.svg.png'},{'name':'Virginia','flag':'4/47/Flag_of_Virginia.svg/44px-Flag_of_Virginia.svg.png'},{'name':'Washington','flag':'5/54/Flag_of_Washington.svg/46px-Flag_of_Washington.svg.png'},{'name':'West Virginia','flag':'2/22/Flag_of_West_Virginia.svg/46px-Flag_of_West_Virginia.svg.png'},{'name':'Wisconsin','flag':'2/22/Flag_of_Wisconsin.svg/45px-Flag_of_Wisconsin.svg.png'},{'name':'Wyoming','flag':'b/bc/Flag_of_Wyoming.svg/43px-Flag_of_Wyoming.svg.png'}];
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