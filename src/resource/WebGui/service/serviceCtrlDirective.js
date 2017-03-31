angular.module('mrlapp.service').directive('serviceCtrlDirective', ['$compile', '$log', 'mrl', 'panelSvc', function($compile, $log, mrl, panelSvc) {
    return {
        scope: {
            //"=" -> binding to items in parent-scope specified by attribute
            //"@" -> using passed attribute
            panel: '=panel'
        },
        link: function(scope, elem, attr) {
            scope.service = mrl.getService(scope.panel.name);
            var isUndefinedOrNull = function(val) {
                return angular.isUndefined(val) || val === null;
            };
            scope.panelconfig = {};
            //prepare dynamic controller injection
            var html = '<div service-ctrl-next ' + 'controller-name="' + scope.panel.simpleName + 'GuiCtrl" ' + 'name="panel.name" ' + 'service="service" ' + 'msginterface="msginterface" ' + 'msgmethods="msgmethods" ' + 'panelconfig="panelconfig" ' + 'size="panel.size" cb="cb"' + '></div>';
            var watch = scope.$watch(function() {
                return scope.panel.templatestatus;
            }, function() {
                if (!isUndefinedOrNull(scope.panel.templatestatus) && scope.panel.templatestatus == 'loaded') {
                    watch();
                    $log.info('deps loaded, start ctrl', scope.panel.name);
                    mrl.createMsgInterface(scope.panel.name).then(function(msg_) {
                        $log.info('msgInterface received', scope.panel.name);
                        scope.panel.msg_ = msg_;
                        scope.msginterface = msg_;
                        scope.msgmethods = msg_.temp.msg;
                        elem.html(html).show();
                        $compile(elem.contents())(scope);
                    }, function(msg_) {
                        console.log('msgInterface-meh!');
                    });
                }
            });
        }
    };
}
]).directive('serviceCtrlNext', ['mrl', 'panelSvc', function(mrl, panelSvc) {
    //dynamic controller
    return {
        scope: {
            msg: '=msgmethods',
            name: '=',
            service: '=',
            //Does it make sense to give him an instance of itself that may be outdated in just a bit? Or let it fetch it's instance himself`?
            size: '='
        },
        bindToController: {
            panelconfig: '=',
            msg: '=msginterface'
        },
        controller: "@",
        controllerAs: "guictrl",
        name: "controllerName",
        link: function(scope, elem, attr) {
            console.log(scope.name, 'serviceCtrlNext-link');
            panelSvc.controllerscope(scope.name, scope);
        }
    };
}
]);
