import threading
import time

from framework.config import Config
from framework.service import Service


class Clock(Service):
    def __init__(self, name, id):
        super().__init__(name, id)
        self.is_running = False
        self.timer = None

    def start_clock(self):
        if not self.is_running:
            self.is_running = True
            self.pulse()

    def pulse(self):
        if self.is_running:
            self.publish_time()
            self.timer = threading.Timer(1, self.pulse)
            self.timer.start()

    def stop_clock(self):
        self.is_running = False
        if self.timer:
            self.timer.cancel()

    def publish_time(self):
        print(time.time())

    def getConfig(self) -> Config:
        return None
