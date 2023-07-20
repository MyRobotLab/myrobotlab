from dataclasses import dataclass
from typing import Tuple


"""
Should be in mrlpy.
Need to add Runtime.install()
and an auto-import of mrlpy.services.meta.*
in the Runtime initializer
"""

metadata_instances = dict()

@dataclass
class MetaData():
    service_type: str
    available: bool = True
    dependencies: Tuple = ()
    description: str = ""
    is_cloud_service: bool = False

    def __post_init__(self):
        if self.service_type is None or self.service_type == "":
            raise ValueError("Cannot have empty service type field")
        if self.service_type in metadata_instances:
            raise ValueError(f"Cannot redefine {self.service_type}'s metadata")

        metadata_instances.update({self.service_type: self})




