import asyncio
from abc import ABC, abstractmethod

from .config import Config
from .message import Message


class Service(ABC):
    def __init__(self, name, id):
        self.name = name
        self.id = id
        self.inbox = asyncio.Queue()
        self.outbox = asyncio.Queue()
        self.notify_list = {}

    @abstractmethod
    def getConfig(self) -> Config:
        pass

    def getName(self):
        return self.name

    def in_msg(self, msg: Message):
        self.inbox.put_nowait(msg)

    def out_msg(self, msg: Message):
        self.outbox.put_nowait(msg)

    def invoke(self, method: str, *args, **kwargs):
        try:
            return self.invoke_on(self, method, *args, **kwargs)
        except AttributeError:
            raise ValueError(f"Method '{method}' not found.")

    def invoke_on(self, method: str, *args, **kwargs):
        try:
            obj = getattr(self, method)(*args, **kwargs)
            # TODO - add obj to outbox
            if method in self.notify_list:
                for listener in self.notify_list[method]:
                    # TODO - add obj to listener.inbo
                    pass
            return obj
        except AttributeError:
            raise ValueError(f"Method '{method}' not found.")
