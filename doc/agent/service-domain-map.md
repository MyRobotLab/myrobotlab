# Service domain map

Use this map to pick the right package when fixing a bug. Categories come from `*Meta.addCategory(...)`. Sponsors in Meta are informal maintainers, not GitHub CODEOWNERS.

## Framework core (this repo)

| Domain | Look here first |
|--------|-----------------|
| Process / registry / install | `org.myrobotlab.service.Runtime` |
| Service base / messaging | `org.myrobotlab.framework.Service`, `Inbox`, `Outbox`, `Message`, `MethodCache` |
| Serialization | `org.myrobotlab.codec.CodecUtils` |
| Repo / Ivy / Maven wrapper | `org.myrobotlab.framework.repo.*` |
| Config model | `org.myrobotlab.service.config.*`, `org.myrobotlab.config.*` |
| Meta / discovery | `org.myrobotlab.service.meta.*` |
| Logging | `org.myrobotlab.logging.*` |

## Domains → typical services

| Domain | Categories (Meta) | Examples / packages |
|--------|-------------------|---------------------|
| Web UI / display | `display` | `WebGui`, `resource/WebGui/` |
| Scripting | `programming` | `Python`, `Py4j`, `JavaScript`, `Blocks` |
| Vision | `vision`, `video` | `OpenCV`, `opencv/*`, `Webcam`, `BoofCV` |
| Speech out | `speech`, `sound` | `MarySpeech`, `Polly`, `WebkitSpeechSynthesis`, abstracts in `meta/abstracts` |
| Speech in | `speech recognition` | `WebkitSpeechRecognition`, `Sphinx`, `VoskSpeechRecognition` |
| Chat / AI | `ai`, `chatbot` | `ProgramAB` (sibling repo resources), `DiscordBot`, `LLM`, `Gpt3`, `OpenAI` |
| Servo / motor | `servo`, `motor`, `control` | `Servo`, `DiyServo`, `Adafruit16CServoDriver`, `Sabertooth`, `RoboClaw` |
| Inverse kinematics | `robot`, `control` | `InverseKinematics3D` (Jacobian / DH), `Fabrik` (FABRIK) |
| Microcontroller | `microcontroller`, `i2c` | `Arduino`, `VirtualArduino`, `RasPi`, `Esp8266`, `Mpu6050` |
| Sensors | `sensors`, `encoder` | `Pir`, `Lidar`, `Gps`, `Ads1115`, `LeapMotion` |
| Robot bodies | `robot` | `InMoov2*` (often sibling repo), `Arm`, `SpotMicro`, `Roomba` |
| Network / cloud | `cloud`, `network` | `Email`, `Twitter`, `Osc`, `KafkaConnector` |
| Search / data | `search`, `ingest` | `Solr`, `DocumentPipeline`, connectors |
| Simulation | `simulator` | `VirtualArduino`, `JMonkeyEngine` |
| Testing helpers | `testing`, `framework` | `TestCatcher`, `TestThrower`, `VirtualDevice`, `MockGateway` |

## Sibling repos (often out of tree)

| Product area | Repo | Local path when linked |
|--------------|------|------------------------|
| InMoov robot UI / peers | InMoov2 | `src/main/resources/resource/InMoov2` |
| AIML chatbot brains | ProgramAB | `src/main/resources/resource/ProgramAB` |

If a bug is “InMoov face tracking” or “ProgramAB bot file”, confirm whether the fix belongs in **this** repo or a sibling.

## UI stacks

| Stack | Role | Path |
|-------|------|------|
| WebGui (AngularJS) | Primary UI | `src/main/resources/resource/WebGui/` |
| Python service scripts | Secondary API | `resource/<Service>/*.py` |
| SwingGui | Legacy / optional Meta | Meta may exist without full service in-tree |
| React | Experimental / often gitignored | `resource/WebGui/react` |

## Adding a service

Full walkthrough: **[adding-a-service.md](adding-a-service.md)** (triple, Meta/Config, resources, logo, WebGui, tests, checklist).

Short version:

1. Copy `_TemplateService`, `_TemplateServiceConfig`, `_TemplateServiceMeta`
2. Rename types; set `addCategory`, `setAvailable(true)`, deps in Meta (mirror deps in `pom.xml`)
3. Add `resource/YourService/` samples (`YourService.py`, `yourservice.yml`) and `resource/YourService.png`
4. Optional but usual: WebGui `YourServiceGui.js` + `views/YourServiceGui.html`
5. Add a focused `YourServiceTest` under `src/test/java`
6. Update this domain map if the service is a notable new entry
