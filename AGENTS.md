# AGENTS.md — MyRobotLab for AI / agentic development

This file is the orientation guide for automated agents (and humans) working in this repo.
Read it before changing framework code, dependencies, Arduino protocol, or WebGui.

More detail lives under [`doc/agent/`](doc/agent/).

## Quick facts

| Item | Value |
|------|--------|
| Language / JDK | Java 11 |
| Build | Single-module Maven (`pom.xml`) |
| Entry point | `org.myrobotlab.service.Runtime` |
| Primary UI | AngularJS WebGui (`src/main/resources/resource/WebGui/`) |
| Default branch | `develop` |
| Runtime deps | Per-service `*Meta` + Ivy → `libraries/` (not Maven alone) |

## Safe change surfaces (prefer these)

1. **One service**: `ServiceX.java` + `ServiceXConfig.java` + `ServiceXMeta.java` + `resource/ServiceX/`
2. **Service UI**: `resource/WebGui/app/service/js/ServiceXGui.js` (+ related HTML/views)
3. **Tests**: `src/test/java/...` mirroring the package under test
4. **Templates**: copy `_TemplateService*` when adding a new service

## Hotspots (high regression risk — minimize edits)

| File | Approx. size | Touch when… |
|------|-------------:|-------------|
| `service/Runtime.java` | ~5.4k lines | Process entry, registry, install, networking, config plans |
| `framework/Service.java` | ~3k lines | Inbox/outbox, invoke, peers, lifecycle, status |
| `codec/CodecUtils.java` | ~1.7k lines | JSON/YAML/Message serialization |
| `arduino/Msg.java` | generated | **Do not edit** — see [Generated files](#generated-files) |

See [`doc/agent/hotspot-map.md`](doc/agent/hotspot-map.md) for region navigation inside Runtime/Service.

## Architecture (mental model)

```
Runtime.main / getInstance
    → create/start services from CLI (-s) or YAML config (-c)
    → each Service has Inbox/Outbox + MethodCache invoke
    → MetaData (*Meta) describes deps/peers; Ivy installs into libraries/
    → WebGui / Python / remote gateways speak Message JSON over WS/HTTP
```

Lifecycle details: [`doc/service-life-cycle.md`](doc/service-life-cycle.md).

### Service triple (always keep in sync)

```
org.myrobotlab.service.Foo
org.myrobotlab.service.config.FooConfig
org.myrobotlab.service.meta.FooMeta
src/main/resources/resource/Foo/   # scripts, yml samples, assets
```

## Dual dependency system (critical)

There are **two** dependency truths:

1. **`pom.xml`** — compile / shade classpath. Most deps are `provided`.
2. **`*Meta.addDependency(...)`** — runtime install via Ivy into `libraries/`.

Fixing only `pom.xml` often leaves runtime broken (or the reverse).

**Cookbook:** [`doc/agent/dependency-updates.md`](doc/agent/dependency-updates.md).

## Generated files

Do **not** hand-edit:

| Generated | Edit instead | Generator |
|-----------|--------------|-----------|
| `src/main/java/org/myrobotlab/arduino/Msg.java` | `src/main/resources/resource/Arduino/generate/arduinoMsgs.schema` | `ArduinoMsgGenerator` |
| `src/main/java/org/myrobotlab/arduino/VirtualMsg.java` | same schema + templates | `ArduinoMsgGenerator` |
| Related Arduino C++ under `resource/Arduino/` from generator | schema / templates | `ArduinoMsgGenerator` |

See [`doc/GENERATED.md`](doc/GENERATED.md).

## Sibling repositories

Not always present in this clone (often gitignored / separate):

| Repo | Expected path for local WebGui/dev |
|------|-------------------------------------|
| InMoov2 | `src/main/resources/resource/InMoov2` |
| ProgramAB | `src/main/resources/resource/ProgramAB` |

Use `make_web_dev.bat` (Windows) to clone siblings when needed. Robot/chatbot bugs may live **outside** this repo.

## Build / test / smoke

```bash
# Full build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Single test
mvn test -Dtest=org.myrobotlab.framework.MethodCacheTest

# Fast agent / PR suite (curated framework/codec tests; skips InMoov/install-heavy)
mvn test -Pagent-tests

# Run from Maven
mvn exec:java -Dexec.mainClass=org.myrobotlab.service.Runtime -Dexec.args="-s webgui WebGui intro Intro python Python"

# Packaged
./myrobotlab.sh   # or myrobotlab.bat
```

**Dev smoke (healthy):** Runtime starts, WebGui listens on `http://localhost:8888`, no continuous install storm if `libraries/` already populated.

VS Code launch: `.vscode/launch.json` → **Runtime** (`-s webgui WebGui intro Intro python Python -c dev`).

Scripts: [`scripts/agent-smoke.ps1`](scripts/agent-smoke.ps1), [`scripts/agent-smoke.sh`](scripts/agent-smoke.sh).

### Test conventions

- Extend `org.myrobotlab.test.AbstractTest` for service/framework tests (virtual Runtime, resource path).
- Prefer unit tests that do **not** require internet, cameras, or `installAll()`.
- Many hardware/chaos tests are `@Ignore` — do not treat ignored tests as coverage.
- Surefire excludes `**/integration/*` by default.

## Typed API preference (new code)

Prefer:

- Typed `*Config` fields and `apply()` / getters-setters
- Interfaces under `org.myrobotlab.service.interfaces`
- `Runtime.getService(name, new StaticType<MyService>() {})` when type matters

Avoid for **new** public surfaces:

- Stringly `invoke("methodName", ...)` as the only API
- Untyped `Object` bags where a Config or DTO fits

Reflection invoke remains core for the message bus; keep typed seams at service boundaries so agents and IDEs can navigate.

## Domain map

Service categories / where to look: [`doc/agent/service-domain-map.md`](doc/agent/service-domain-map.md).

## WebGui notes

- Stack: AngularJS 1.x under `resource/WebGui/app/`
- Per-service GUI: `*Gui.js` + views
- Message bus client: `mrl.js`
- React tree under `resource/WebGui/react` is experimental/ignored — do not assume it is the primary UI

## Maven clean side effects

`mvn clean` deletes `libraries/`, `data/`, and copied `resource/` trees (see `maven-clean-plugin` in `pom.xml`). After clean, the next run may re-download large native deps.

## PR checklist for agents

1. Touch the smallest surface that fixes the bug (service triple before Runtime/Service).
2. If changing deps → follow dependency cookbook (Meta **and** pom sync).
3. If changing Arduino protocol → edit schema, regenerate, never hand-patch `Msg.java`.
4. Add or update a focused unit test when practical.
5. Run `mvn test -Pagent-tests` (and the specific test for the area you changed).
6. Do not commit secrets, local `libraries/`, or `data/` runtime state.
