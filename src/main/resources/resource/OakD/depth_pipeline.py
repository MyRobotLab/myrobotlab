# OAK-D stereo depth → MyRobotLab OakD.onStridedDepth (Py4j).
# Java prepends OAKD_SERVICE, OAKD_STOP, OAKD_RUN_ID, OAKD_FPS, OAKD_STRIDE, ...
# DepthAI 3.x is the install target (wheels through CPython 3.14). 2.x remains
# as a fallback if an older venv still has depthai 2.29.
import base64
import threading
import time

try:
    _oakd_run_id
except NameError:
    _oakd_run_id = 0


def _dai_major(dai):
    try:
        return int(str(dai.__version__).split(".")[0])
    except Exception:
        return 3


def _try_get(q):
    if q is None:
        return None
    if hasattr(q, "tryGet"):
        return q.tryGet()
    try:
        return q.get()
    except Exception:
        return None


def _intrinsics(dai, calib, w, h):
    fx = fy = float(w) * 0.9
    cx = w / 2.0
    cy = h / 2.0
    if calib is None:
        return fx, fy, cx, cy
    for name in ("CAM_B", "CAM_A", "LEFT", "RGB"):
        try:
            socket = getattr(dai.CameraBoardSocket, name)
            mat = calib.getCameraIntrinsics(socket, w, h)
            return float(mat[0][0]), float(mat[1][1]), float(mat[0][2]), float(mat[1][2])
        except Exception:
            continue
    return fx, fy, cx, cy


# Stereo 400P. Width/height must be multiples of 16. RGB sensors (e.g. IMX214
# half-width 2104) are not — setDepthAlign without setOutputSize crashes the
# device: "Disparity/depth width must be multiple of 16, but RGB camera width is 2104".
DEPTH_W = 640
DEPTH_H = 400


def _depth_queue(stereo):
    try:
        return stereo.depth.createOutputQueue(maxSize=4, blocking=False)
    except TypeError:
        return stereo.depth.createOutputQueue()


def _align_depth_to_rgb(dai, stereo):
    """Warp depth into the RGB camera frame at DEPTH_W x DEPTH_H (not native RGB size)."""
    # Pin size first so a failed align cannot leave StereoDepth at native RGB width.
    try:
        stereo.setOutputSize(DEPTH_W, DEPTH_H)
    except Exception as e:
        print("setOutputSize (before align):", e)
    try:
        stereo.setDepthAlign(dai.CameraBoardSocket.CAM_A)
    except Exception:
        try:
            stereo.setDepthAlign(dai.CameraBoardSocket.RGB)
        except Exception as e:
            print("setDepthAlign skipped:", e)
            return False
    try:
        stereo.setOutputSize(DEPTH_W, DEPTH_H)
    except Exception as e:
        print("setOutputSize failed (RGB align needs an explicit size):", e)
        return False
    return True


def _jpeg_b64(pkt, cache):
    if pkt is None:
        return ""
    img = None
    try:
        img = pkt.getCvFrame()
    except Exception:
        try:
            img = pkt.getFrame()
        except Exception:
            return ""
    if img is None:
        return ""
    try:
        import cv2

        if len(img.shape) == 2:
            img = cv2.cvtColor(img, cv2.COLOR_GRAY2BGR)
        elif len(img.shape) == 3 and img.shape[2] == 4:
            img = cv2.cvtColor(img, cv2.COLOR_BGRA2BGR)
        ok, buf = cv2.imencode(".jpg", img, [int(cv2.IMWRITE_JPEG_QUALITY), 55])
        if not ok:
            return ""
        return base64.b64encode(buf.tobytes()).decode("ascii")
    except Exception as e:
        if not cache.get("jpeg_warned"):
            print("rgb jpeg encode failed (install opencv-python):", e)
            cache["jpeg_warned"] = True
        return ""


def _publish_depth(oakd, dai, np, pkt, stride, calib, cache, rgb_pkt=None):
    depth = pkt.getFrame()
    h, w = int(depth.shape[0]), int(depth.shape[1])
    if cache.get("fx") is None:
        fx, fy, cx, cy = _intrinsics(dai, calib, w, h)
        cache["fx"], cache["fy"], cache["cx"], cache["cy"] = fx, fy, cx, cy
    sampled = depth[::stride, ::stride]
    jpeg = _jpeg_b64(rgb_pkt, cache)
    oakd.onStridedDepth(
        w,
        h,
        stride,
        sampled.astype("int32").flatten().tolist(),
        cache["fx"],
        cache["fy"],
        cache["cx"],
        cache["cy"],
        jpeg,
    )


def _run_v3(dai, np, oakd, run_id):
    pipeline = dai.Pipeline()
    mono_left = pipeline.create(dai.node.Camera).build(dai.CameraBoardSocket.CAM_B)
    mono_right = pipeline.create(dai.node.Camera).build(dai.CameraBoardSocket.CAM_C)
    stereo = pipeline.create(dai.node.StereoDepth)

    fps = int(globals().get("OAKD_FPS", 12))

    def mono_out(cam):
        try:
            cap = dai.ImgFrameCapability()
            cap.size.fixed((DEPTH_W, DEPTH_H))
            cap.fps.fixed(float(fps))
            return cam.requestOutput(cap, True)
        except Exception:
            return cam.requestFullResolutionOutput()

    mono_out(mono_left).link(stereo.left)
    mono_out(mono_right).link(stereo.right)
    try:
        stereo.setRectification(True)
    except Exception:
        pass
    try:
        stereo.setDefaultProfilePreset(dai.node.StereoDepth.PresetMode.HIGH_DENSITY)
    except Exception:
        try:
            stereo.setDefaultProfilePreset(dai.node.StereoDepth.PresetMode.ROBOTICS)
        except Exception:
            pass
    stereo.setLeftRightCheck(True)

    rgb_queue = None
    try:
        cam_rgb = pipeline.create(dai.node.Camera).build(dai.CameraBoardSocket.CAM_A)
        cap = dai.ImgFrameCapability()
        cap.size.fixed((DEPTH_W, DEPTH_H))
        cap.fps.fixed(float(fps))
        try:
            rgb_out = cam_rgb.requestOutput(cap, False)
        except Exception:
            rgb_out = cam_rgb.requestOutput(cap, True)
        try:
            rgb_queue = rgb_out.createOutputQueue(maxSize=4, blocking=False)
        except TypeError:
            rgb_queue = rgb_out.createOutputQueue()
        _align_depth_to_rgb(dai, stereo)
    except Exception as e:
        print("rgb camera disabled:", e)
        rgb_queue = None

    depth_queue = _depth_queue(stereo)
    stride = max(1, int(globals().get("OAKD_STRIDE", 8)))
    cache = {}
    spatial = bool(globals().get("OAKD_SPATIAL", False))
    if spatial:
        print("spatial detections are not wired on DepthAI 3.x; depth-only pipeline")

    try:
        with pipeline:
            pipeline.start()
            oakd.onHardwareStarted()
            calib = None
            try:
                device = pipeline.getDefaultDevice()
                try:
                    calib = device.readCalibration()
                except Exception:
                    calib = device.readCalibration2()
            except Exception:
                pass
            last_rgb = None
            while (
                globals().get("OAKD_RUN_ID") == run_id
                and not globals().get("OAKD_STOP", False)
                and pipeline.isRunning()
            ):
                pkt = _try_get(depth_queue)
                if pkt is None:
                    time.sleep(0.01)
                    continue
                rgb_pkt = _try_get(rgb_queue)
                if rgb_pkt is not None:
                    last_rgb = rgb_pkt
                _publish_depth(oakd, dai, np, pkt, stride, calib, cache, last_rgb)
    except Exception as e:
        oakd.onHardwareFailed(str(e))
        return
    try:
        pipeline.stop()
    except Exception:
        pass


def _run_v2(dai, np, oakd, run_id):
    pipeline = dai.Pipeline()
    mono_left = pipeline.create(dai.node.MonoCamera)
    mono_right = pipeline.create(dai.node.MonoCamera)
    stereo = pipeline.create(dai.node.StereoDepth)

    try:
        mono_left.setCamera("left")
        mono_right.setCamera("right")
    except Exception:
        mono_left.setBoardSocket(dai.CameraBoardSocket.CAM_B)
        mono_right.setBoardSocket(dai.CameraBoardSocket.CAM_C)

    mono_left.setResolution(dai.MonoCameraProperties.SensorResolution.THE_400_P)
    mono_right.setResolution(dai.MonoCameraProperties.SensorResolution.THE_400_P)
    fps = int(globals().get("OAKD_FPS", 12))
    try:
        mono_left.setFps(fps)
        mono_right.setFps(fps)
    except Exception:
        pass

    try:
        stereo.setDefaultProfilePreset(dai.node.StereoDepth.PresetMode.HIGH_DENSITY)
    except Exception:
        pass
    stereo.setLeftRightCheck(True)

    mono_left.out.link(stereo.left)
    mono_right.out.link(stereo.right)

    xout = pipeline.create(dai.node.XLinkOut)
    xout.setStreamName("depth")
    stereo.depth.link(xout.input)

    rgb_stream = False
    try:
        cam_rgb_tex = pipeline.create(dai.node.ColorCamera)
        try:
            cam_rgb_tex.setBoardSocket(dai.CameraBoardSocket.CAM_A)
        except Exception:
            cam_rgb_tex.setBoardSocket(dai.CameraBoardSocket.RGB)
        cam_rgb_tex.setPreviewSize(DEPTH_W, DEPTH_H)
        try:
            cam_rgb_tex.setVideoSize(DEPTH_W, DEPTH_H)
        except Exception:
            pass
        cam_rgb_tex.setInterleaved(False)
        try:
            cam_rgb_tex.setColorOrder(dai.ColorCameraProperties.ColorOrder.BGR)
        except Exception:
            pass
        try:
            cam_rgb_tex.setFps(fps)
        except Exception:
            pass
        xout_rgb = pipeline.create(dai.node.XLinkOut)
        xout_rgb.setStreamName("rgb")
        cam_rgb_tex.preview.link(xout_rgb.input)
        rgb_stream = True
        _align_depth_to_rgb(dai, stereo)
    except Exception as e:
        print("rgb camera disabled:", e)

    spatial = bool(globals().get("OAKD_SPATIAL", False))
    blob = globals().get("OAKD_BLOB", "") or ""
    nn_out = None
    if spatial:
        try:
            blob_path = blob
            if not blob_path:
                import blobconverter

                blob_path = blobconverter.from_zoo(name="yolo-v4-tiny-tf", shaves=6)
            cam_rgb = pipeline.create(dai.node.ColorCamera)
            cam_rgb.setPreviewSize(416, 416)
            cam_rgb.setInterleaved(False)
            cam_rgb.setColorOrder(dai.ColorCameraProperties.ColorOrder.BGR)
            spatial_nn = pipeline.create(dai.node.YoloSpatialDetectionNetwork)
            spatial_nn.setBlobPath(blob_path)
            spatial_nn.setConfidenceThreshold(float(globals().get("OAKD_CONFIDENCE", 0.5)))
            spatial_nn.input.setBlocking(False)
            spatial_nn.setBoundingBoxScaleFactor(0.5)
            spatial_nn.setDepthLowerThreshold(int(globals().get("OAKD_MIN_MM", 300)))
            spatial_nn.setDepthUpperThreshold(int(globals().get("OAKD_MAX_MM", 4000)))
            cam_rgb.preview.link(spatial_nn.input)
            stereo.depth.link(spatial_nn.inputDepth)
            nn_out = pipeline.create(dai.node.XLinkOut)
            nn_out.setStreamName("nn")
            spatial_nn.out.link(nn_out.input)
        except Exception as e:
            print("spatial detections disabled:", e)
            nn_out = None

    try:
        device = dai.Device(pipeline)
    except Exception as e:
        oakd.onHardwareFailed(str(e))
        return

    oakd.onHardwareStarted()
    q_depth = device.getOutputQueue("depth", maxSize=4, blocking=False)
    q_rgb = device.getOutputQueue("rgb", maxSize=4, blocking=False) if rgb_stream else None
    q_nn = device.getOutputQueue("nn", maxSize=4, blocking=False) if nn_out is not None else None

    calib = None
    try:
        calib = device.readCalibration()
    except Exception:
        pass

    labels = [
        "person", "bicycle", "car", "motorbike", "aeroplane", "bus", "train", "truck",
        "boat", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench",
        "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra",
        "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove",
        "skateboard", "surfboard", "tennis racket", "bottle", "wine glass", "cup",
        "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
        "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "sofa",
        "pottedplant", "bed", "diningtable", "toilet", "tvmonitor", "laptop", "mouse",
        "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
        "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
        "toothbrush",
    ]

    stride = max(1, int(globals().get("OAKD_STRIDE", 8)))
    cache = {}
    last_rgb = None
    try:
        while globals().get("OAKD_RUN_ID") == run_id and not globals().get("OAKD_STOP", False):
            pkt = q_depth.tryGet()
            if pkt is None:
                time.sleep(0.01)
                continue
            rgb_pkt = q_rgb.tryGet() if q_rgb is not None else None
            if rgb_pkt is not None:
                last_rgb = rgb_pkt
            _publish_depth(oakd, dai, np, pkt, stride, calib, cache, last_rgb)
            if q_nn is not None:
                det_pkt = q_nn.tryGet()
                if det_pkt is not None:
                    for d in det_pkt.detections:
                        label = labels[d.label] if 0 <= d.label < len(labels) else str(d.label)
                        sc = d.spatialCoordinates
                        oakd.onDetection(
                            label,
                            float(d.confidence),
                            float(d.xmin),
                            float(d.ymin),
                            float(d.xmax),
                            float(d.ymax),
                            float(sc.x) / 1000.0,
                            float(sc.y) / 1000.0,
                            float(sc.z) / 1000.0,
                        )
    finally:
        try:
            device.close()
        except Exception:
            pass


def _oakd_depth_loop(run_id):
    oakd = runtime.getService(OAKD_SERVICE)
    if oakd is None:
        print("OakD service not found:", OAKD_SERVICE)
        return
    try:
        import depthai as dai
        import numpy as np
    except Exception as e:
        oakd.onHardwareFailed("depthai import failed: " + str(e))
        return

    try:
        if _dai_major(dai) >= 3:
            _run_v3(dai, np, oakd, run_id)
        else:
            _run_v2(dai, np, oakd, run_id)
    except AttributeError:
        try:
            _run_v2(dai, np, oakd, run_id)
        except Exception as e:
            oakd.onHardwareFailed(str(e))
    except Exception as e:
        oakd.onHardwareFailed(str(e))


OAKD_STOP = bool(globals().get("OAKD_STOP", False))
_oakd_run_id = int(globals().get("OAKD_RUN_ID", 1))
_oakd_thread = threading.Thread(target=_oakd_depth_loop, args=(_oakd_run_id,), daemon=True)
_oakd_thread.start()
