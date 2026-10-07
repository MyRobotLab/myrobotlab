# Running MyRobotLab in Docker

This repo includes a `Dockerfile` that builds `myrobotlab.jar`, runs a **full** `Runtime --install` (every service’s Ivy dependencies), and starts **WebGui on port 8888**.

| Item | Value |
|------|--------|
| Image workdir | `/opt/mrl` |
| Web UI | `http://localhost:8888` |
| Config root | `/opt/mrl/data/config/<configName>/` |
| Default start | `-s log Log security Security webgui WebGui intro Intro python Python` |
| Sample InMoov config | `docker/config/inmoov` → `-c inmoov` |
| Service deps | Installed into `/opt/mrl/libraries` at **image build** time |

## Prerequisites

- Docker Engine 20+ (Linux recommended for serial / camera / GPU passthrough)
- Network access during `docker build` (Ivy downloads all service jars/natives)
- Optional: [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html) for GPU access
- Optional: Docker Compose v2

> **Windows Docker Desktop:** WebGui port publishing works. USB serial and webcams are **not** available via `--device` alone — you must bridge USB into WSL2 with [usbipd-win](https://github.com/dorssel/usbipd-win) first. See [Windows Docker Desktop (serial and webcams)](#windows-docker-desktop-serial-and-webcams).
>
> **macOS Docker Desktop:** USB serial and webcam passthrough are limited; prefer a Linux host (or Jetson) for Arduino / cameras / GPUs.

## Build

From the repository root (full install makes the first build slow; later layers cache):

```bash
docker build -t myrobotlab .
```

Skip the full install only for local Dockerfile experiments:

```bash
docker build -t myrobotlab --build-arg INSTALL_ALL=false .
```

Or with Compose:

```bash
docker compose build
```

## Quick start (WebGui)

```bash
docker run --rm -p 8888:8888 --name myrobotlab myrobotlab
```

Open [http://localhost:8888](http://localhost:8888). No install step is required at runtime — dependencies are already in the image. The default command starts **Log**, **Security**, **WebGui**, **Intro**, and **Python**.

Equivalent Compose:

```bash
docker compose up
```

## Hardware access (serial, video, GPU)

The examples in this section assume a **Linux** Docker host (or Linux VM) where device nodes already exist. Adjust names to match your machine (`ls /dev/ttyACM* /dev/ttyUSB* /dev/video*`).

On **Windows**, skip ahead to [Windows Docker Desktop (serial and webcams)](#windows-docker-desktop-serial-and-webcams) — `--device=/dev/video0` alone will not expose a Windows webcam.

### Serial (Arduino / USB-UART)

```bash
docker run --rm -p 8888:8888 \
  --device=/dev/ttyACM0 \
  --device=/dev/ttyACM1 \
  --group-add dialout \
  --name myrobotlab \
  myrobotlab
```

Inside the container, configure Arduino / InMoov ports as `/dev/ttyACM0`, `/dev/ttyUSB0`, etc. (not `COMx`).

### Video (V4L2 webcams)

```bash
docker run --rm -p 8888:8888 \
  --device=/dev/video0 \
  --group-add video \
  --name myrobotlab \
  myrobotlab
```

### GPU (NVIDIA)

Requires the NVIDIA Container Toolkit on the host:

```bash
docker run --rm -p 8888:8888 \
  --gpus all \
  -e NVIDIA_DRIVER_CAPABILITIES=compute,utility,video \
  --name myrobotlab \
  myrobotlab
```

### All together

```bash
docker run --rm -p 8888:8888 \
  --gpus all \
  --device=/dev/ttyACM0 \
  --device=/dev/ttyACM1 \
  --device=/dev/video0 \
  --group-add dialout \
  --group-add video \
  -v mrl-data:/opt/mrl/data \
  --name myrobotlab \
  myrobotlab
```

For a catch-all device tree on trusted Linux hosts you can use `--privileged -v /dev:/dev` instead of listing each `--device`. Prefer explicit devices when possible.

Edit `docker-compose.yml` and uncomment the `devices:` / GPU sections to persist the same settings under Compose.

## Windows Docker Desktop (serial and webcams)

Docker Desktop on Windows runs Linux containers inside a **WSL2** VM. OpenCV in the container expects Linux V4L2 nodes (`/dev/video*`), and Arduino expects `/dev/ttyACM*` / `/dev/ttyUSB*` — not Windows `COMx` or DirectShow cameras.

Docker Desktop does **not** passthrough host USB natively. Bridge devices with [usbipd-win](https://github.com/dorssel/usbipd-win) (see also [Connect USB devices to WSL](https://learn.microsoft.com/en-us/windows/WSL/connect-usb)):

```
Windows USB device  →  usbipd attach --wsl  →  WSL2 / docker-desktop (/dev/…)  →  docker --device / privileged
```

While a device is attached to WSL, Windows apps cannot use it.

### Install usbipd-win (one time)

In an **elevated** PowerShell:

```powershell
winget install --interactive --exact dorssel.usbipd-win
wsl --update
wsl --shutdown
```

### List, bind, and attach devices

```powershell
usbipd list
```

Example output:

```text
BUSID  VID:PID    DEVICE                          STATE
2-3    2341:0043  Arduino Uno                     Not shared
2-4    046d:0825  Logitech USB Webcam             Not shared
2-5    1a86:7523  USB-SERIAL CH340 (COM5)         Not shared
```

Share (`bind`, admin once per device) and attach to WSL (each session, or after unplug/reboot):

```powershell
# Serial / Arduino (replace busid with yours)
usbipd bind --busid 2-5
usbipd attach --wsl --busid 2-5

# Webcam
usbipd bind --busid 2-4
usbipd attach --wsl --busid 2-4
```

Confirm Linux device nodes exist in the Docker Desktop distro:

```powershell
wsl -d docker-desktop -- ls -l /dev/ttyACM* /dev/ttyUSB* /dev/video*
wsl -d docker-desktop -- lsusb
```

You want nodes such as `/dev/ttyUSB0` and `/dev/video0`. USB-serial adapters usually work with the stock WSL kernel. Webcams often need UVC/V4L2 support — see [Webcam caveat](#webcam-caveat-wsl-kernel) below.

### Start the container with serial and video

After the nodes exist under WSL/`docker-desktop`, pass them into the container.

Catch-all (convenient on a trusted dev machine):

```powershell
docker run --rm -p 8888:8888 `
  --privileged `
  -v /dev:/dev `
  --group-add dialout `
  --group-add video `
  --name myrobotlab `
  myrobotlab
```

Explicit devices (preferred when node names are stable):

```powershell
docker run --rm -p 8888:8888 `
  --device=/dev/ttyUSB0 `
  --device=/dev/ttyACM0 `
  --device=/dev/video0 `
  --device=/dev/video1 `
  --group-add dialout `
  --group-add video `
  --name myrobotlab `
  myrobotlab
```

Many UVC cameras create **two** nodes (`video0` capture + `video1` metadata); map both if both exist.

Compose equivalent (`docker-compose.yml`):

```yaml
privileged: true
volumes:
  - mrl-data:/opt/mrl/data
  - /dev:/dev
group_add:
  - dialout
  - video
# Or explicit devices instead of privileged + /dev:
# devices:
#   - /dev/ttyUSB0:/dev/ttyUSB0
#   - /dev/video0:/dev/video0
#   - /dev/video1:/dev/video1
```

```powershell
docker compose up
```

### Verify inside the container

```powershell
docker exec -it myrobotlab bash
```

```bash
ls -l /dev/video* /dev/ttyUSB* /dev/ttyACM*
v4l2-ctl --list-devices
```

In OpenCV / InMoov config use camera index `0` (or whichever `v4l2-ctl` lists). For Arduino use `/dev/ttyUSB0` / `/dev/ttyACM0`, **not** `COM5`.

### Session cheat sheet

```powershell
# One-time
winget install --interactive --exact dorssel.usbipd-win
wsl --update

# Each session (bus IDs from `usbipd list`)
usbipd bind --busid 2-5          # once per device
usbipd attach --wsl --busid 2-5  # serial
usbipd attach --wsl --busid 2-4  # webcam

wsl -d docker-desktop -- ls /dev/ttyUSB* /dev/ttyACM* /dev/video*

docker run --rm -p 8888:8888 `
  --privileged -v /dev:/dev `
  --group-add dialout --group-add video `
  --name myrobotlab `
  myrobotlab
```

### Webcam caveat (WSL kernel)

If after `usbipd attach` the camera appears in `lsusb` but **`/dev/video*` is missing**, the WSL kernel likely lacks UVC/media drivers. Stock WSL often includes common USB-serial drivers but not full webcam support.

Options:

1. Build a custom WSL2 kernel with media/UVC support (`CONFIG_MEDIA_SUPPORT`, `CONFIG_USB_VIDEO_CLASS`) — see the [usbipd-win WSL wiki](https://github.com/dorssel/usbipd-win/wiki/WSL-support).
2. If `/dev/video0` exists in `docker-desktop` but the container cannot open it, fix ownership in that distro, then restart the container:

   ```powershell
   wsl -d docker-desktop -- sh -c "chown 1000:1000 /dev/video0 /dev/video1 2>/dev/null; ls -l /dev/video*"
   ```

3. Run MyRobotLab **natively on Windows** for OpenCV (DirectShow / COM ports), or run Docker on a **Linux** host where `--device=/dev/video0` works as in the Linux section above.

## Injecting a custom configuration (InMoov)

MyRobotLab loads a **config set**: a directory under `data/config/<name>/` that must contain at least `runtime.yml`. The `runtime.yml` `registry` list is the ordered set of services to start; each named service needs a matching `<service>.yml` in that directory.

CLI flag: `-c` / `--config` (see `CmdOptions`).

### Sample InMoov config shipped in the image

The image includes `docker/config/inmoov`, which is seeded into `data/config/inmoov` on first boot:

| File | Role |
|------|------|
| `runtime.yml` | Starts `log`, `security`, `webgui`, `intro`, `python`, and `i01` (InMoov2) |
| `webgui.yml` | Port **8888**, `autoStartBrowser: false` |
| `python.yml` | Python service |
| `i01.yml` | InMoov2 service |

Start it (InMoov/OpenCV/Arduino deps are already installed in the image):

```bash
docker run --rm -p 8888:8888 \
  --device=/dev/ttyACM0 \
  --device=/dev/ttyACM1 \
  --device=/dev/video0 \
  --group-add dialout \
  --group-add video \
  --gpus all \
  --name myrobotlab \
  myrobotlab --log-level info -c inmoov
```

Or with Compose, override the command:

```bash
docker compose run --service-ports myrobotlab --log-level info -c inmoov
```

### Mount your own config from the host

1. Create a host directory (copy the sample as a starting point):

```bash
mkdir -p ./my-inmoov-config
cp -a docker/config/inmoov/. ./my-inmoov-config/
# edit runtime.yml / i01.yml / add peer overrides (i01.left.yml, …)
```

2. Bind-mount it over the container config path and select it with `-c`:

```bash
docker run --rm -p 8888:8888 \
  --device=/dev/ttyACM0 \
  --device=/dev/video0 \
  --group-add dialout \
  --group-add video \
  -v "$(pwd)/my-inmoov-config:/opt/mrl/data/config/inmoov" \
  --name myrobotlab \
  myrobotlab --log-level info -c inmoov
```

Compose equivalent (already sketched in `docker-compose.yml`):

```yaml
volumes:
  - ./my-inmoov-config:/opt/mrl/data/config/inmoov
command: ["--log-level", "info", "-c", "inmoov"]
```

### Minimal `runtime.yml` for InMoov

```yaml
!!org.myrobotlab.service.config.RuntimeConfig
logLevel: info
registry:
- runtime
- webgui
- python
- i01
resource: resource
type: Runtime
virtual: false
```

Set `virtual: true` to run without physical serial hardware.

Pair with `webgui.yml` (`port: 8888`, `autoStartBrowser: false`) and `i01.yml` (`type: InMoov2`). Peer overrides (Arduino ports, OpenCV camera index, etc.) are additional YAML files in the same directory.

## Persistence

| Volume / path | Purpose |
|---------------|---------|
| `/opt/mrl/data` | Config sets, service data |
| `/opt/mrl/libraries` | **Baked into the image** by `Runtime --install` during build |
| `/opt/mrl/resource` | Resources extracted from the jar (kept in the image; do not mount empty over this path) |

Avoid mounting an empty host directory or volume over `/opt/mrl/libraries` — that hides the pre-installed dependencies. If you do mount one and `libraries/repo.json` is missing, the entrypoint runs a full `--install` as a fallback.

## Entrypoint environment variables

| Variable | Meaning |
|----------|---------|
| `JAVA_OPTS` | JVM flags (library path, heap, encoding) |
| `MRL_INSTALL` | Set to `none` to skip the fallback full install when `libraries/repo.json` is missing |
| `NVIDIA_VISIBLE_DEVICES` | GPU visibility for NVIDIA Container Toolkit |

## Troubleshooting

- **WebGui not reachable** — confirm `-p 8888:8888` (or Compose `ports`) and that `webgui.yml` uses `port: 8888`.
- **No serial ports listed** — check `--device` mappings and `--group-add dialout`; nodes must exist on the host (on Windows: `usbipd attach` first, then confirm `/dev/tty*` under `docker-desktop`).
- **Camera fails in OpenCV** — map `/dev/videoN` and add `--group-add video`. On Windows, attach the USB camera with usbipd and confirm `/dev/video*` before `docker run` (see [Windows Docker Desktop](#windows-docker-desktop-serial-and-webcams)).
- **Windows: `lsusb` shows the webcam but no `/dev/video*`** — WSL kernel missing UVC/V4L2; custom kernel or run MRL/OpenCV outside Docker (see [Webcam caveat](#webcam-caveat-wsl-kernel)).
- **Windows: device busy / missing after attach** — detach with `usbipd detach --busid <id>`, unplug/replug, re-attach; only one of Windows or WSL can own the USB device at a time.
- **GPU not visible** — install NVIDIA Container Toolkit; use `--gpus all`; CUDA-accelerated natives may need a CUDA base image beyond the default JRE image.
- **Missing jars at runtime** — rebuild without masking `/opt/mrl/libraries`, or remove an old empty libraries volume (`docker volume rm …`).
- **`NoClassDefFoundError` / `ClassNotFoundException` for service deps (e.g. `org.bytedeco.opencv…`)** — the process must start with `-cp myrobotlab.jar:libraries/jar/*` (as `entrypoint.sh` / `Launcher` do). `java -jar myrobotlab.jar` alone does not put Ivy jars on the classpath.
- **`StringIndexOutOfBoundsException` in `Platform.getLocalInstance` at startup** — fixed in `Platform` for Docker builds that skip git metadata; rebuild the image so the updated jar is included.
- **Build is very slow / large** — expected: full `--install` downloads every service dependency. Use `--build-arg INSTALL_ALL=false` only for Dockerfile iteration.
