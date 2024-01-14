from framework.config import Config
from framework.service import Service


class Servo(Service):
    def __init__(self, name, id):
        super().__init__(name, id)

    def getConfig(self) -> Config:
        return None
