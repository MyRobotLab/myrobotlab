# Generated files — do not hand-edit

Agents and contributors must regenerate these artifacts from their sources of truth.

## Arduino / MrlComm protocol

| Generated artifact | Source of truth |
|--------------------|-----------------|
| `src/main/java/org/myrobotlab/arduino/Msg.java` | `src/main/resources/resource/Arduino/generate/arduinoMsgs.schema` |
| `src/main/java/org/myrobotlab/arduino/VirtualMsg.java` | same schema + Java templates under `generate/` |
| Generated C++ / headers under Arduino resource tree | schema + templates in `src/main/resources/resource/Arduino/generate/` |

**Generator:** `org.myrobotlab.arduino.ArduinoMsgGenerator`

**How to regenerate (typical):**

```bash
# From repo root, with classpath built
mvn -q -DskipTests compile exec:java -Dexec.mainClass=org.myrobotlab.arduino.ArduinoMsgGenerator
```

If `exec:java` is not wired for this class in your environment, run `ArduinoMsgGenerator.main` from the IDE after `mvn compile`.

**Rule:** All message/method edits go in `arduinoMsgs.schema` (and templates), never by patching `Msg.java` / `VirtualMsg.java` bodies.

See also: `src/main/resources/resource/Arduino/generate/README.md`.

## Other build outputs (do not commit as sources)

| Path | Notes |
|------|-------|
| `target/` | Maven output |
| `libraries/` | Ivy/runtime downloads (cleaned by `mvn clean`) |
| `data/` | Runtime config/state (cleaned by `mvn clean`) |
| `src/main/resources/resource/framework/serviceData.json` | May be regenerated / cleaned |

## How to recognize generated Java

Generated Arduino message classes include a header similar to:

```
Welcome to Msg.java
Its created by running ArduinoMsgGenerator
...
All message editing should be done in the arduinoMsg.schema
```

If you see that banner, stop and edit the schema instead.
