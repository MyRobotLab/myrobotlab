#########################################
# Random.py
# description: Random message generator - sends messages
# at random intervales with random parameters
# categories: general
# more info @: http://myrobotlab.org/service/Random
#########################################

# start the service
python = runtime.start("python", "Python")
random = runtime.start("random", "Random")
clock = runtime.start("clock", "Clock")


def happy():
    print("i am happy")


def sad():
    print("i am sad")


def angry():
    print("i am angry")


# add a named random task
random.addRandom("random emotion", 1000, 2000, "python", "exec", "happy()", "sad()", "angry()")


# enable random events
random.enable()


def roll_dice(value):
    print("roll_dice " + str(value))


# roll the dice every 1 to 2 seonds
random.addRandom(1000, 2000, "python", "roll_dice", random.intRange(1, 6))


# add a complex dice
def roll_complex_dice(value):
    print("roll_complex_dice " + str(value))


# roll the complex dice every 1 to 2 seonds
random.addRandom(1000, 2000, "python", "roll_complex_dice", random.doubleRange(1, 6))


def random_color(value):
    print("random_color " + str(value))


# roll the dice every 1 to 2 seonds
random.addRandom(1000, 2000, "python", "random_color", random.setRange("red", "green", "blue", "yellow"))


# do a complex multi parameter, multi-type method
def kitchen_sink(dice, complex_dice, colors, names):
    print("kitchen_sink ", dice, complex_dice, colors, names)


random.addRandom(1000, 2000, "python", "kitchen_sink", random.intRange(1, 6), random.doubleRange(1, 6), random.setRange("red","green","blue","yellow"), random.setRange("bob","jane","fred","mary"))

# set the interval on a clock between 1000 and 8000
# if you look in the UI you can see the clock interval changing
random.addRandom(200, 500, "clock", "setInterval", random.intRange(1000, 8000))

# Chaos monkey clock starting and stopping !
random.addRandom(200, 500, "clock", "startClock")
random.addRandom(200, 500, "clock", "stopClock")

# disable single random event generator - must be explicit with name.method key
random.disable("python.roll_dice")

# you know longer should see the python.roll_dice event firing - since it was explicitly disabled

# stop events - but leave all random event data
# allows re-enabling
# random.disable()

# enable events
# random.enable()

# stop events and clear all random event ata
# random.purge()
