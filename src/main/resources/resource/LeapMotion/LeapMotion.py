leap = runtime.start('leap','LeapMotion')

python.subscribe('leap', 'publishLeapData')

def onLeapData(data):
    # print(data)
    leftHand = data.leftHand
    rightHand = data.rightHand
    if leftHand:
        print("left",leftHand.thumb, leftHand.index, leftHand.middle, leftHand.ring, leftHand.pinky)
    if rightHand:
        print("right",rightHand.thumb, rightHand.index, rightHand.middle, rightHand.ring, rightHand.pinky)

