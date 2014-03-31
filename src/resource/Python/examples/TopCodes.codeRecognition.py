from org.myrobotlab.opencv import OpenCVData
from org.myrobotlab.service import OpenCV

opencv = Runtime.create("opencv","OpenCV")
topcodes = Runtime.createAndStart("topcodes","TopCodes")
opencv.startService()
opencv.addFilter("PyramidDown1", "PyramidDown")

def input() :
 simage = opencv.getDisplay()
 codes = topcodes.scan(simage.getImage())
 limit = codes.size()
 for x in range(limit) :
    code = codes.get(x)
    print 'code number' , x ,'=', code.getCode()
# other information could be provided
    #print 'diameter' , x , 'is' , code.getDiameter()
    #print 'x of center' , x , 'is' , code.getCenterX()
    #print 'y of center' , x , 'is' , code.getCenterY()
    #print 'rotation' , x , 'is' , code.getOrientation() , 'radians'
opencv.addListener("publishOpenCVData", python.name, "input");
opencv.setCameraIndex(0)
opencv.capture()