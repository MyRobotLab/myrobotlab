# TODO: Implement this script fpr
fsm = runtime.start("fsm","FiniteStateMachine")

# add 4 states
fsm.addState("neutral")
fsm.addState("ill")
fsm.addState("sick")
fsm.addState("vomiting")

# woah - can handle multiple states - it propegates
# events across all of them
# fsm.setStates("neutral", "ill", "sick", "vomiting");
# fsm.setStates("neutral")

# add 8 transitions of 2 types
fsm.addTransition("neutral","ill-event","ill")
fsm.addTransition("ill","ill-event","sick")
fsm.addTransition("sick","ill-event","vomiting")
fsm.addTransition("vomiting","ill-event","vomiting")

fsm.addTransition("vomiting","clear-event","sick")
fsm.addTransition("sick","clear-event","ill")
fsm.addTransition("ill","clear-event","neutral")
fsm.addTransition("neutral","clear-event","neutral")

fsm.fire("clear-event");
print(fsm.getCurrentState())
sleep(1)

fsm.fire("ill-event");
print(fsm.getCurrentState())
sleep(1)

fsm.fire("ill-event");
print(fsm.getCurrentState())
sleep(1)

fsm.fire("ill-event");
print(fsm.getCurrentState())
sleep(1)

fsm.fire("clear-event");
print(fsm.getCurrentState())
sleep(1)

fsm.fire("clear-event");
print(fsm.getCurrentState())
sleep(1)

fsm.fire("clear-event");
print(fsm.getCurrentState())
sleep(1)


print(fsm.getCurrentState())
print(fsm.getCurrentStates())

# cleare the diagram
# fsm.clear()