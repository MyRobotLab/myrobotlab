def onNewFrame(frame, source):
    pass


def onShowFrame(frame, source):
    pass

labels = ["background",  "aeroplane",  "bicycle",  "bird",  "boat",  "bottle",  "bus",  "car",  "cat",  "chair",  "cow",  "diningtable",  "dog",  "horse",  "motorbike",  "person",  "pottedplant",  "sheep",  "sofa",  "train",  "tvmonitor"]


def onNn(nn_packet, decoded_data):
    oakd = runtime.getService(globals().get("OAKD_SERVICE", "oakd"))
    if oakd is None:
        return
    for detection in decoded_data:
        sc = detection.spatialCoordinates
        oakd.onDetection(
            labels[detection.label],
            float(detection.confidence),
            float(detection.xmin),
            float(detection.ymin),
            float(detection.xmax),
            float(detection.ymax),
            float(sc.x) / 1000.0,
            float(sc.y) / 1000.0,
            float(sc.z) / 1000.0,
        )


def onReport(report):
    pass


def onSetup(*args, **kwargs):
    pass


def onTeardown(*args, **kwargs):
    pass


def onIter(*args, **kwargs):
    pass
