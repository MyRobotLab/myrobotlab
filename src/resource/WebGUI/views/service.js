angular.module('mrlapp.service', [])

        .directive('serviceBody', [function () {
                return {
                    scope: {
                        name: '=name',
                        inst: '=inst'
                    },
                    link: function (scope, elem, attr) {
                        scope.getContentUrl = function () {
                            return 'services/views/' + attr.type + 'gui.html';
                        };
                    },
                    template: '<div ng-include="getContentUrl()"></div>'
                };
            }])

        .controller('ServiceCtrl', ['$scope', '$modal', 'ServiceControllerService', function ($scope, $modal, ServiceControllerService) {
                $scope.name = $scope.list[$scope.index].name;
                $scope.drag = $scope.list[$scope.index].drag;
                $scope.type = $scope.list[$scope.index].type;
                $scope.simpletype = $scope.list[$scope.index].simpletype;

                //START_specific Service-Initialisation
                //"inst" is given to the specific service-UI
                $scope.inst = ServiceControllerService.getServiceInst($scope.name);
                if ($scope.inst == null) {
                    $scope.inst = {};
                    $scope.inst.fw = {}; //framework-section - DO NOT WRITE IN THERE!
                    $scope.inst.data = {}; //data-section
                    $scope.inst.methods = {}; //methods-section
                    ServiceControllerService.addService($scope.name, $scope.inst);
                }
                console.log("$scope,size", $scope.size);
                if ($scope.size != null && $scope.size.lastIndexOf("force", 0) == 0) {
                    $scope.inst.fw.oldsize = $scope.inst.fw.size;
                    $scope.inst.fw.size = $scope.size.substring(5, $scope.size.length);
                    $scope.inst.fw.forcesize = true;
                } else {
                    if ($scope.inst.fw.oldsize != null) {
                        $scope.inst.fw.size = $scope.inst.fw.oldsize;
                        $scope.inst.fw.oldsize = null;
                    }
                    $scope.inst.fw.forcesize = false;
                }
                if (!$scope.inst.fw.size) {
                    $scope.inst.fw.size = "medium";
                    $scope.inst.fw.oldsize = null;
                }
                //TODO: add whatever service-specific functions are needed (init, ...)
                //attachGUI(), detachGUI(), send(method, data), sendTo(name, method, data),
                //subscribe(inMethod, outMethod), subscribeTo(publisherName, inMethod, outMethod),
                //key(inStr), releaseService(), serviceGUIInit(), broadcastState()
                if ($scope.inst.fw.send == null) {
                    $scope.inst.fw.send = function (method, data) {
                        $scope.inst.fw.sendTo($scope.name, method, data);
                    };
                    $scope.inst.fw.sendTo = function (name, method, data) {
                        ServiceControllerService.sendTo(name, method, data);
                    };
                    $scope.inst.fw.subscribe = function (inMethod, outMethod) {
                        $scope.inst.fw.subscribeTo($scope.name, inMethod, outMethod);
                    };
                    $scope.inst.fw.subscribeTo = function (publisherName, inMethod, outMethod) {
                        ServiceControllerService.subscribeTo(publisherName, inMethod, outMethod);
                    };
                }
                //to be overridden (fallback, if not)
                if ($scope.inst.methods.init == null) {
                    $scope.inst.methods.init = function () {
                    };
                }
                if ($scope.inst.methods.attachGUI == null) {
                    $scope.inst.methods.attachGUI = function () {
                    };
                }
                if ($scope.inst.methods.detachGUI == null) {
                    $scope.inst.methods.detachGUI = function () {
                    };
                }
                //END_specific Service-Initialisation

                //footer-size-change-buttons
                $scope.changesize = function (size) {
                    console.log("button clicked", size);
                    $scope.inst.fw.oldsize = $scope.inst.fw.size;
                    $scope.inst.fw.size = size;
                    if (size == "full") {
                        //launch the service as a modal ('full')
                        var modalInstance = $modal.open({
                            animation: true,
                            templateUrl: 'views/servicefulltemplate.html',
                            controller: 'ServiceFullCtrl',
                            size: 'lg',
                            resolve: {
                                name: function () {
                                    return $scope.name;
                                },
                                type: function () {
                                    return $scope.type;
                                },
                                simpletype: function () {
                                    return $scope.simpletype;
                                },
                                inst: function () {
                                    return $scope.inst;
                                }
                            }
                        });
                        //modal closed -> recover to old size
                        modalInstance.result.then(function () {
                            $scope.inst.fw.size = $scope.inst.fw.oldsize;
                            $scope.inst.fw.oldsize = null;
                        });
                    }
                };
            }])

        .controller('ServiceFullCtrl', function ($scope, $modalInstance, name, type, simpletype, inst) {
            //Controller for the modal (service-full)

            $scope.name = name;
            $scope.type = type;
            $scope.simpletype = simpletype;
            $scope.inst = inst;

            $scope.modal = true;

            console.log("servicefullctrl", $scope.name, $scope.type, $scope.simpletype, $scope.inst);

            $scope.close = function () {
                $modalInstance.close();
            };
        });
