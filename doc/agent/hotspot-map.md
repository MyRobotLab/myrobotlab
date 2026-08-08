# Hotspot map — Runtime & Service

`Runtime.java` and `Service.java` are large by design. Prefer fixing bugs in a specific service. When you must edit these files, jump by **region** (search for `AGENT REGION` comments in source).

Line numbers drift; use the region banners and method names below as the source of truth.

## Runtime (`org.myrobotlab.service.Runtime`)

| AGENT REGION | Typical concerns | Entry methods (search) |
|--------------|------------------|------------------------|
| REGISTRY | Global service map, lookup, export | `getRegistry`, `getService`, `getLocalServices` |
| CREATE_START | Create/start from name+type or lists | `createAndStart`, `createAndStartServices`, `start` |
| SINGLETON | Process singleton, options bootstrap | `getInstance` |
| INSTALL | Ivy/repo install threads | `install` |
| LIFECYCLE_RELEASE | Release one/all, shutdown | `releaseService`, `releaseAll`, `shutdown` |
| NETWORK | Connect, route, remote services | `connect`, `RouteTable`, `connections` |
| CONFIG_PLAN | Load plan, YAML config paths | `load`, `readServiceConfig`, `releaseConfigPath` |
| MAIN_CLI | Process entry, picocli options | `main` |
| PLATFORM_INFO | Memory, version, platform bits | `getVersion`, `getPlatform`, `getUptime` |

### Facade guidance (incremental)

Do **not** big-bang rewrite Runtime. When adding new behavior:

1. Put new cohesive logic in a focused class under `org.myrobotlab.framework` (or a subpackage) when it does not need `Runtime` private state.
2. Keep `Runtime` as a thin delegator for new APIs.
3. Prefer extending existing helpers (`Repo`, `RouteTable`, `Plan`, `CodecUtils`) over growing `Runtime` further.

Existing extraction-friendly collaborators already outside Runtime:

- `org.myrobotlab.framework.repo.Repo` / `IvyWrapper` / `MavenWrapper` — install
- `org.myrobotlab.framework.Plan` — start plans
- `org.myrobotlab.framework.MethodCache` — invoke resolution
- `org.myrobotlab.framework.registration.*` — registration records

## Service (`org.myrobotlab.framework.Service`)

| AGENT REGION | Typical concerns | Entry methods (search) |
|--------------|------------------|------------------------|
| MESSAGING | Inbox/outbox, listeners | `addListener`, `inbox`, `outbox`, `getMsg` |
| INVOKE | Reflection dispatch, futures | `invoke`, `invokeFuture` |
| PEERS | Peer keys, peer lifecycle | `getPeers`, `startPeer`, peer helpers |
| CONFIG | Typed config apply/load/save | `apply`, `getConfig`, `setConfig` |
| LIFECYCLE | start/stop/release threads | `startService`, `stopService`, `releaseService` |
| STATUS | Status/error publishing | `error`, `publishStatus`, `broadcastState` |
| RESOURCES | Service resource files | resource helpers near `resources begin` |

### Typed seams

When adding service APIs used by WebGui or Python:

- Add a real Java method + document it (becomes invokable via MethodCache).
- Prefer `*Config` fields over parallel ad-hoc instance fields when persisted.
- Use interfaces in `org.myrobotlab.service.interfaces` for attach/detach patterns.

## Other hotspots

| File | Notes |
|------|-------|
| `codec/CodecUtils.java` | Message/JSON/YAML; changes affect all transports |
| `service/OpenCV.java` + `opencv/*` | Native/heavy; tests often need libs |
| `arduino/Msg.java` | Generated — see `doc/GENERATED.md` |
| `service/Mpu6050.java` | Very large hardware service — localize edits |

## Suggested verify after hotspot edits

```bash
mvn test -Pagent-tests
mvn test -Dtest=org.myrobotlab.framework.MethodCacheTest,org.myrobotlab.framework.ServiceLifeCycleTest,org.myrobotlab.codec.CodecUtilsTest
```
