## launching dexarm

from java.lang import String
import time
import os, sys

log = Runtime.start("log","Log")
RuningFolder="resource"
RuningFolder=os.getcwd().replace("\\", "/")+"/"+RuningFolder+"/"
execfile(RuningFolder+'/Dexarm/scripts/pydexarm1.py')
execfile(RuningFolder+'/Dexarm/scripts/pydexarm2.py')
execfile(RuningFolder+'/Dexarm/scripts/swingGui.py')
execfile(RuningFolder+'/Dexarm/scripts/joystick.py')
#execfile(RuningFolder+'/Dexarm/scripts/example2.py')
execfile(RuningFolder+'/Dexarm/scripts/letter1.py')