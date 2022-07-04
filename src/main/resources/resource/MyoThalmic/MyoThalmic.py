
from com.thalmic.myo import Pose

myo = runtime.start("myo", "MyoThalmic")

# start optional virtual arduino service, used for test
if ('virtual' in globals() and virtual):
    myo.setVirtual(True)

myo.connect()
myo.addPoseListener(python)

def onPose(pose):
  print(pose.getType())
