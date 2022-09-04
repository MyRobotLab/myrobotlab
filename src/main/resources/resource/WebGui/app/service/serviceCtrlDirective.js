angular.module('mrlapp.service').directive('serviceCtrlDirective', ['$compile', 'mrl', function($compile, mrl) {
    return {
        scope: {
            //"=" -> binding to items in parent-scope specified by attribute
            //"@" -> using passed attribute
            panel: '=panel'
        },
        link: function(scope, elem, attr) {
            scope.service = mrl.getService(scope.panel.name);
            scope.panelconfig = {};

            // ACTUAL SCOPE IS CREATED IN serviceCtrlNext DIRECTIVE !!!!
            //prepare dynamic controller injection
            var html = '<div service-ctrl-next ' + 'controller-name="' + scope.panel.simpleName + 'GuiCtrl" ' + 'name="panel.name" ' + 'service="service" ' + 'mrl="mrl" ' + 'msginterface="msginterface" ' + 'msgmethods="msgmethods" ' + 'panelconfig="panelconfig" ' + 'size="panel.size" cb="cb"' + '></div>';
            var watch = scope.$watch(function() {
                return scope.panel.templatestatus;
            }, function() {
                if (scope.panel.templatestatus && scope.panel.templatestatus == 'loaded') {
                    watch();
                    console.info('deps loaded, start ctrl', scope.panel.name);
                    mrl.createMsgInterface(scope.panel.name).then(function(msg_) {
                        console.info('==== msgInterface received', scope.panel.name);
                        scope.panel.msg_ = msg_;
                        scope.msginterface = msg_;
                        scope.msgmethods = msg_.temp.msg;
                        scope.mrl = mrl
                        elem.html(html).show();
                        console.info("elem.contents")
                        // console.info(elem.contents())
                        
                        $compile(elem.contents())(scope);
                        
                    }, function(msg_) {
                        console.log('msgInterface-meh!');
                    });
                }
            });
        }
    };
}
]).directive('serviceCtrlNext', ['mrl', function(mrl) {
    //dynamic controller
    return {
        scope: {
            msg: '=msgmethods',
            name: '=',
            service: '=',
            //Does it make sense to give him an instance of itself that may be outdated in just a bit? Or let it fetch it's instance himself`?
            size: '=',
            mrl:'=mrl'
        },
        bindToController: {
            panelconfig: '=',
            msg: '=msginterface'
        },
        controller: "@",
        controllerAs: "guictrl",
        name: "controllerName",
        link: function(scope, elem, attr) {
            console.log(scope.name, '==== serviceCtrlNext-link');
            mrl.controllerscope(scope.name, scope);
        }
    };
}
]);
