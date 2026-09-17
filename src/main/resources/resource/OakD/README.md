# OakD (Luxonis OAK-D)

Stereo depth for InMoov / JMonkeyEngine via **DepthAI in the Py4j virtualenv**.

## What failed (`subprocess returned 1`)

`OakD` used to pin **`depthai==2.29.0`**. That release has **no binary wheel** for
CPython **3.13 or 3.14**. Pip downloaded the sdist and tried to compile (CMake /
NMake). The compile failed, Py4j only logged `subprocess returned 1`, and the
JSON blob next to it was `Py4j.getScriptList()` dumping config — not the pip
error.

**Fix:** install DepthAI **3.x** (`depthai>=3.2.0,<4`) with
`--only-binary=depthai` so pip never compiles. DepthAI 3.9+ ships a Windows
`cp314` wheel. The depth script speaks the v3 Camera API and still has a v2
fallback.

## Where packages land

| Item | Path |
|------|------|
| Shared Py4j venv | `data/Py4j/venv` |
| OakD scripts / instance data | `data/Py4j/<serviceName>` e.g. `oakd.py4j` |
| Interpreter | **system** `python3` or `python` on `PATH` (`Py4jConfig.useBundledPython` is unused) |

`mvn clean` deletes `data/`, so the venv is recreated on the next run.

## Pip packages

Installed by `OakD.installDepthAi()` when `OakDConfig.py4jInstall` is true
(default).

| Spec | Role |
|------|------|
| `depthai>=3.2.0,<4` | Required. Stereo depth. Binary wheels only. |
| `numpy` | Required (also a depthai dependency). |
| `opencv-python` | Required for RGB JPEG (mesh texture). |
| `blobconverter==1.4.3` | Optional. YOLO `.blob` download for **DepthAI 2.x** spatial detections. |

Python **3.9–3.14** with a depthai 3.x wheel is expected. There is no Java
`*Meta.addDependency` for these — they are Python, not Ivy/Maven.

## Hardware

USB 3 is recommended for live stereo. If the camera is missing, `syntheticFallback`
(default true) publishes a wall+box cloud for the simulator. Synthetic frames include
a gray wall and blue box so the RGB mesh looks distinct from the depth colormap.

## Voxel cloud vs RGB mesh

The OakD WebGui checkbox **RGB mesh** switches the JMonkeyEngine overlay:

| Checkbox | Overlay |
|----------|---------|
| Off (default) | Depth-colored voxel cubes |
| On | Organized triangle mesh textured from the color camera |

Live RGB is a JPEG from CAM_A (quality ~55) so Py4j does not send a full RGB int
list. `opencv-python` is required to encode that JPEG; without it the mesh falls
back to the depth colormap. Depth is aligned to RGB with `setDepthAlign(CAM_A)`
**and** `setOutputSize(640, 400)`. Aligning without an explicit size uses the RGB
sensor width (e.g. IMX214 half-res **2104**, not a multiple of 16) and DepthAI
crashes the device (`X_LINK_ERROR` / access violation).

Spatial YOLO (`enableSpatialDetections`) is still the DepthAI 2.x path; on 3.x
the pipeline logs that spatial is not wired and continues depth-only.

## Click a point for FABRIK

Left-click the visible overlay in JMonkeyEngine (RGB mesh or voxel cloud). The
simulator publishes that world-meter point (`publishClickPoint`). A running
**Fabrik** service receives `onPoint` and `moveTo`s the current arm there. The
green marker is the goal. Clicks on the robot still select; the floor grid,
frustum, and hand markers are ignored. Drag to orbit as usual.

Requires Fabrik attached to the simulator (VirtualInMoovIkDemo does this) or
Fabrik started while JME is already running.

## World scale (meters)

Depth is unprojected with the **RGB (CAM_A)** camera matrix into meters
(`Z = mm / 1000`). JMonkeyEngine uses the same meters as IK / VinMoov (X right,
Y up, Z forward). Overlay vertices are **not** parented under the character
mesh; the overlay node sits at the chest camera with local scale `1`.

If DepthAI returns a full-sensor `K` (principal point outside the 640×400
image), Java `DepthFrame.normalizeIntrinsics()` rescales it so XY extent is not
doll-sized at a correct Z.

Default overlay multiplier `depthCloudScale` is **1** (real-world). A yellow
**1 m stick** along the camera +Z is a visual check against InMoov.

If a known object still disagrees:

1. Click that object on the overlay.
2. Enter the tape-measured distance from the camera (meters).
3. **Calibrate** on the OakD or JMonkeyEngine WebGui (or
   `oakd.calibrateDepthScale(1.2)` / `jme.calibrateDepthScale(1.2)`).

That sets `simulator.depthCloudScale` so the click lands at the measured range.
Reset to `1.0` to return to the metric default.

## Manual install

```text
data/Py4j/venv/Scripts/python.exe -m pip install --upgrade --only-binary=depthai "depthai>=3.2.0,<4" numpy
```

On Linux/macOS use `data/Py4j/venv/bin/python`.
