angular.module('mrlapp.service.AzureTranslatorGui', []).controller('AzureTranslatorGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('AzureTranslatorGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.locations = ["eastus", "eastus2", "southcentralus", "westus2", "westus3", "australiaeast", "southeastasia", "northeurope", "swedencentral", "uksouth", "westeurope", "centralus", "northcentralus", "westus", "southafricanorth", "centralindia", "eastasia", "japaneast", "jioindiawest", "koreacentral", "canadacentral", "francecentral", "germanywestcentral", "norwayeast", "switzerlandnorth", "uaenorth", "brazilsouth", "centralusstage", "eastusstage", "eastus2stage", "northcentralusstage", "southcentralusstage", "westusstage", "westus2stage", "asia", "asiapacific", "australia", "brazil", "canada", "europe", "france", "germany", "global", "india", "japan", "korea", "norway", "southafrica", "switzerland", "uae", "uk", "unitedstates", "unitedstateseuap", "eastasiastage", "southeastasiastage", "centraluseuap", "eastus2euap", "westcentralus", "southafricawest", "australiacentral", "australiacentral2", "australiasoutheast", "japanwest", "jioindiacentral", "koreasouth", "southindia", "westindia", "canadaeast", "francesouth", "germanynorth", "norwaywest", "switzerlandwest", "ukwest", "uaecentral", "brazilsoutheast"]

    $scope.translatedText = ''
    $scope.languageDetected = ''
    $scope.key = ''

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    // init scope variables
    $scope.translatedText = ''

    this.onMsg = function(inMsg) {
        data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onDetectedLanguage':
            $scope.detectTitle = 'detected'
            $scope.languageDetected = data
            $scope.$apply()
            break
        case 'onText':
            $scope.translatedText = data
            $scope.$apply()
            break
        case 'onKey':
            $scope.key = data
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + data)
            break
        }
    }

    $scope.setFrom = function(from) {
        msg.send('setFrom', from)
    }

    $scope.setTo = function(to) {
        msg.send('setTo', to)
    }

    msg.subscribe("getKey")
    msg.subscribe("publishDetectedLanguage")
    msg.subscribe("publishText")
    msg.send('getKey')
    msg.subscribe(this)

}
])
