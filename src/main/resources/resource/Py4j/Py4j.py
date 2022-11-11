################################
# Py4j.py
# more info here: https://www.py4j.org/
# Py4J enables Python programs running in a Python interpreter to dynamically access 
# Java objects in a Java Virtual Machine. 
# Methods are called as if the Java objects resided in the Python interpreter and 
# Java collections can be accessed through standard Python collection methods. 
# Py4J also enables Java programs to call back Python objects. Py4J is distributed under the BSD
# Python 2.7 -to- 3.x is supported
# to use you will need the py4j lib

# run in mrl instance in Jython or start manually
py4j = runtime.start("py4j","Py4j")

# start the listening socket on the gateway
py4j.start()

########################################
# In your python 3.x project
# pip install py4j
# you have full access to mrl instance that's running
# the gateway

from py4j.java_gateway import JavaGateway
gateway = JavaGateway()
runtime =  gateway.jvm.org.myrobotlab.service.Runtime.getInstance()
print(runtime.getUptime())
brain = runtime.start('brain','ProgramAB')
response = brain.getResponse('what can you do?')
