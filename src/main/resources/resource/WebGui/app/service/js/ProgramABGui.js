angular.module('mrlapp.service.ProgramABGui', []).controller('ProgramABGuiCtrl', ['$scope', '$compile', 'mrl', '$uibModal', '$sce', function($scope, $compile, mrl, $uibModal, $sce) {
    // $modal ????
    console.info('ProgramABGuiCtrl')
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
    $scope.predicates = []
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

    $scope.chatLog = []

    // start info status
    $scope.log = []

    // following the template.
    this.updateState = function(service) {
        // use another scope var to transfer/merge selection
        // from user - service.currentSession is always read-only
        // all service data should never be written to, only read from
        $scope.currentUserName = service.config.currentUserName
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
        // console.info("ProgramABGui.onMsg(" + inMsg.method + ')')
        let data = inMsg.data[0]

        switch (inMsg.method) {

        case 'onStatus':
            $scope.status = data;
            $scope.$apply()
            break

        case 'onBotImage':
            $scope.currentBotImage = data
            $scope.$apply()
            break

        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break

        case 'onTopic':
            $scope.service.currentTopic = data
            $scope.$apply()
            break

        case 'onAimlFile':
            $scope.aimlFileData.data = data
            $scope.$apply()
            break

        case 'onPredicates':
            $scope.predicates = data
            $scope.$apply()
            break

        case 'onPredicate':
            $scope.predicates[data.name] = data.value
            $scope.$apply()
            break
                

        case 'onRequest':
            var textData = data
            $scope.chatLog.unshift({
                type: 'User',
                name: $scope.currentUserName,
                text: $sce.trustAsHtml(textData)
            })
            console.info('onRequest', textData)
            $scope.$apply()
            break
        case 'onResponse':
            var textData = data
            $scope.chatLog.unshift({
                type: 'Bot',
                name: $scope.service.config.currentBotName,
                text: $sce.trustAsHtml(data.msg)
            })
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
            console.info('currResponse', textData)
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.getAimlFile = function(filename) {
        $scope.aimlFile = filename
        console.log('getting aiml file ' + filename)
        msg.send('getAimlFile', $scope.service.config.currentBotName, filename)
        $scope.tabs.selected = 2
    }

    $scope.saveAimlFile = function() {
        msg.send("saveAimlFile", $scope.service.config.currentBotName, $scope.aimlFile, $scope.aimlFileData.data)
    }

    $scope.setSessionKey = function() {
        msg.send("setCurrentUserName", $scope.service.config.currentUserName)
        msg.send("setCurrentBotName", $scope.service.config.currentBotName)
    }

    $scope.getBotInfo = function() {
        if ($scope.service && $scope.service.bots){
            return $scope.service.bots[$scope.service.config.currentBotName]
        }
        return null
    }

    $scope.getCurrentSession = function() {
        if (!$scope.service.sessions){
            return null
        }
        if ($scope.getCurrentSessionKey()in $scope.service.sessions) {
            return $scope.service.sessions[$scope.getCurrentSessionKey()]
        }
        return null
    }

    $scope.getCurrentSessionKey = function() {
        return $scope.service.config.currentUserName + ' <-> ' + $scope.service.config.currentBotName
    }

    $scope.test = function(session, utterance) {
        msg.send("getCategories", "hello")
    }

    $scope.getSessionResponse = function(utterance) {
        console.info("SESSION GET RESPONSE (" + $scope.currentUserName + " " + $scope.service.config.currentBotName + ")")
        $scope.getResponse($scope.currentUserName, $scope.service.config.currentBotName, utterance)
    }

    $scope.getResponse = function(username, botname, utterance) {
        console.info("USER BOT RESPONSE (" + username + " " + botname + ")")
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
        if (!$scope.getBotInfo()){
            return null
        }
        return $scope.getBotInfo()['properties']
    }

    $scope.getProperty = function(propName) {
        try {
            if ($scope.getBotInfo() && $scope.getBotInfo()['properties']){
                return $scope.getBotInfo()['properties'][propName]                
            }
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

    $scope.getBotPath = function(e) {
        if ($scope.service?.bots && $scope.service?.bots[$scope.service?.config?.currentBotName]?.path){
            return $scope.service?.bots[$scope.service?.config.currentBotName].path
        }
        return null
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
    msg.subscribe('publishTopic')
    msg.subscribe('publishRequest')
    msg.subscribe('publishResponse')
    msg.subscribe('publishLog')
    msg.subscribe('publishOOBText')
    msg.subscribe('getPredicates')
    msg.subscribe('publishPredicate')
    msg.subscribe('getAimlFile')


    msg.send('getPredicates')
    
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
