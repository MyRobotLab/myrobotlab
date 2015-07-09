angular.module('mrlapp.service')
.directive('oscope', ['$compile', 'mrl', '$log', function($compile, mrl, $log) {
        return {
            restrict: "E",
            templateUrl: 'widget/oscope.html',
            link: function(scope, element) {

                // FIXEME - should be a better way than 
                // using the grandparent scope :P
                // WE DO NEED the service name who created us !
                // and to make oscopes work with other services they 
                // need to implement the same control & callback interfaces

                // oscope display interface should allow
                //  * dynamically changing the number of trace buttons
                //  * an interface to setup callback for publishing trace data
                //  * zoom
                //  * set gradient range

                // display update interfaces
                var setTraceButtons = function(pinList) {
                    scope.pinList = service.pinList;
                }

                // FIXME - get name through attribute
                // FIXME - create isolated scope !
                var serviceScope = scope.$parent.$parent;
                var name = serviceScope.service.name;
                var service = mrl.getService(name);

                // initializing display data      
                setTraceButtons(service.pinList);

                // set up call backs to interfaces
                // getPinList --> setTraceButtons
                // publishPin --> onTraceData

                // initial set of tracebuttons
                
                var screen = document.getElementById("screen");
                
                if (!screen.getContext) {
                    $log.error("could not find oscope screen")
                }
                
                var ctx = screen.getContext('2d');
                ctx.rect(0, 0, screen.width, screen.height);
                ctx.fillStyle = "black";
                ctx.fill();

                // variable that decides if something should be drawn on mousemove
                var drawing = false;

                // the last coordinates before the current move
                var lastX;
                var lastY;
                
                element.bind('mousedown', function(event) {
                    if (event.offsetX !== undefined) {
                        lastX = event.offsetX;
                        lastY = event.offsetY;
                    } else {
                        lastX = event.layerX - event.currentTarget.offsetLeft;
                        lastY = event.layerY - event.currentTarget.offsetTop;
                    }

                    // begins new line
                    ctx.beginPath();
                    
                    drawing = true;
                });
                element.bind('mousemove', function(event) {
                    if (drawing) {
                        // get current mouse position
                        if (event.offsetX !== undefined) {
                            currentX = event.offsetX;
                            currentY = event.offsetY;
                        } else {
                            currentX = event.layerX - event.currentTarget.offsetLeft;
                            currentY = event.layerY - event.currentTarget.offsetTop;
                        }
                        
                        draw(lastX, lastY, currentX, currentY);

                        // set current coordinates to last one
                        lastX = currentX;
                        lastY = currentY;
                    }
                
                });
                element.bind('mouseup', function(event) {
                    // stop drawing
                    drawing = false;
                });

                // canvas reset
                function reset() {
                    element[0].width = element[0].width;
                }
                
                function draw(lX, lY, cX, cY) {
                    // line from
                    ctx.moveTo(lX, lY);
                    // to
                    ctx.lineTo(cX, cY);
                    // color
                    ctx.strokeStyle = "#ccc";
                    // draw it
                    ctx.stroke();
                }
            }
        };
    }]);
