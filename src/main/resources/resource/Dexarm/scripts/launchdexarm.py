## launching dexarm

from java.lang import String
import os, sys

log = Runtime.start("log","Log")
RuningFolder="dexarm"
RuningFolder=os.getcwd().replace("\\", "/")+"/"+RuningFolder+"/"
execfile(RuningFolder+'/scripts/pydexarm.py')
#execfile(RuningFolder+'/scripts/example.py')
execfile(RuningFolder+'/scripts/letter1.py')