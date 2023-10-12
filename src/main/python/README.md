## How Runtime starts a Python service
When the Java-side Runtime is instantiated, it extracts this python_services
directory, just like with resource.

During install() when installing all, the virtual environment
is setup and configured, installing mrlpy and the python_services modules.

A config option in Runtime's config sets whether to also start the python-side
runtime. If true, it first checks if the virtual environment is configured,
if so it starts a Python subprocess that execute's mrlpy's Runtime.


TODO replace explicit serviceRunners field with just finding services that implement
the interface when needed

This Runtime connects to the Java-side and registers itself as a "ServiceRunner,"
a new interface that denotes services able to start other services.

When creating a service (calling Java-side Runtime.createService()) with
a foreign type (like py:mrlpy.services.TestService), Runtime will look
through all registered ServiceRunners to find any that can
start services with that language key. If it finds them, it will choose the
first one matching the desired ID to call createService() on. The service runner creates the service
on its side and then returns the constructed service, which Runtime then continues
to configure or start.
