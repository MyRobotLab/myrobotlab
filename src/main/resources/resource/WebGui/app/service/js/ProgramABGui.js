angular.module('mrlapp.service.ProgramABGui', []).controller('ProgramABGuiCtrl', ['$scope', '$compile', '$log', 'mrl', '$uibModal', '$sce', function($scope, $compile, $log, mrl, $uibModal, $sce) {
    // $modal ????
    $log.info('ProgramABGuiCtrl')
    // grab the self and message
    var _self = this
    var startDialog = null
    var msg = this.msg

    // use $scope only when the variable
    // needs to interract with the display
    $scope.currentUserName = ''
    $scope.utterance = ''
    $scope.currentSessionKey = null
    $scope.status = null

    $scope.currentBotImage = null

    $scope.aimlEditor = null

    $scope.tabs = {
        "selected": 1
    }
    $scope.tabsRight = {
        "selected": 1
    }

    // active tab index
    // $scope.active = 0

    $scope.aimlFile = "<category>blah \n blah <category>"
    $scope.aimlFileData = {
        "data": "HELLO THERE !!!"
    }

    $scope.lastResponse

    // an angularjs necessary evil - booleans must
    // be put in an object to be effectively modified
    // when $sope is used
    $scope.edit = {
        properties: false
    }

    // grab defaults.
    $scope.newUserName = $scope.service.currentUserName

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
        $scope.currentSessionKey = $scope.getCurrentSessionKey()

        /*
        for (let bot in $scope.service.sessions){
            for (let username in $scope.service.sessions[bot]){
                console.info(username)
            }
        }
        */

    }

    this.onMsg = function(inMsg) {
        $log.info("ProgramABGui.onMsg(" + inMsg.method + ')')
        let data = inMsg.data[0]

        switch (inMsg.method) {

        case 'onStatus':
            $scope.status = data;
            $scope.$apply()
            break

        case 'onBotImage':
            $scope.currentBotImage = data;
            $scope.$apply()
            break

        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break

        case 'onAimlFile':
            $scope.aimlFileData.data = data
            $scope.$apply()
            break

        case 'onRequest':
            var textData = data
            $scope.chatLog.unshift({
                type: 'User',
                name: $scope.currentUserName,
                text: $sce.trustAsHtml(textData)
            })
            $log.info('onRequest', textData)
            $scope.$apply()
            break
        case 'onText':
            var textData = data
            $scope.chatLog.unshift({
                type: 'Bot',
                name: $scope.service.currentBotName,
                text: $sce.trustAsHtml(textData)
            })
            $log.info('onText', textData)
            $scope.lastResponse = textData
            $scope.$apply()
            break
        case 'onLog':
            var textData = data
            let filename = null
            parts = textData.split(" ")
            if (parts.length > 2 && parts[1] == "Matched:") {
                filename = parts[parts.length - 1]
                textData = textData.substr(0, textData.lastIndexOf(' ') + 1)
                // pos0 = textData.lastIndexOf(' ') + 1
                // url = textData.substr(0,pos0) + '<button class="btn btn-xs btn-info" ng-click="msg.getAimlFile(\'' + filename + '\')">' + filename + '</button>'
                // url = textData.substr(0,pos0) + '<button class="btn btn-xs btn-info" ng-click="test()">' + filename + '</button>'
                // textData = url
            }

            $scope.log.unshift({
                'name': '',
                'text': textData,
                'filename': filename
            })

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

    $scope.getAimlFile = function(filename) {
        $scope.aimlFile = filename
        console.log('getting aiml file ' + filename)
        msg.send('getAimlFile', $scope.service.currentBotName, filename)
        $scope.tabs.selected = 2
    }

    $scope.saveAimlFile = function() {
        msg.send("saveAimlFile", $scope.service.currentBotName, $scope.aimlFile, $scope.aimlFileData.data)
    }

    $scope.setSessionKey = function() {
        msg.send("setCurrentUserName", $scope.service.currentUserName)
        msg.send("setCurrentBotName", $scope.service.currentBotName)
    }

    $scope.getBotInfo = function() {
        if ($scope.service && $scope.service.bots){
            return $scope.service.bots[$scope.service.currentBotName]
        }
        return null
    }

    $scope.getCurrentSession = function() {
        if ($scope.getCurrentSessionKey()in $scope.service.sessions) {
            return $scope.service.sessions[$scope.getCurrentSessionKey()]
        }
        return null
    }

    $scope.getCurrentSessionKey = function() {
        return $scope.service.currentUserName + ' <-> ' + $scope.service.currentBotName
    }

    $scope.test = function(session, utterance) {
        msg.send("getCategories", "hello")
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
        // FIXME !!!! lame
    }

    $scope.getProperties = function() {
        return $scope.getBotInfo()['properties']
    }

    $scope.getProperty = function(propName) {
        try {
            return $scope.getBotInfo()['properties'][propName]
        } catch (error){
            console.warn('getProperty(' + propName + ') not found')
            return null
        }
    }

    $scope.removeBotProperty = function(propName) {
        delete $scope.getBotInfo()['properties'][propName]
        msg.send("removeBotProperty", propName)
    }

    $scope.aceLoaded = function(_editor) {
        // _editor.setReadOnly(true);
        $scope.aimlEditor = _editor
        console.log('aceLoaded')
    }

    $scope.aceChanged = function(e) {
        //
        console.log('aceChanged')
    }

    $scope.getStatusLabel = function(level) {
        if (level == 'error') {
            return 'row label col-md-12 label-danger'
        }
        if (level == 'warn') {
            return 'row label col-md-12 label-warning'
        }

        return 'row label col-md-12 label-info'
    }

    // subscribe to the response from programab.
    msg.subscribe('publishRequest')
    msg.subscribe('publishText')
    msg.subscribe('publishLog')
    msg.subscribe('publishOOBText')
    msg.subscribe('getAimlFile')
    msg.subscribe(this)
}
])

/* .filter('orderObjectBy', function() {
  return function(items, field, reverse) {
    var filtered = [];
    angular.forEach(items, function(item, key) {
      filtered.push({'key':key, 'value':item});
    });
    
    filtered.sort(function (a, b) {
      return (a[field] > b[field] ? 1 : -1);
    });
    if(reverse) filtered.reverse();
    return filtered;
  };
});

*/
