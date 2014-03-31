# basic script for Cortex - doesnt do much at the moment
cortex = Runtime.create("cortex","Cortex")
tracking = Runtime.create("tracking","Tracking")
cortex.attach()
cortex.setState("idle")