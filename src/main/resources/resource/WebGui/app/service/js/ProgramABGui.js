angular.module('mrlapp.service.ProgramABGui', [])
.controller('ProgramABGuiCtrl', ['$scope', '$log', 'mrl', '$uibModal', '$sce', function($scope, $log, mrl, $uibModal, $sce) { // $modal ????
    $log.info('ProgramABGuiCtrl')
    // grab the self and message
    var _self = this
    var startDialog = null
    var msg = this.msg
    
    // use $scope only when the variable
    // needs to interract with the display
    $scope.currentUserName =  ''
    $scope.utterance = ''

    $scope.currentBotImage = null;

    // grab defaults.
    $scope.newUserName = $scope.service.currentUserName

    // start info status
    $scope.chatLog = []

    // start info status
    $scope.log = []
    
    // following the template.
    this.updateState = function(service) {
        // use another scope var to transfer/merge selection
        // from user - service.currentSession is always read-only
        // all service data should never be written to, only read from
        $scope.currentUserName = service.currentUserName
        $scope.service = service

        /*
        for (let bot in $scope.service.sessions){
            for (let username in $scope.service.sessions[bot]){
                console.info(username)
            }
        }
        */

    }
    
    
    this.onMsg = function(inMsg) {
        $log.info("ProgramABGui.onMsg(" + inMsg.method +')')
        let data = inMsg.data[0]

        switch (inMsg.method) {
        
        case 'onBotImage':
            $scope.currentBotImage = data;
            $scope.$apply()
            break

        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onRequest':
            var textData = data
            $scope.chatLog.unshift({
                name: $scope.currentUserName,
                text: $sce.trustAsHtml(textData)
            })
            $log.info('onRequest', textData)
            $scope.$apply()
            break
        case 'onText':
            var textData = data
            $scope.chatLog.unshift({
                name: $scope.service.currentBotName,
                text: $sce.trustAsHtml(textData)
            })
            $log.info('onText', textData)
            $scope.$apply()
            break
        case 'onLog':
            var textData = data
            $scope.log.unshift({
                name: '',
                text: $sce.trustAsHtml(textData)
            })
            //$log.info('currResponse', textData)
            $scope.$apply()
            break
        case 'onOOBText':
            var textData = data
            $scope.chatLog.unshift({
                name: " > oob <",
                text: $sce.trustAsHtml(textData)
            })
            $log.info('currResponse', textData)
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }


    $scope.getBotInfo = function() {
        return $scope.service.bots[$scope.service.currentBotName]
    }

    $scope.getCurrentSession = function() {
        return $scope.service.sessions[$scope.getCurrentSessionKey()]
    }
    
    $scope.getCurrentSessionKey = function(){
        return $scope.service.currentUserName + ' <-> ' + $scope.service.currentBotName
    }

    $scope.test = function(session, utterance) {
        msg.send("getCategories","hello")
    }
    
    
    $scope.getSessionResponse = function(utterance) {
    	$log.info("SESSION GET RESPONSE (" + $scope.currentUserName + " " + $scope.service.currentBotName + ")")
    	$scope.getResponse($scope.currentUserName, $scope.service.currentBotName, utterance)
    }
    
    $scope.getResponse = function(username, botname, utterance) {
    	$log.info("USER BOT RESPONSE (" + username + " " + botname + ")")
        msg.send("getResponse", username, botname, utterance)
        $scope.utterance = ""
    }
    
    $scope.startDialog = function() {
        startDialog = $uibModal.open({
            templateUrl: "startDialog.html",
            scope: $scope,
            controller: function($scope) {
                $scope.cancel = function() {
                    startDialog.dismiss()
                }
                
            
            }
        })
    }
    
    $scope.startSession = function(username, botname) {
        $scope.currentUserName = username
    	$scope.chatLog.unshift("Reload Session for Bot " + botname)
        $scope.startSessionLabel = 'Reload Session'
        msg.send("startSession", username, botname)
        startDialog.dismiss()
    }
    
    $scope.savePredicates = function() {
        $scope.service = mrl.getService($scope.service.name)
        mrl.sendTo($scope.service.name, "savePredicates")
    }
    
    // subscribe to the response from programab.
    msg.subscribe('publishRequest')
    msg.subscribe('publishText')
    msg.subscribe('publishLog')
    msg.subscribe('publishOOBText')
    msg.subscribe(this)
}
])