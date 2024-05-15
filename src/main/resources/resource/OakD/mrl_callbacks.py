import requests
import json

def onNewFrame(frame, source):
    pass


def onShowFrame(frame, source):
    pass

# found in config.json in the model directory
# e.g. resources/nn/mobilenet-ssd/mobilenet-ssd.json
labels = ["background",  "aeroplane",  "bicycle",  "bird",  "boat",  "bottle",  "bus",  "car",  "cat",  "chair",  "cow",  "diningtable",  "dog",  "horse",  "motorbike",  "person",  "pottedplant",  "sheep",  "sofa",  "train",  "tvmonitor"]
# TODO - debounce the detections

def onNn(nn_packet, decoded_data):
    # print(dir(nn_packet))
    # print(dir(decoded_data))
    # print(json.dumps(decoded_data))
    for detection in decoded_data:
        print(detection.spatialCoordinates.x)
        print(detection.spatialCoordinates.y)
        print(detection.spatialCoordinates.z)
        # print(detection.boundingBoxMapping)
        # print(dir(detection.boundingBoxMapping))
        print(labels[detection.label])
        print(detection.confidence)
        print(f'xmin {detection.xmin}')
        print(f'xmax {detection.xmin}')
        print(f'ymin {detection.ymin}')
        print(f'ymax {detection.xmin}')
        # Add detections to visualizer
        url = 'http://localhost:8888/api/service/i01/publishClassification'  # Replace with your URL
        data = {
            "xmin": detection.xmin,
            "xmax": detection.xmax,
            "ymin": detection.xmin,
            "ymax": detection.ymax,
            "label": labels[detection.label],
            "confidence": detection.confidence,
            "x": detection.spatialCoordinates.x,
            "y": detection.spatialCoordinates.y,
            "z": detection.spatialCoordinates.z
        }

        # Make the POST request with JSON data
        try:
            response = requests.post(url, json=[data])
        except requests.exceptions.HTTPError as http_err:
            print(f'HTTP error occurred: {http_err}')  # HTTP error
        except requests.exceptions.ConnectionError as conn_err:
            print(f'Connection error occurred: {conn_err}')  # Connection error
        except requests.exceptions.Timeout as timeout_err:
            print(f'Timeout error occurred: {timeout_err}')  # Timeout error
        except requests.exceptions.RequestException as req_err:
            print(f'An error occurred: {req_err}')  # Other errors

def onReport(report):
    pass


def onSetup(*args, **kwargs):
    pass


def onTeardown(*args, **kwargs):
    pass


def onIter(*args, **kwargs):
    pass
