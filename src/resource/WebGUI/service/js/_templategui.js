angular.module('mrlapp.service.templategui', [])
        .controller('TemplateGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('TemplateGuiCtrl');

                //clockgui.js is a good example of how a simple service could look like

                //you can store your "data" into $scope.data
                //this is the ONLY persistent data-storage

                //you can call functions on this.
                //& write both you init- & onMsg-functions in it,
                //but NEVER something else

                //do everything you need to do ONCE (per service) in here
                this.init = function () {

                    //set custom panel-names
                    this.setPanelNames(['me1', 'me2', 'me3']);
                    //set if panel-name should be shown or hidden (true->show)
                    this.setPanelShowNames([true, false, true]);
                    //set custom-sizes
                    this.setPanelSizes([
                        {/*panel1*/
                            sizes: {
                                //size-options, these will be shown as a option to select from
                                //(and can be applied)
                                tiny: {
                                    glyphicon: 'glyphicon glyphicon-minus', //define a glyphicon to show
                                    width: 200, //width of this size-setting
                                    body: 'collapse', //means that the body-section of the panel won't be shown
                                    footer: 'collapse' //don't show footer-section of panel
                                },
                                small: {
                                    glyphicon: 'glyphicon glyphicon-resize-small',
                                    width: 300
                                },
                                large: {
                                    glyphicon: 'glyphicon glyphicon-resize-full',
                                    width: 500
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
                                    width: 500,
                                    freeform: true //allow free-form resizing (width)
                                }
                            },
                            order: ["free", "full", "large", "small", "tiny"], //shows your size-options in this order
                            aktsize: 'large' //set this as the start-value
                        },
                        {/*panel2*/},
                        {/*panel3*/}]);

                    //get latest copy of a service
                    //this is your service-object, it is the representation of the service running in mrl
                    $scope.data.service = this.getService();
                    //init variables ...
                    //...

                    //you HAVE TO define this method
                    //-> you will receive all messages routed to your service here
                    this.onMsg = function (msg) {
                        switch (msg.method) {
                            case 'onMethod':
                                //do something
                                break;
                            default:
                                $log.warn("ERROR - unhandled method " + $scope.data.service.name + " " + msg.method);
                                break;
                        }
                    };

                    //define needed method ...
                    //...

                    //Subscriptions
                    this.subscribe('method');
                };

                //simply call this method at the end of your controller (will probably be removed at some point in the future)
                $scope.cb.notifycontrollerisready(this);
            }]);
