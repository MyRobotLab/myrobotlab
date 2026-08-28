# Dependency update checklist (quick)

Full guide: [`doc/agent/dependency-updates.md`](../doc/agent/dependency-updates.md)

1. Edit `src/main/java/org/myrobotlab/service/meta/<Service>Meta.java` → `addDependency(...)`
2. Mirror GAV in root `pom.xml`
3. Clear `libraries/` if an old jar may be cached
4. `mvn test -Dtest=org.myrobotlab.framework.DependencyTest`
5. `mvn test -Pagent-tests`
6. Smoke-start the affected service if natives/classifiers changed
