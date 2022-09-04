angular.module('ModalController', [])
    .controller('ModalController', ['$scope', '$uibModal', '$uibModalInstance', 'titleValue', 'textValue', 'buttons',
        function ($scope, $uibModal, $uibModalInstance, titleValue, textValue, buttons) {
            $scope.template = 
            $scope.title = titleValue
            $scope.text = textValue
            $scope.buttons = buttons

            $scope.btnClicked = function (callback) {
                $uibModalInstance.close()
                callback()
            }

            $scope.cancel = function () {
                $uibModalInstance.dismiss('cancel')
                if ($scope.buttons.cancelButton) {
                    $scope.buttons.cancelButton.callbackFunction()
                }
            }

        }])

angular.module('modalService', [])
    .service('modalService', ['$uibModal', '$templateCache', 'mrl',
        function ($uibModal, $templateCache, mrl) {

            this.open = function (template, title, text, buttons, scope) {
                if (!buttons) {
                    buttons = {
                        buttonSet: [{
                            buttonText: "OK",
                            callbackFunction: function () {
                                console.info("OK clicked")
                            },
                            buttonStyle: "submit-button"
                        }]
                    }
                }

                $uibModal.open({                    
                    templateUrl: template,
                    controller: 'ModalController',
                    scope: scope,
                    resolve: {
                        titleValue: function () {
                            return title
                        },
                        textValue: function () {
                            return text
                        },
                        buttons: function () {
                            return buttons
                        }
                    },
                    backdrop: 'static', /*  this prevent user interaction with the background  */
                    keyboard: true /* allow or disallow esc from canceling */
                })
            }

            this.openOkCancel = function (template, title, text, onOK, onCancel, scope) {
                var modalWindow = {
                    buttonSet: [{
                        buttonText: "OK",
                        callbackFunction: onOK,
                        buttonStyle: "modal-contect-appealInterface submit-button"
                    }],
                    cancelButton: {
                        buttonText: "Cancel",
                        callbackFunction: onCancel,
                        buttonStyle: "modal-contect-appealInterface submit-button"
                    }
                }
                this.open(template, title, text, modalWindow, scope)
            }
        }])