# Adding a new service

Step-by-step checklist for creating a MyRobotLab service. Prefer this over editing `Runtime.java` / `Service.java`.

**Templates to copy:** `_TemplateService`, `_TemplateServiceConfig`, `_TemplateServiceMeta`, plus WebGui `_TemplateGui.js` / `_TemplateGui.html`.

**Recent full example:** `VoskSpeechRecognition` (service + config + meta + resources + WebGui + test + logo).

**Related:** [service life cycle](../service-life-cycle.md) · [dependency updates](dependency-updates.md) · [domain map](service-domain-map.md)

---

## 1. Name and place the service triple

Pick a PascalCase type name, e.g. `Foo`. Keep these **exactly** in sync (discovery is by naming convention — no central registry edit):

| Piece | Path |
|-------|------|
| Service | `src/main/java/org/myrobotlab/service/Foo.java` |
| Config | `src/main/java/org/myrobotlab/service/config/FooConfig.java` |
| Meta | `src/main/java/org/myrobotlab/service/meta/FooMeta.java` |
| Resources | `src/main/resources/resource/Foo/` |
| Logo | `src/main/resources/resource/Foo.png` |

Copy from `_TemplateService*` and rename types/packages/strings.

If the service belongs to a domain with a shared abstract (speech in/out, etc.), **extend that abstract** instead of bare `Service` — e.g. `AbstractSpeechRecognizer`, `AbstractSpeechSynthesis`. See [service-domain-map.md](service-domain-map.md).

---

## 2. Implement the service class

```java
public class Foo extends Service<FooConfig> {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Foo.class);

  public Foo(String n, String id) {
    super(n, id);
  }
}
```

Guidelines:

- Prefer **typed public methods** and config fields over string-only `invoke("method")` as the primary API.
- Publish events with `invoke("publishX", …)` / `publishX(...)` so WebGui and Python can subscribe.
- Call `broadcastState()` when UI-visible fields change.
- Override `apply(FooConfig c)` when start/stop or peers must react to loaded config (see `Clock.apply`).
- Optional `main()` for local smoke (template pattern).
- Implement relevant interfaces under `org.myrobotlab.service.interfaces` when attaching to mouths, ears, text listeners, etc.

Lifecycle reminder ([service-life-cycle.md](../service-life-cycle.md)): create → `setConfig` → **`apply`** → `startService` → … → `release`. Put config-driven behavior in `apply()`, not only ad-hoc setters.

---

## 3. Define `FooConfig`

```java
package org.myrobotlab.service.config;

public class FooConfig extends ServiceConfig {
  public int interval = 1000;
  // public fields only for values that should persist in YAML
}
```

- `type` is set automatically from the class name (`Foo` from `FooConfig`).
- Override `getDefault(Plan plan, String name)` when the service has **peers**:

```java
@Override
public Plan getDefault(Plan plan, String name) {
  super.getDefault(plan, name);
  addDefaultPeerConfig(plan, name, "serial", "Serial");
  return plan;
}
```

Peers belong in **Config** (for `-c` plans), not only legacy Meta peer declarations.

---

## 4. Define `FooMeta`

```java
public class FooMeta extends MetaData {
  public FooMeta() {
    addDescription("one-line description shown in UI / install lists");
    addCategory("sensors");           // see service-domain-map.md
    setAvailable(true);               // false hides from UI (template uses false for itself)
    // setSponsor("YourName");
    // setCloudService(true);
    // setRequiresKeys(true);
    // setLicenseApache();

    // Runtime jars (Ivy → libraries/)
    // addDependency("com.example", "example-lib", "1.2.3");
  }
}
```

**Important:** `_TemplateServiceMeta` ends with `setAvailable(false)` so the template stays hidden. Your service must call `setAvailable(true)`.

If you add dependencies:

1. Declare them in `*Meta.addDependency(...)`.
2. Mirror the same GAV in root `pom.xml` with `<scope>provided</scope>` (alphabetically near related services).
3. Follow [dependency-updates.md](dependency-updates.md).

---

## 5. Add resource folder samples

Create `src/main/resources/resource/Foo/`:

| File | Purpose |
|------|---------|
| `Foo.py` | Tutorial / example script (primary in-repo how-to) |
| `foo.yml` | Sample YAML (`!!org.myrobotlab.service.config.FooConfig`, `type: Foo`) |
| Extra assets | models, grammars, images as needed |

Python header convention:

```python
#########################################
# Foo.py
# description: short description
# categories: sensors
# more info @: http://myrobotlab.org/service/Foo
#########################################

foo = runtime.start("foo", "Foo")
```

---

## 6. Service logo (recommended)

Add `src/main/resources/resource/Foo.png`:

- Typically **48×48** PNG with transparency.
- Prefer an official upstream logo when the service wraps an external project.
- WebGui loads it as `/Foo.png` (tabs, Runtime service list).

`Service.getServiceIcon("Foo")` reads `resource/Foo.png`.

---

## 7. WebGui (recommended for user-facing services)

Lazy-loaded by type name in `mrl.js` — **filename must match**:

| File | Path |
|------|------|
| Controller | `src/main/resources/resource/WebGui/app/service/js/FooGui.js` |
| View | `src/main/resources/resource/WebGui/app/service/views/FooGui.html` |

Conventions (see `_TemplateGui.js` / `ClockGui.js`):

- Angular module: `mrlapp.service.FooGui`
- Controller: `FooGuiCtrl`
- Implement `updateState(service)` and `onMsg` handling at least `onState`
- Subscribe: `msg.subscribe('publishSomething')`, `msg.subscribe(this)`
- Call service methods: `msg.send('methodName', arg)` or `$scope.msg.methodName(...)`

If either file is missing, the panel falls back toward **NoGui**. No separate registration step.

Copy starting points from:

- `resource/WebGui/app/service/js/_TemplateGui.js`
- `resource/WebGui/app/service/views/_TemplateGui.html`

---

## 8. Tests

Add `src/test/java/org/myrobotlab/service/FooTest.java` extending `AbstractTest`:

- Prefer unit tests that do **not** need internet, cameras, or `installAll()`.
- Cover start/stop, config fields, and pure helpers; mock or skip hardware/native paths.
- If deps changed, also run `DependencyTest` / agent-tests.

```bash
mvn test -Dtest=org.myrobotlab.service.FooTest
mvn test -Pagent-tests
```

---

## 9. Documentation touch-ups

| Layer | Action |
|-------|--------|
| JavaDoc | Class-level purpose; document public methods and publish topics |
| `Foo.py` | In-repo tutorial + `http://myrobotlab.org/service/Foo` link |
| `foo.yml` | Shows valid config for `-c` / plans |
| Domain map | Add the service under the right row in [service-domain-map.md](service-domain-map.md) when it is a new or notable entry |
| External wiki | Optional page at `myrobotlab.org/service/Foo` (not generated from this repo) |

---

## 10. Verify locally

```bash
# Compile / focused test
mvn test -Dtest=org.myrobotlab.service.FooTest

# Agent PR suite
mvn test -Pagent-tests

# Runtime smoke (example)
mvn exec:java -Dexec.mainClass=org.myrobotlab.service.Runtime \
  -Dexec.args="-s webgui WebGui foo Foo python Python"
```

Then open `http://localhost:8888`, confirm `Foo` appears in the service list with its icon, and that the WebGui panel loads.

After `mvn clean`, runtime may need to re-download Meta/Ivy jars into `libraries/`.

---

## Checklist (copy for PRs)

- [ ] `Foo.java` + `FooConfig.java` + `FooMeta.java` (`setAvailable(true)`, description, category)
- [ ] Typed API + `apply()` if behavior depends on config
- [ ] Interfaces / domain abstract used when appropriate
- [ ] `resource/Foo/Foo.py` + `foo.yml`
- [ ] `resource/Foo.png` (48×48 service logo)
- [ ] `FooGui.js` + `FooGui.html` (or accept NoGui)
- [ ] Deps in Meta **and** `pom.xml` if any ([dependency-updates.md](dependency-updates.md))
- [ ] Focused `FooTest`
- [ ] Domain map / wiki note if public-facing
- [ ] `mvn test -Dtest=...FooTest` and `mvn test -Pagent-tests`

---

## Anti-patterns

- Editing only `pom.xml` (or only Meta) for a runtime library
- Hand-editing generated Arduino `Msg.java` / `VirtualMsg.java`
- Leaving `setAvailable(false)` from the template
- Peer definitions only in Meta when Config/`Plan` should own them
- Stringly `invoke` as the only public surface for new APIs
- Committing `libraries/`, `data/`, or secrets
