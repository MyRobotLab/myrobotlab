# MyRobotLab — multi-stage image
# Build:  docker build -t myrobotlab .
# Run:    see doc/docker.md

# ---------- build ----------
FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /build

COPY pom.xml assembly.xml ./
# Warm the local Maven repo when possible (network required).
RUN mvn -B -DskipTests dependency:go-offline || true

COPY src ./src
# Shade produces target/myrobotlab.jar (Main-Class: org.myrobotlab.service.Runtime)
RUN mvn -B -DskipTests -Dmaven.gitcommitid.skip=true package

# ---------- runtime ----------
FROM eclipse-temurin:11-jre-jammy

LABEL org.opencontainers.image.title="MyRobotLab" \
      org.opencontainers.image.description="Open Source Framework for Robotics and Creative Machine Control" \
      org.opencontainers.image.url="https://myrobotlab.org" \
      org.opencontainers.image.source="https://github.com/MyRobotLab/myrobotlab"

# Host device access helpers:
# - v4l-utils / libv4l : webcams (/dev/video*)
# - udev               : stable device node handling
# Serial ports (/dev/ttyUSB*, /dev/ttyACM*) need only the device mount + dialout.
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates \
      curl \
      udev \
      v4l-utils \
      libv4l-0 \
      libusb-1.0-0 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -f dialout \
    && groupadd -f video

ENV MRL_HOME=/opt/mrl \
    JAVA_OPTS="-Xms256m -Xmx2g -Djava.library.path=libraries/native -Djna.library.path=libraries/native -Dfile.encoding=UTF-8" \
    # Match Launcher: myrobotlab.jar first, then Ivy deps in libraries/jar.
    # Never start with `java -jar` alone — that omits libraries/jar from the classpath.
    MRL_CLASSPATH="/opt/mrl/myrobotlab.jar:/opt/mrl/libraries/jar/*" \
    # NVIDIA Container Toolkit (used when started with --gpus / compose deploy.resources)
    NVIDIA_VISIBLE_DEVICES=all \
    NVIDIA_DRIVER_CAPABILITIES=compute,utility,video

WORKDIR ${MRL_HOME}

COPY --from=build /build/target/myrobotlab.jar ${MRL_HOME}/myrobotlab.jar

# Install ALL service Ivy dependencies into libraries/ so the image is ready to run
# without a first-boot download. Requires network during `docker build`.
# Placed before config COPY so config tweaks do not invalidate this expensive layer.
# --id docker avoids null-id bootstrap issues if a runtime.yml has id: null.
# Dev shortcut: docker build --build-arg INSTALL_ALL=false ...
RUN mkdir -p ${MRL_HOME}/libraries ${MRL_HOME}/data/config
ARG INSTALL_ALL=true
RUN if [ "${INSTALL_ALL}" = "true" ]; then \
      echo "Installing all MyRobotLab service dependencies..." && \
      java \
        -Xms256m -Xmx2g \
        -Djava.library.path=libraries/native \
        -Djna.library.path=libraries/native \
        -Dfile.encoding=UTF-8 \
        -cp "${MRL_CLASSPATH}" org.myrobotlab.service.Runtime --id docker --install; \
    else \
      echo "Skipping full install (INSTALL_ALL=${INSTALL_ALL})"; \
    fi

COPY docker/entrypoint.sh ${MRL_HOME}/entrypoint.sh
COPY docker/config/ ${MRL_HOME}/docker-config/

RUN chmod +x ${MRL_HOME}/entrypoint.sh \
    # Bake sample configs into the image so VOLUME init / first boot have them
    && cp -a ${MRL_HOME}/docker-config/default ${MRL_HOME}/data/config/default \
    && cp -a ${MRL_HOME}/docker-config/inmoov ${MRL_HOME}/data/config/inmoov

# Persist runtime data/config. Do not VOLUME-mount resource/ — an empty volume
# would hide jar-extracted WebGui assets (FileIO skips extract if dir exists).
VOLUME ["${MRL_HOME}/data"]

EXPOSE 8888

ENTRYPOINT ["/opt/mrl/entrypoint.sh"]
# Default services (matches myrobotlab.sh): Log, Security, WebGui, Intro, Python
CMD ["--log-level", "info", "-s", "log", "Log", "security", "Security", "webgui", "WebGui", "intro", "Intro", "python", "Python"]
