angular.module('mrlapp.service', ['ngDragDrop'])

        .directive('serviceBody', [function () {
                return {
                    scope: {
                        //index: '@index',
                        //list: '=list'
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

        .controller('ServiceCtrl', ['$scope', 'ServiceControllerService', function ($scope, ServiceControllerService) {
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
                    $scope.inst.size = $scope.size.substring(5, $scope.size.length);
                    $scope.inst.forcesize = true;
                } else {
                    $scope.inst.forcesize = false;
                }
                if (!$scope.inst.size) {
                    $scope.inst.size = "medium";
                }
                //TODO: add whatever service-specific functions are needed (init, ...)
                //attachGUI(), detachGUI(), send(method, data), sendTo(name, method, data),
                //subscribe(inMethod, outMethod), subscribeTo(publisherName, inMethod, outMethod),
                //key(inStr), releaseService(), serviceGUIInit(), broadcastState()
                //END_specific Service-Initialisation

                $scope.changesize = function (size) {
                    console.log("button clicked", size);
                    if (size != "full") {
                        $scope.inst.size = size;
                    } else {
                        alert('Not yet');
                        //TODO - full: https://angular-ui.github.io/bootstrap/#modal , large
                    }
                };
            }]);