angular.module('mrlapp.service.Hd44780Gui', [])
.controller('Hd44780GuiCtrl', ['$scope', '$log', 'mrl', '$uibModal', '$sce', function($scope, $log, mrl, $uibModal, $sce) { // $modal ????
    $log.info('Hd44780GuiCtrl');
    // grab the self and message
    var _self = this;
    var msg = this.msg;
    
    // use $scope only when the variable
    // needs to interract with the display
    $scope.backLight =  false;
    $scope.screenContent =  '';

    // start info status
    $scope.rows = [];
    
    // following the template.
    this.updateState = function(service) {
        // use another scope var to transfer/merge selection
        // from user - service.currentSession is always read-only
        // all service data should never be written to, only read from
        $scope.screenContent =  '';
        Object.keys(service.screenContent).forEach(function(key) {
        $scope.screenContent+=service.screenContent[key].trim()+'\n';
        //console.log(service.screenContent[key]);
        });
        $scope.backLight = service.backLight;
        $scope.service = service;
    }
    ;
    
    this.onMsg = function(inMsg) {
        $log.info("Hd44780 Msg !");
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0]);
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
        ;
    }
    ;

    $scope.setBackLight = function(status) {
        
        msg.send("setBackLight", status)
    };  

    $scope.display = function(textArea) {
        console.log('setBackLicght');
        msg.send("clear");
        var lines = textArea.split('\n');
        for(var i = 0;i < lines.length && i<4;i++){
            msg.send("display", lines[i], i+1)
            //$log.info("Hd44780 Msg send !");
        }
    };      
    // subscribe to the response
    msg.subscribe(this);
}
]);