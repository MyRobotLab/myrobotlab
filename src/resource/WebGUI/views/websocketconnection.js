angular.module('mrlapp.wsconn', [])
        .service('wsconnService', [function () {
                //refactor this - more generalization & probably using .provide to inject a config & a callback before creation of the service

                var callbacks = [];

                this.subscribeToMessages = function (callback) {
                    callbacks.push(callback);
                };

                this.sendMessage = function (message) {
                    var json = jQuery.stringifyJSON(message);
                    this.sendDirectMessage(json);
                };

                this.sendDirectMessage = function (message) {
                    socket.push(message);
                    console.log('Sent message: ' + message);
                };

                var transport = 'websocket';
                var socket = null;

                this.connect = function (host) {
                    var request = {url: host,
                        transport: 'websocket',
                        enableProtocol: true,
                        trackMessageLength: true,
                        logLevel: 'debug'};

                    request.onOpen = function (response) {
                        // Socket open ...
                        console.log('Info: ' + transport + ' connection opened.');
                    };

                    request.onClose = function (response) {
                        console.log('websocket, onclose');
                        if (response.state == "unsubscribe") {
                            console.log('Info: ' + transport + ' closed.');
                        }
                    };

                    request.onTransportFailure = function (errorMsg, request) {
                        jQuery.atmosphere.info(errorMsg);
                        if (window.EventSource) {
                            request.fallbackTransport = "sse";
                        } else {
                            request.fallbackTransport = 'long-polling';
                        }
                        transport = request.fallbackTransport;

                        console.log('Error: falling back to ' + transport + ' ' + errorMsg);
                    };

                    request.onMessage = function (response) {
                        var body = response.responseBody;
                        if (body == 'X') {
                            console.log("heartbeat:", body);
                        } else {
                            var msg;
                            try {
                                msg = jQuery.parseJSON(body);
                            } catch (e) {
                                console.log('Error onMessage: ', e, body);
                                return;
                            }
                            angular.forEach(callbacks, function (value, key) {
                                value(msg);
                            });
                        }
                    };

                    socket = $.atmosphere.subscribe(request);
                };
            }]);