##################################################################################
# Deeplearning4j.py
# description: A wrapper service for the Deeplearning4j framework.
# categories: ai
# more info @: http://myrobotlab.org/service/Deeplearning4j
##################################################################################

# start the deeplearning4j service
deeplearning4j = runtime.start('deeplearning4j','Deeplearning4j')

# load the VGG16 model from the zoo
deeplearning4j.loadVGG16()

# run an image file through the model and get the classifications / confidence
classifications = deeplearning4j.classifyImageFileVGG16("image0-1.png")

# print them out... it's a dictionary/map of label to confidence level (between 0-1)
for label in classifications:
  print(label + " : " + str(classifications.get(label)))

