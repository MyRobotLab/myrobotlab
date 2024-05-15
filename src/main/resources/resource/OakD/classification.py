# new awesome robot script
# new awesome robot script
python = runtime.getService("python")
if python:
    python.subscribe("i01","publishClassification")

def onClassification(c):
    print("label       ", c.get("label"))
    print("confidence  ", c.get("confidence"))
    print("bounding box", c.get("xmin"), c.get("xmax"), c.get("ymin"), c.get("ymax"))    
    print("position    ", c.get("x"), c.get("y"), c.get("z"))
    
print("loaded classifications.py")
