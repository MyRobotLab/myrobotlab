
from com.thalmic.myo import Pose

myo = Runtime.start("myo", "MyoThalmic")

myo.connect()
myo.addPoseListener(python)

def onPose(pose):
  print(pose.getType())
