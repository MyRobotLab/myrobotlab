
class InMoov2(object):
    """Library to support inmoov2 in Python
    Intended to be used with Jython or Py4j.
    This will be a singleton class.

    """

    def __init__(self):
        self.name = "MyRobotLab"
        self.connections = {}
        # TODO initialize any singleton state variables here

    def connect(self, host="127.0.0.1", port=25333):
        
        self.connections[host + ":" + str(port)] = {
            "host": host,
            "port": port,
        }

        print("connecting.....")
    
_myrobotlab = InMoov2()
connect = _myrobotlab.connect