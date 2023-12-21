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

    @abstractmethod
    def getConfig(self) -> Config:
        pass

    def getName(self):
        return self.name

    def in_msg(self, msg: Message):
        self.inbox.put_nowait(msg)

    def out_msg(self, msg: Message):
        self.outbox.put_nowait(msg)
