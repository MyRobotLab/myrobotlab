## launching dexarm

from java.lang import String
import os, sys

log = Runtime.start("log","Log")
RuningFolder="dexarm"
RuningFolder=os.getcwd().replace("\\", "/")+"/"+RuningFolder+"/"
execfile(RuningFolder+'/resource/Dexarm/scripts/pydexarm.py')
#execfile(RuningFolder+'/resource/Dexarm//scripts/example.py')
execfile(RuningFolder+'/resource/Dexarm//scripts/letter1.py')
