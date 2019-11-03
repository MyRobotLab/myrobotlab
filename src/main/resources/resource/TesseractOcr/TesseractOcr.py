# start the service
tesseractocr = Runtime.start("tesseractocr","TesseractOcr")

text = tesseract.ocr("traffic.sign.jpg");

print "Found Text :" + text