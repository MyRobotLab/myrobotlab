################################
# Py4j.py
# Py4j is a Gateway
# Just like the WebGui is a Gateway
# Py4j should have a convention of remote services like {ServiceName}Handler - as all its gateway services are in the jvm
#
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
# Handlers represent proxied processing of messages - same as {service}Gui in the WebGui

# with open('src/main/resources/resource/Py4j/Py4j.py', 'r') as file:
#     script_code = file.read()

# exec(script_code)


import json
import sys
from abc import ABC, abstractmethod

from py4j.java_collections import JavaClass, JavaObject
from py4j.java_gateway import CallbackServerParameters, GatewayParameters, JavaGateway

# REQUIRED TO SET FIELDS !
# from py4j.java_gateway import set_field
# rename java_object proxy or service?


class SingletonMeta(type):
    _instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            cls._instances[cls] = super().__call__(*args, **kwargs)
        return cls._instances[cls]


class Service(ABC):
    def __init__(self, name):
        self.java_object = runtime.start(name, self.getType())

    def __getattr__(self, attr):
        # Delegate attribute access to the underlying Java object
        return getattr(self.java_object, attr)

    def __str__(self):
        # Delegate string representation to the underlying Java object
        return str(self.java_object)

    def subscribe(self, topic, method):
        print("subscribe")
        self.java_object.subscribe(topic, method)

    # FIXME - TODO - getName(), getConfig(), send(), invoke(), sendBlocking()

    @abstractmethod
    def getType(self):
        pass


class NeoPixel(Service):
    def __init__(self, name):
        super().__init__(name)

    def getType(self):
        return "NeoPixel"

    def onFlash(self):
        print("onFlash")


# TODO dynamically add classes that you don't bother to check in

# class Runtime(Service):
#     def __init__(self, name):
#         super().__init__(name)


# FIXME - REMOVE THIS - DO NOT SET ANY GLOBALS !!!!
runtime = None


# TODO - rename to mrl_lib ?
# e.g.
# mrl = mrl_lib.connect("localhost", 1099)
# i01 = InMoov("i01", mrl)
# or
# runtime = mrl_lib.connect("localhost", 1099) # JVM connection Py4j instance needed for a gateway
# runtime.start("i01", "InMoov2") # starts Java service
# runtime.start("nativePythonService", "NativePythonClass") # starts Python service no gateway needed
# class MessageHandler(object):


class MessageHandler(metaclass=SingletonMeta):
    """
    The class responsible for receiving and processing Py4j messages,
    including handling `invoke()` and `exec()` requests. Class
    must be initialized and then the `setName()` method must be invoked before
    the Java and Python sides can talk correctly.
    """

    def __init__(self):
        global runtime
        # initializing stdout and stderr
        print("initializing")
        self.name = None
        self.stdout = sys.stdout
        self.stderr = sys.stderr
        # registry is registry of py4j services
        self.registry = {}
        # registry_handlers is registry of python handlers for java proxies
        self.registry_handlers = {}
        sys.stdout = self
        sys.stderr = self
        self.gateway = JavaGateway(
            callback_server_parameters=CallbackServerParameters(),
            python_server_entry_point=self,
            gateway_parameters=GatewayParameters(auto_convert=True, auto_field=True),
        )
        self.runtime = self.gateway.jvm.org.myrobotlab.service.Runtime.getInstance()
        # FIXME - REMOVE THIS - DO NOT SET ANY GLOBALS !!!!
        runtime = self.runtime
        self.py4j = None  # need to wait until name is set
        print("initialized ... waiting for name to be set")

    def construct_runtime(self):
        """
        FIXME - remove this method
        Constructs a new Runtime instance and returns it.
        """
        jvm_runtime = self.gateway.jvm.org.myrobotlab.service.Runtime.getInstance()

        # Define class attributes and methods as dictionaries
        class_attributes = {
            "x": 0,
            "y": 0,
            "move": lambda self, dx, dy: setattr(self, "x", self.x + dx)
            or setattr(self, "y", self.y + dy),
            "get_position": lambda self: (self.x, self.y),
        }

        # Create the class dynamically using the type() function
        MyDynamicClass = type("MyDynamicClass", (object,), class_attributes)

        # Create an instance of the dynamically created class
        obj = MyDynamicClass()

        return self.runtime

    # Define the callback function
    def handle_connection_break(self):
        # Add your custom logic here to handle the connection break
        print("Connection with Java gateway was lost or terminated.")
        print("goodbye.")
        sys.exit(1)

    def write(self, string):
        if self.py4j:
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
        print("name set to", name)
        self.name = name
        self.py4j = self.runtime.getService(name)
        print(self.runtime.getUptime())

        print("python started", sys.version)
        print("runtime attached", self.runtime.getVersion())
        print("services", self.runtime.getServiceNames())
        for name in self.runtime.getServiceNames():
            print(name)
            self.registry[name] = self.runtime.getService(name)
            long_type = self.registry.get(name).getTypeKey()
            parts = long_type.split(".")
            if len(parts) > 1:
                short_type = parts[-1]
            else:
                short_type = long_type

            print("simpleName", self.registry.get(name).getTypeKey())

            # , 'InMoov2': <class '__main__.InMoov2'>}
            # and isinstance(globals()[short_type], type): - module needs to be __main__
            if short_type in globals():
                print(f"'{short_type}' exists as a class in the global space.")
            else:
                print(f"'{short_type}' will be unknown.")

        # TODO print env vars PYTHONPATH etc
        return name

    def getRuntime(self):
        return self.runtime

    def exec(self, code):
        """
        Executes Python code in the global namespace.
        All exceptions are caught and printed so that the
        Python subprocess doesn't crash.

        :param code: The Python code to execute.
        :type code: str
        """
        try:
            # Restricts the exec() to the global namespace,
            # so the code is executed as if it were the top level of a module
            exec(code, globals())
        except Exception as e:
            print(e)

    def send(self, json_msg):
        msg = json.loads(json_msg)
        if msg.get("data") is None or msg.get("data") == []:
            globals()[msg.get("method")]()
        else:
            globals()[msg.get("method")](*msg.get("data"))

    # equivalent to JS onMessage
    def invoke(self, method, data=None):
        """
        Invoke a function from the global namespace with the given parameters.

        :param method: The name of the function to invoke.
        :type method: str
        :param data: The parameters to pass to the function, defaulting to
        no parameters.
        :type data: Iterable
        """

        # convert to list
        # params = list(data) not necessary will always be a json string

        # Lookup the method in the global namespace
        # Much much faster than using eval()

        # data should be None or always a list of params
        if data is None:
            globals()[method]()
        else:
            # one shot json decode
            params = json.loads(data)
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
        implements = ["org.myrobotlab.framework.interfaces.Invoker"]


############################################################################################################
# service specific


# IMPORTANT - Jython could call overloaded methods, Py4j cannot !!!
# names are guaranteed to be unique
# the kluge of changing period to underscore
# auto create globals ? (bad idea)
# isolate to callbacks
# from py4j.java_gateway import JavaObject

# unsuccessful attempt to modularize
# class InMoov2(JavaObject):
#     global runtime

#     def __init__(self, name):
#         # super().__init__(runtime.start(name, "InMoov2"))
#         super().__init__("blah", handler.gateway)
#         self.name = name
#         self.robot = runtime.start(name, "InMoov2")


# FOR SUBSCRIPTIONS WHOS DESTINATION IS PYTHON
# SHOULD PROBABLY BE MAINTAINED HERE


# global routing - handle inside of py4j router
def onPythonMessage(msg):
    # FIXME - don't know hot to fix yet
    global python_runtime
    """Initial processing point for all messages.
    Messages are sent from the Java side to the Python side,
    then decoded and routed to the appropriate method.

    Decoding msg is unwrapped from the tunnelled message, and
    the python method is called with a parameter of the tunnelled.sender
    e.g.
      onCallback('i01', data):
        # msg body
 
    The preferred way would be to use a python side service class where self is the
    first parameter, and the service is returned from the registry.

    e.g.
      onCallback(self, data):
        # msg body

    But I have not figured out how to augment py4j to do this... yet.

    Args:
        msg (_type_): tunnelled message, with data message
    """
    try:
        print("onPythonMessage method", msg.get("method"))
        print("onPythonMessage data", msg.get("data"))
        print("onPythonMessage sender", msg.get("sender"))
        # untunnelled msg
        # untunnelled = msg.get[0]("data")
        # print("untunnelled", untunnelled.get("method"), untunnelled.get("data"))
        # exec(msg.method)(msg)
        # exec(code, globals())
        # registry.get(msg.sender)
    except Exception as e:
        print(e)


class InMoov2(Service):
    def __init__(self, name):
        super().__init__(name)
        self.subscribe(name, "onStateChange")

    def getType(self):
        return "InMoov2"

    def onOnStateChange(self, state):
        print("onOnStateChange")
        print(state)
        print(state.get("last"))
        print(state.get("current"))
        print(state.get("event"))

    # FIXME - global method not name specific
    def onStartSpeaking(self, text):
        global runtime

        print("onStartSpeaking", text)

        # FIXME FIXME FIXME name delivered as a parameter or
        # make class that is created by the runtime with name
        i01 = runtime.getService("i01")

        if i01:
            # i01_neoPixel = i01.getPeer("neoPixel")
            # if i01_neoPixel and i01.getConfig().neoPixelFlashWhenSpeaking:
            # i01_neoPixel.setAnimation("Ironman", 255, 255, 255, 20)

            # Great idea, but has hardcoded translations - this should be done in the language service but the
            # raw gesture still be processed
            # if 'oui ' in text or 'yes ' in text or ' oui' in text or 'ja ' in text or text=="yes" or text=="kyllä":Yes()
            # if 'non ' in text or 'no ' in text or 'nicht ' in text or 'neen ' in text or text=="no" or text=="ei":No()

            # force random move while speaking, to avoid conflict with random life gesture
            if i01.getConfig().robotCanMoveHeadWhileSpeaking:
                i01_random = runtime.getService("i01.random")
                if i01_random and i01.getState() != "tracking":
                    i01_random.disableAll()
                    i01_random.enable("i01.setHeadSpeed")
                    i01_random.enable("i01.moveHead")
                    i01_random.enable()

    # FIXME - global method not name specific
    def onEndSpeaking(self, text):
        global runtime
        print("onEndSpeaking", text)
        i01 = runtime.getService("i01")
        i01_random = runtime.getService("i01.random")
        if i01_random and i01.getState() != "tracking":
            # i01_random.disable("i01.setHeadSpeed")
            # i01_random.disable("i01.moveHead")
            i01_random.disable()

        # if i01:
        #     if i01.getConfig().robotCanMoveHeadWhileSpeaking:
        #         i01_random = runtime.getService("i01.random")
        #         if i01_random:
        #             i01_random.disable()
        #             i01_random.enable('i01.moveLeftArm')
        #             i01_random.enable('i01.moveRightArm')
        #             i01_random.enable('i01.moveLeftHand')
        #             i01_random.enable('i01.moveRightHand')
        #             i01_random.enable('i01.moveTorso')
        #             i01_random.enable('i01.setLeftArmSpeed')
        #             i01_random.enable('i01.setRightArmSpeed')
        #             i01_random.enable('i01.setLeftHandSpeed')
        #             i01_random.enable('i01.setRightHandSpeed')
        #             i01_random.enable('i01.setTorsoSpeed')
        #             if runtime.isStarted('i01.head'):
        #                 i01_random.addRandom(200, 1000, "i01", "setHeadSpeed", 8.0, 20.0, 8.0, 20.0, 8.0, 20.0)
        #                 i01_random.addRandom(200, 1000, "i01", "moveHead", 70.0, 110.0, 65.0, 115.0, 70.0, 110.0)
        #         i01_neoPixel = runtime.getService('i01.neoPixel')
        #         if i01.getConfig().neoPixelFlashWhenSpeaking and runtime.isStarted("i01.neoPixel"):
        #             i01_neoPixel.clear()

    # Sensor events begin ========================================

    def on_pir_on(self, name):
        robot = runtime.getService(name)
        # FIXME - chatBot.getResponse("SYSTEM_EVENT on_start")
        mouth = robot.getPeer("mouth")
        mouth.speak("I feel your presence")
        print("on_pir_on", name)

    def on_pir_off(self, name):
        robot = runtime.getService(name)
        # FIXME - chatBot.getResponse("SYSTEM_EVENT on_start")
        mouth = robot.getPeer("mouth")
        mouth.speak("I'm so alone")
        print("on_pir_off", name)

    # Sensor events end ==========================================
    # Topic events begin ========================================
    # FIXME - since its a direct subscribe from i01.chatBot -to-> python
    # we don't have a name - so we can't use the name as a parameter

    def onTopic(self, topic_event):
        """Called when topic changes in chatbot,
        is rebroadcasted from InMoov2
        TopicEvent.
            botname = name of bot
            use = name of user
            topic = current topic
            src = name of source

        Args:
            topic (_type_): _description_
        """
        # FIXME - find a solution for the hardcoded name !
        # route through inmoov add name as field
        robot = runtime.getService(topic_event.src)
        mouth = robot.getPeer("mouth")
        if mouth:
            mouth.speak("New topic, the topic is " + topic_event.topic)
        print("onTopic", topic_event.topic)

    # Topic events end ==========================================
    # State change events begin ========================================

    def onStateChange(self, state_event):
        """The main router for state changes
        it calls the appropriate method based on the state change
        Hooked to InMoov2.publishStateChange

        Args:
            data (InMoov2State): contains src and state
        """

        # Python 2 unicode pain
        state = str(state_event.state)
        src = str(state_event.src)

        robot = runtime.getService(src)

        # leaving state changes
        fsm = robot.getPeer("fsm")
        random = robot.getPeer("random")
        mouth = robot.getPeer("mouth")
        chatBot = robot.getPeer("chatBot")

        if fsm:
            leavingState = fsm.getPreviousState()

        if random and leavingState == "random":
            random.disable()

        if chatBot:
            chatBot.setPredicate("state", state)

        # if chatBot and leavingState == "first_init":
        #     # move the botname from human predicates to new user predicates
        #     # chatBot.setPredicate("botname", chatBot.getPredicate("human","botname")
        #     # this sets the first user to be the one identified at the end of first_init
        #     chatBot.setConfigValue("username", chatBot.getUsername())
        #     # Not sure if this should only be maintained in predicates
        #     # but config.username is the first session
        #     # it is "modifying" config hower, which might be difficult to support
        #     chatBot.save()

        # FIXME - chatBot.getResponse("SYSTEM_EVENT on_start")
        if mouth:
            mouth.speak("leaving " + leavingState + " state and entering " + state)

        print("on_state_change", src, state)

        # call the new state handler
        eval("on_" + state + "('" + src + "')")

    def on_start(self, name):
        """Start is where all custom activity can begin.
        It is the first state changed called after boot.
        At this point InMoov2 service has started and andy
        runtime configuration has been processed.
        The few services required to be running fsm chatBot and python
        will be running.  Other services may need to be checked
        e.g.
        opencv = runtime.getService("i01.opencv")
        if opencv:
            .... do something
        Args:
            name (string): name of service
        """
        robot = runtime.getService(name)
        chatbot = robot.getPeer("chatBot")
        # FIXME - chatBot.getResponse("SYSTEM_EVENT on_start")
        mouth = robot.getPeer("mouth")
        fsm = robot.getPeer("fsm")

        # iterate through all current started peers
        # and add subscriptions to this service ?
        # remove all the python subscriptions from InMoov2Config ?

        # FIXME - chatBot.getResponse("SYSTEM_EVENT on_start")
        if mouth:
            mouth.speak("I am starting")
        print("on_start state change from", name)
        # TODO - make a boot report and give it - errors and warnings ?
        # TODO - reporting in led display or verbal or wait for verbal

        # human by default is the first user and first predicate file
        # on startup try to identify the user
        if chatbot.getUsername() == "human":
            # try to identify user go through FIRST_INIT
            fsm.fire("first_init")
        else:
            # if user is known - go through WAKE_UP
            fsm.fire("wake")

    def on_first_init(self, name):
        """Purpose of this state is to identify the user
        and first initial configuration of the robot.

        The user should be asked to identify themselves.

        Various other information could be gathered as well
        although the user should be able to leave at any time.

        Coming back to this state should be possible at any time.

        Args:
            name (string): name of InMoov2 robot
        """
        robot = runtime.getService(name)
        chatbot = robot.getPeer("chatBot")
        chatbot.getResponse("NEW_USER")

        # do anything else desired
        # generate picture data of the user
        # go through personal questionare
        # e.g. ask

    def on_wake(self, name):
        robot = runtime.getService(name)
        chatbot = robot.getPeer("chatBot")
        chatbot.getResponse("WAKE_UP")
        print("on_wake state change from", name)

    def on_idle(self, name):
        robot = runtime.getService(name)
        mouth = robot.getPeer("mouth")
        mouth.speak("I am idle")
        print("on_idle state change from", name)

    def on_random(self, name):
        robot = runtime.getService(name)
        mouth = robot.getPeer("mouth")
        mouth.speak("I am doing random stuff")
        print("on_random state change from", name)

    def on_sleep(self, name):
        robot = runtime.getService(name)
        mouth = robot.getPeer("mouth")
        mouth.speak("I am going to sleep")
        print("on_sleep state change from", name)

    # State change events end ========================================
    # Service change events begin ========================================

    def on_peer_started(self, name):
        robot = runtime.getService(name)
        mouth = robot.getPeer("mouth")
        mouth.speak("I am starting a new service")
        print("on_peer_started service change from", name)
        # add subscriptions for newly started peers

    def on_peer_released(self, name):
        robot = runtime.getService(name)
        mouth = robot.getPeer("mouth")
        mouth.speak("I am releasing a service")
        print("on_peer_released service change from", name)
        # remove subscriptions for newly released peers

    # Service change events end ========================================

    def on_sensor_data(self, data):
        """generalized sensor handler

        Args:
            data (_type_): _description_
        """
        print("on_sensor_data", data)

    def onHeartbeat(self, sender: str):
        """onHeartbeat a incremental timer used to drive
        state machines and other time based events.
        Heartbeats here do not begin until after boot.

        Args:
            sender (string): the robot's name sending the heartbeat
        """
        print("onHeartbeat", sender)

        robot = runtime.getService(sender)
        neoPixel = robot.getPeer("neoPixel")

        if neoPixel:
            neoPixel.flash("heartbeat")
        # if robot.getState() == "first_init":
        #     robot.setRandomIdle()

    def onPredicate(self, predicate_event):
        robot = runtime.getService(predicate_event.src)
        robot.info(
            "predicate " + predicate_event.name + " changed to " + predicate_event.value
        )
        # mouth = robot.getPeer("mouth")
        # if mouth:
        #     mouth.speak("predicate " + predicate_event.name + " changed to " + predicate_event.value)

    def onSession(self, session_event):
        robot = runtime.getService(session_event.src)
        mouth = robot.getPeer("mouth")
        chatBot = robot.getPeer("chatBot")
        if mouth:
            mouth.speak("new session with " + session_event.user)
        # if chatBot:
        #     chatBot.setTopic("new_user")

    def onMessage(self, msg):
        robot = runtime.getService(msg.sender)
        print("onMessage.method", msg.method)
        print("onMessage.data", msg.data)
        robot.info("onMessage.method " + msg.method)
        robot.info("onMessage.data " + str(msg.data))
        # eval(msg.method)(msg.data
        # auto invoke method
        # expand parameters ?

    def on_new_user(self, data):
        print("on_new_user", data)
        robot = runtime.getService(data.sender)
        chatBot = robot.getPeer("chatBot")

        # TODO - some function that identifies user and reconciles identities
        # until then we'll just create a new user
        chatBot.setUser(data[0])  # name
        chatBot.setPredicate("botname", chatBot.getPredicate("human", "botname"))


# FIXME - better singleton
if "handler" not in globals():
    handler = MessageHandler()
    if len(sys.argv) > 1:
        handler.setName(sys.argv[1])
    else:
        raise RuntimeError(
            "This script requires the full name of the Py4j service as its first command-line argument"
        )

print("loaded Py4j.py")
