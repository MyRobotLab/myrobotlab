## launching dexarm

from java.lang import String
import os, sys

log = Runtime.start("log","Log")
RuningFolder="resource"
RuningFolder=os.getcwd().replace("\\", "/")+"/"+RuningFolder+"/"
execfile(RuningFolder+'/Dexarm/scripts/pydexarm.py')
#execfile(RuningFolder+'/Dexarm/scripts/example.py')
execfile(RuningFolder+'/Dexarm/scripts/letter1.py')
