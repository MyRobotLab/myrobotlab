angular.module('mrlapp.service.TemplateGui', [])
        .controller('TemplateGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function ($scope, $log, mrl, $timeout) {
                $log.info('TemplateGuiCtrl');
                var _self = this;
                var msg = this.msg;
                
                // GOOD TEMPLATE TO FOLLOW
                this.updateState = function (service) {
                    $scope.service = service;
                };
                
                // get fresh copy
                $scope.service = mrl.getService($scope.service.name);
                
                _self.updateState($scope.service);           
                
                //set custom-sizes
                this.panelconfig.setPanelSizes({
                    me1: {
                        sizes: {
                            //size-options, these will be shown as a option to select from
                            //(and can be applied)
                            tiny: {
                                glyphicon: 'glyphicon glyphicon-minus', //define a glyphicon to show (as a symbol)
                                width: 200, //width of this size-setting
                                body: 'collapse', //means that the body-section of the panel won't be shown
                                footer: 'collapse'//don't show footer-section of panel
                            },
                            small: {
                                glyphicon: 'glyphicon glyphicon-resize-small',
                                width: 400
                            },
                            large: {
                                glyphicon: 'glyphicon glyphicon-resize-full',
                                width: 800
                            },
                            full: {
                                glyphicon: 'glyphicon glyphicon-fullscreen',
                                width: 0,
                                fullscreen: true, //show fullscreen (modal)
                                body: 'collapse',
                                footer: 'collapse'
                            },
                            free: {
                                glyphicon: 'glyphicon glyphicon-resize-horizontal',
                                width: 800,
                                freeform: true //allow free-form resizing (width)
                            }
                        },
                        order: ["free", "full", "large", "small", "tiny"], //shows your size-options in _self order
                        aktsize: 'large'//set this as the start-/default-size
                    },
                    me2: {/*...*/},
                    me3: {/*...*/}});
                
                //init variables you always need
               $scope.pulseData = '';

                //you HAVE TO define this method
                //-> you will receive all messages routed to your service here
                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
                        case 'onState':
                            $timeout(function () {
                                _self.updateState(inMsg.data[0]);
                            });
                            break;
                        case 'onPulse':
                            $timeout(function () {
                                $scope.pulseData = inMsg.data[0];
                            });
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };
                
                //to send a message to the service, use:
                //$scope.msg.<serviceFunction>();
                $scope.msg.startClock();

                //subscribe to functions and to the service
                msg.subscribe('pulse');
                msg.subscribe(this);
            }]);
