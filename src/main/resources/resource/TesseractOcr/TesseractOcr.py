##################################################################################
# TesseractOcr.py
# description: A service to wrap the OCR library Tesseract this extracts
#              text from images.  Typically scanned documents.
# categories: 
# more info @: http://myrobotlab.org/service/TesseractOcr
##################################################################################

# start the service
tesseractocr = runtime.start("tesseractocr","TesseractOcr")

text = tesseractocr.ocr("traffic-sign.jpg");

print("Found Text :" + text)