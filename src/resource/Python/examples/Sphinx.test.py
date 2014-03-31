# simple Sphinx test 

ear = runtime.createAndStart("speech","Sphinx")

ear.addCommand("say foo", "python", "foo")
ear.addCommand("say bar", "python", "bar")
ear.startListening()

def foo():
	print "i said foo"
def bar():
	print "i said bar"