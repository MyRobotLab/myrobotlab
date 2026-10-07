# Dependency update cookbook

MyRobotLab uses **two** dependency systems. Updating only one of them is a common source of “works in IDE, fails at runtime” bugs.

## Systems

| System | Where | Used for |
|--------|-------|----------|
| Maven | Root `pom.xml` | Compile, test, shade `myrobotlab.jar` |
| Meta + Ivy | `*Meta.addDependency(...)` → `IvyWrapper` | Runtime install into `libraries/jar` and natives |

Most Maven dependencies are scoped `provided`. Runtime resolution is driven by service Meta data.

## When to change what

| Goal | Edit |
|------|------|
| New/updated library for one service | That service’s `*Meta.java` **and** matching `pom.xml` entry |
| Build-only / test-only tool | `pom.xml` only (test scope) |
| Native / classifier zip (e.g. JavaCV) | Meta (classifiers, excludes) + pom |

## Step-by-step (service library bump)

1. **Find the Meta**  
   `src/main/java/org/myrobotlab/service/meta/YourServiceMeta.java`

2. **Change the version** in `addDependency(group, artifact, version)` (and classifiers/excludes if needed).

3. **Sync `pom.xml`**  
   Search for the same `groupId`/`artifactId` and update the version.  
   Optional: regenerate a pom fragment via `org.myrobotlab.framework.repo.MavenWrapper` + `resource/framework/pom.xml.template` — treat the checked-in root `pom.xml` as what CI builds.

4. **Clear stale runtime state** (if an old jar is cached):
   ```bash
   # Windows PowerShell
   Remove-Item -Recurse -Force libraries -ErrorAction SilentlyContinue
   Remove-Item -Force libraries/serviceData.json -ErrorAction SilentlyContinue

   # Unix
   rm -rf libraries
   ```
   Note: `mvn clean` also wipes `libraries/` and `data/`.

5. **Verify**:
   ```bash
   mvn test -Dtest=org.myrobotlab.framework.DependencyTest
   mvn test -Pagent-tests
   # Plus a service-specific test if one exists
   ```

6. **Runtime install check** (optional): start Runtime and install/start the service so Ivy pulls the new artifact into `libraries/`.

## Checklist

- [ ] `*Meta` version updated
- [ ] `pom.xml` version updated (same GAV)
- [ ] Classifiers / exclusions mirrored if Meta has them
- [ ] Stale `libraries/` cleared when needed
- [ ] `DependencyTest` / `agent-tests` pass
- [ ] No accidental commit of downloaded `libraries/` jars

## Anti-patterns

- Editing only `pom.xml` for a service that installs via Meta/Ivy
- Hand-copying jars into `libraries/` without Meta (breaks clean installs)
- Bumping majors without checking native classifiers (OpenCV/JavaCV especially)

## Example Meta snippet

```java
// In YourServiceMeta constructor:
addDependency("com.example", "example-lib", "1.2.3");
```

Mirror in `pom.xml`:

```xml
<dependency>
  <groupId>com.example</groupId>
  <artifactId>example-lib</artifactId>
  <version>1.2.3</version>
  <scope>provided</scope>
</dependency>
```
