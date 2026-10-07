##################################################################################
# TesseractOcr.py
# description: Extract text from images with Tesseract 5.
# more info @: http://myrobotlab.org/service/TesseractOcr
##################################################################################

tesseractocr = runtime.start("tesseractocr", "TesseractOcr")

# Page segmentation: 3=auto (documents), 7=single line (signs), 11=sparse
tesseractocr.setPsmPreset("auto")

result = tesseractocr.recognize("src/test/resources/OpenCV/i_am_a_droid.jpg")
print("text:", result.text)
print("confidence:", result.meanConfidence)

# Camera / scene text: add OpenCV filter type "Ocr" and point it at this service
# opencv = runtime.start("opencv", "OpenCV")
# opencv.addFilter("ocr", "Ocr")
