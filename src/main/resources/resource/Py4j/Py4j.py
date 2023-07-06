################################
# Py4j.py
# more info here: https://www.py4j.org/
# Py4J enables Python programs running in a Python interpreter to dynamically access 
# Java objects in a Java Virtual Machine. 
# Methods are called as if the Java objects resided in the Python interpreter and 
# Java collections can be accessed through standard Python collection methods. 
# Py4J also enables Java programs to call back Python objects. Py4J is distributed under the BSD license
# Python 2.7 -to- 3.x is supported
# In your python 3.x project
# pip install py4j
# you have full access to mrl instance that's running
# the gateway

import sys

from py4j.java_collections import JavaObject, JavaClass
from py4j.java_gateway import JavaGateway, CallbackServerParameters, GatewayParameters

runtime = None


class MessageHandler(object):
    """
    The class responsible for receiving and processing Py4j messages,
    including handling `invoke()` and `exec()` requests. Class
    must be initialized and then the `setName()` method must be invoked before
    the Java and Python sides can talk correctly.
    """

    def __init__(self):
        global runtime
        # initializing stdout and stderr
        self.name = None
        self.stdout = sys.stdout
        self.stderr = sys.stderr
        sys.stdout = self
        sys.stderr = self
        # self.gateway = JavaGateway(callback_server_parameters=CallbackServerParameters(connection_callback=self.handle_connection_break),
        self.gateway = JavaGateway(callback_server_parameters=CallbackServerParameters(),
                                   python_server_entry_point=self,
                                   gateway_parameters=GatewayParameters(auto_convert=True))
        self.runtime = self.gateway.jvm.org.myrobotlab.service.Runtime.getInstance()
        runtime = self.runtime
        self.py4j = None  # need to wait until name is set

    # Define the callback function
    def handle_connection_break(self):
        # Add your custom logic here to handle the connection break
        print("Connection with Java gateway was lost or terminated.")        
        print("goodbye.")
        sys.exit(1)

    def write(self,string):
        if (self.py4j):
            self.py4j.handleStdOut(string)

    def flush(self):
        pass

    def setName(self, name):
        """
        Called after initialization completed in order
        for this Python side handler to know how to contact the Java-side
        service.

        :param name: Name of the Java-side Py4j service this script is linked to, preferably as a full name.
        :type name: str
        """
        self.name = name
        self.py4j = self.runtime.getService(name)
        print(self.runtime.getUptime())

        print("python started", sys.version)
        print("runtime attached", self.runtime.getVersion())
        # TODO print env vars PYTHONPATH etc
        return name

    def exec(self, code):
        """
        Executes Python code in the global namespace.
        All exceptions are caught and printed so that the Python subprocess doesn't crash.

        :param code: The Python code to execute.
        :type code: str
        """
        try:
            # Restricts the exec() to the global namespace,
            # so the code is executed as if it were the top level of a module
            exec(code, globals())
        except Exception as e:
            print(e)

    def invoke(self, method, data=()):
        """
        Invoke a function from the global namespace with the given parameters.

        :param method: The name of the function to invoke.
        :type method: str
        :param data: The parameters to pass to the function, defaulting to no parameters.
        :type data: Iterable
        """

        # convert to list
        params = list(data)

        # Lookup the method in the global namespace
        # Much much faster than using eval()
        globals()[method](*params)

    def shutdown(self):
        """
        Shutdown the Py4j gateway
        :return:
        """
        self.gateway.shutdown()

    def convert_array(self, array):
        """
        Utility method used by Py4j to convert arrays of Java objects
        into equivalent Python lists.
        :param array: The array to convert
        :return: The converted array
        """
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
if len(sys.argv) > 1:
    handler.setName(sys.argv[1])
else:
    raise RuntimeError("This script requires the full name of the Py4j service as its first command-line argument")
