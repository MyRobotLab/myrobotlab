################################
# Py4j.py
# more info here: https://www.py4j.org/
# Py4J enables Python programs running in a Python interpreter to dynamically access 
# Java objects in a Java Virtual Machine. 
# Methods are called as if the Java objects resided in the Python interpreter and 
# Java collections can be accessed through standard Python collection methods. 
# Py4J also enables Java programs to call back Python objects. Py4J is distributed under the BSD
# Python 2.7 -to- 3.x is supported
# In your python 3.x project
# pip install py4j
# you have full access to mrl instance that's running
# the gateway

import sys
from time import sleep
from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
from py4j.java_collections import JavaList

from py4j.java_gateway import JavaGateway, CallbackServerParameters
from py4j.java_collections import JavaArray, JavaObject, JavaClass

runtime = None

class MessageHandler(object):

    def __init__(self):
        global runtime
        # initializing stdout and stderr
        self.name = None
        self.stdout = sys.stdout
        self.stderr = sys.stderr
        sys.stdout = self
        sys.stderr = self
        self.gateway = JavaGateway(callback_server_parameters=CallbackServerParameters(), python_server_entry_point=self)
        self.runtime = self.gateway.jvm.org.myrobotlab.service.Runtime.getInstance()
        runtime = self.runtime
        self.py4j = None # need to wait until name is set

    def write(self,string):
        if (self.py4j):
            self.py4j.handleStdOut(string)

    def setName(self, name):
        """Method called right after initialization from MRL
        responsible for 'naming' the MessageHandler

        Args:
            name (string): sets the name of the MessageHandler
        """
        self.name = name
        self.py4j = self.runtime.getService(name)
        print(self.runtime.getUptime())

        print("python started", sys.version)
        print("runtime attached", self.runtime.getVersion())
        # TODO print env vars PYTHONPATH etc
        return name

    def get_name(self):
        return self.name
    
    def exec(self, code):
        """executes python script

        Args:
            code (_type_): code to execute
        """
        try:
            exec(code)
        except Exception as e:
            print(e)

    def invoke(self, method, data=None):
        # convert to list
        params = list(data)
        eval(method)(*params)

    def flush(self):
        pass

    def shutdown(self):
        self.gateway.shutdown()
    
    def getInstance(self):
        return self.runtime

    def convert_array(self, array):
        result = []
        for item in array:
            if isinstance(item, JavaObject):
                item_class = item.getClass()
                if item_class == JavaClass.forName("java.lang.String"):
                    result.append(str(item))
                elif item_class == JavaClass.forName("java.lang.Integer"):
                    result.append(int(item))
                elif item_class == JavaClass.forName("java.lang.Double"):
                    result.append(float(item))
                elif item_class == JavaClass.forName("java.lang.Boolean"):
                    result.append(bool(item))
                elif item_class == JavaClass.forName("java.lang.Long"):
                    result.append(int(item))
                elif item_class == JavaClass.forName("java.lang.Short"):
                    result.append(int(item))
                elif item_class == JavaClass.forName("java.lang.Float"):
                    result.append(float(item))
                elif item_class == JavaClass.forName("java.lang.Byte"):
                    result.append(int(item))
                else:
                    raise ValueError("Unsupported type: {}".format(item_class))
            else:
                raise ValueError("Unsupported type: {}".format(type(item)))
        return result

    class Java:
        implements = ['org.myrobotlab.framework.interfaces.Invoker']

handler = MessageHandler()
runtime = handler.getInstance()
