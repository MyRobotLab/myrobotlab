function init() {
  registerHandler();
}

let eventBus;

function registerHandler() {
  eventBus = new EventBus('https://localhost:8443/eventbus');
  eventBus.onopen = function () {
    eventBus.registerHandler('out', function (error, message) {
      const counter = message.body;
      document.getElementById('current_value').innerHTML = counter;
    });
  }
}

function increment() {
  eventBus.send('in')
}
