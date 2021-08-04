#########################################
# InMoov01_start.py
# categories: inmoov
# more info @: http://myrobotlab.org/service/InMoov
#########################################
import shutil
import os
import errno
# uncomment for virtual hardware
# Platform.setVirtual(True)
# start service InMoov2 
i01 = Runtime.start('i01', 'InMoov2')

#copy configs and bots, only if not exist
programABFolder="resource/InMoov2/chatbot/bots/"
configFolder="resource/InMoov2/config/"
#destination directories
dataConfig="data/config/"
dataProgAB="data/ProgramAB/"
# list of folders to be copied
progAB_dir = ['cn-ZH','de-DE', 'en-US', 'es-ES', 'fi-FI', 'fr-FR', 'hi-IN', 'it-IT', 'nl-NL', 'pt-PT', 'ru-RU', 'tr-TR']
config_dir = ['InMoov2_FingerStarter','InMoov2_Full', 'InMoov2_LeftHand', 'InMoov2_LeftSide', 'InMoov2_RightSide']

######################################

# enumerate on progAB_dir to get the 
# content of all the folders and store it in a dictionary
progAB_list = {}
for index, val in enumerate(progAB_dir):
    progAB_path = os.path.join(programABFolder, val)
    progAB_list[ progAB_dir[index] ] = os.listdir(progAB_path)
#####################
# loop through the list of folders
for progAB_dir in progAB_list:
    try:
        shutil.copytree(programABFolder+progAB_dir, dataProgAB+progAB_dir)
    except OSError as e:
        # If the error was caused because the source wasn't a directory
        if e.errno == errno.ENOTDIR:
            shutil.copytree(programABFolder+progAB_dir, dataProgAB+progAB_dir)
        #else:
            #print('No need to copy %s' % e)

#######################################

# enumerate on config_dir to get the 
# content of all the folders and store it in a dictionary
config_list = {}
for index, val in enumerate(config_dir):
    path = os.path.join(configFolder, val)
    config_list[ config_dir[index] ] = os.listdir(path)
#####################
# loop through the list of folders
for config_dir in config_list:
    try:
        shutil.copytree(configFolder+config_dir, dataConfig+config_dir)
    except OSError as e:
        # If the error was caused because the source wasn't a directory
        if e.errno == errno.ENOTDIR:
            shutil.copytree(configFolder+config_dir, dataConfig+config_dir)
        #else:
            #print('No need to copy %s' % e)           
   
