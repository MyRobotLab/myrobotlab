# Arduino message generation

**Do not hand-edit** `org.myrobotlab.arduino.Msg` or `VirtualMsg`.

1. Edit `arduinoMsgs.schema` (and templates in this directory) for protocol changes.
2. Run `org.myrobotlab.arduino.ArduinoMsgGenerator` to regenerate Java/C++ artifacts.
3. Build and run Arduino-related tests / VirtualArduino checks.

See [`doc/GENERATED.md`](../../../../../../doc/GENERATED.md) and [`AGENTS.md`](../../../../../../AGENTS.md).
