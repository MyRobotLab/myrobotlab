#########################################
# ChessGame.py
# description: chess game 
# categories: game, ai
# more info @: http://myrobotlab.org/service/ChessGame
#########################################

# we will virtualize an Arduino
virtual = True
port="COM3"

# start services
arduino = runtime.start("arduino","Arduino")
chessgame = runtime.start("chessgame","ChessGame")

# start optional virtual arduino service, used for test
if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)

#you have to replace COMX with your arduino serial port number
# arduino.connect("/dev/ttyUSB0") - Linux way
arduino.connect(port)

# wait 6 seconds for the game to start
sleep(6)

# subscribes to the game engines move method
python.subscribe(chessgame, "makeHMove")

print("game has started !")

# prints out all moves
def onMakeHMove(move):
  print('move is ', move)
  print('move is encoded as', move.from, move.to)
  print('sending data to arduino in custom msg')
  arduino.customMsg(move.from, move.to)

# moves pawn from b2 to b3
chessgame.move("b2-b3")

# wait for computer
sleep(15)

# moves pawn from c2 to c3
chessgame.move("c2-c3")

# wait for computer
sleep(15)

# moves pawn from d2 to d3
chessgame.move("d2-d3")
  
