import asyncio
import importlib
import json
from time import sleep

import websockets
from codec.codec_util import CodecUtil
from framework.config import Config
from framework.service import Service


class RuntimeConfig(Config):
    def __init__(self, config):
        super().__init__(config)


class Runtime(Service):
    __instance = None

    def __init__(self, name, id=None):
        if Runtime.__instance is not None:
            return Runtime.__instance
        if id is None:
            id = CodecUtil.generate_name()
        super().__init__(name, id)
        Runtime.__instance = self
        self.registry = {}
        # TODO - override with command line
        self.id = CodecUtil.generate_name()

    def getConfig(self) -> Config:
        return RuntimeConfig("Runtime Configuration")

    @staticmethod
    def getRuntime():
        if Runtime.__instance is None:
            Runtime.__instance = Runtime("runtime")
        return Runtime.__instance

    def register(self, service: Service):
        self.registry[service.getName()] = service

    def getService(self, name: str) -> Service:
        return self.registry[name]

    def getServices(self) -> list:
        return list(self.registry.values())

    def start(self, name: str, type_: str):
        if name and type_:
            new_service = self.load_class(name, self.id, type_)
            # if type_ not in globals():
            #     raise ValueError(f"Class '{type_}' not found.")
            # service_class = globals()[type_]
            # if not issubclass(service_class, Service):
            #     raise ValueError(f"'{type_}' is not a subclass of Service.")
            # new_service = service_class(name)
            self.registry[name] = new_service
            print(f"Service '{name}' of type '{type_}' added to the registry.")
            return new_service
        else:
            raise ValueError("Both name and type are required to add a service")

    def load_class(self, name, id, class_name):
        try:
            module_name = class_name.lower()
            module = importlib.import_module(f"service.{module_name}")
            my_class = getattr(module, class_name)
            return my_class(name, id) if my_class else None
        except (ImportError, AttributeError):
            return None  # Return None if class couldn't be loaded

    def connect(self, url: str):
        asyncio.run(self.receive_messages())

    async def receive_messages(self, url: str):
        async with websockets.connect(
            "ws://localhost:8888/api/messages?id=123"
        ) as websocket:
            while True:
                message = await websocket.recv()
                print(f"recv message {message}")
                msg = json.loads(message)
                print(f'msgId {msg.get("msgId")}')
                print(f'data {msg.get("data")}')
                data = msg.get("data")
                print(f"len data {len(data)}")
                # data = json.loads(message)


# Example usage:
runtime = Runtime.getRuntime()
print(runtime.getName())  # Output: runtime
clock = runtime.start("clock1", "Clock")
print(clock.getName())  # Output: servo1
clock.start_clock()  # Output: 1604391129.031
# Creating another Runtime instance won't work
# runtime2 = Runtime("runtimex")  # This will raise a RuntimeError

sleep(5)
