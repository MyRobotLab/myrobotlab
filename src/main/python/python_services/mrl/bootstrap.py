import logging
import sys

from mrlpy import mcommand
from mrlpy.framework import runtime
from mrlpy.framework.runtime import Runtime
runtime.runtime_id = sys.argv[1]

Runtime.init_runtime()
logging.basicConfig(level=logging.INFO, force=True)
mcommand.connect(id=sys.argv[1], daemon=False)
