from py4j.java_collections import JavaClass, JavaObject
from py4j.java_gateway import CallbackServerParameters, GatewayParameters, JavaGateway

gateway = JavaGateway(
    gateway_parameters=GatewayParameters(auto_convert=True, auto_field=True)
)

runtime = gateway.jvm.org.myrobotlab.service.Runtime.getInstance()

clock = runtime.start("clock", "Clock")
clock.startClock()
