angular.module('mrlapp.service', ['ngDragDrop'])

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
                $scope.title = $scope.list[$scope.index].title;
                $scope.drag = $scope.list[$scope.index].drag;
                $scope.type = $scope.list[$scope.index].type;

                //START_specific Service-Initialisation
                //"inst" is given to the specific service-UI
                $scope.inst = ServiceControllerService.getServiceInst($scope.name);
                if ($scope.inst == null) {
                    $scope.inst = {};
                    ServiceControllerService.addService($scope.name, $scope.inst);
                }
                console.log("$scope,size", $scope.size);
                if ($scope.size != null && $scope.size.lastIndexOf("force", 0) == 0) {
                    $scope.inst.oldsize = $scope.inst.size;
                    $scope.inst.size = $scope.size.substring(5, $scope.size.length);
                    $scope.inst.forcesize = true;
                } else {
                    if ($scope.inst.oldsize != null) {
                        $scope.inst.size = $scope.inst.oldsize;
                        $scope.inst.oldsize = null;
                    }
                    $scope.inst.forcesize = false;
                }
                if (!$scope.inst.size) {
                    $scope.inst.size = "medium";
                    $scope.inst.oldsize = null;
                }
                //TODO: add whatever service-specific functions are needed (init, ...)
                //attachGUI(), detachGUI(), send(method, data), sendTo(name, method, data),
                //subscribe(inMethod, outMethod), subscribeTo(publisherName, inMethod, outMethod),
                //key(inStr), releaseService(), serviceGUIInit(), broadcastState()
                //END_specific Service-Initialisation

                $scope.changesize = function (size) {
                    console.log("button clicked", size);
                    $scope.inst.oldsize = $scope.inst.size;
                    $scope.inst.size = size;
                    if (size == "full") {
                        //alert('Not yet');
                        //TODO - full: https://angular-ui.github.io/bootstrap/#modal , large
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
                                inst: function () {
                                    return $scope.inst;
                                }
                            }
                        });
                        modalInstance.result.then(function () {
                            $scope.inst.size = $scope.inst.oldsize;
                            $scope.inst.oldsize = null;
                        });
                    }
                };
            }])

        .controller('ServiceFullCtrl', function ($scope, $modalInstance, name, type, inst) {

            $scope.name = name;
            $scope.type = type;
            $scope.inst = inst;
            
            $scope.modal = true;
            
            console.log("servicefullctrl", $scope.name, $scope.type, $scope.inst);
            
            $scope.close = function () {
                $modalInstance.close();
            };
        });