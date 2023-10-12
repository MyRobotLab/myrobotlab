package org.myrobotlab.ext.python;

import org.bytedeco.javacpp.Loader;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.process.SubprocessException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.myrobotlab.framework.Status.error;
import static org.myrobotlab.io.FileIO.fs;

public class PythonUtils {
    private static Logger logger = LoggerFactory.getLogger(PythonUtils.class);

    public static final String SYSTEM_PYTHON_COMMAND = "python3";


    /**
     * Ensures a virtual environment is setup at the
     * desired location, creating it if it doesn't exist,
     * and returning the absolute file path to the
     * virtual environment Python executable.
     *
     * @param venv             The location to setup the virtual environment
     * @param useBundledPython Whether to use the bundled Python
     *                         or the system python for setting up
     *                         the virtual environment.
     * @return The location of the virtual environment interpreter
     */
    public static String setupVenv(String venv, boolean useBundledPython, List<String> packages) throws IOException, InterruptedException {
        String pythonCommand = (Platform.getLocalInstance().isWindows()) ? venv + fs + "Scripts" + fs + "python.exe" : venv + fs + "bin" + fs + "python";
        if (!FileIO.checkDir(venv)) {
            // We don't have an initialized virtual environment, so lets make one
            // and install our required packages
            String hostPython = (useBundledPython) ? Loader.load(org.bytedeco.cpython.python.class) : SYSTEM_PYTHON_COMMAND;
            ProcessBuilder installProcess;
            int ret;
            String venvLib = new File(hostPython).getParent() + fs + "lib" + fs + "venv" + fs + "scripts" + fs + "nt";
            if (Platform.getLocalInstance().isWindows()) {
                // Super hacky workaround, venv works differently on Windows and requires these two
                // files, but they are not distributed in bare-bones Python or in any pip packages.
                // So we copy them where it expects, and it seems to work now
                String containingDir = new File(hostPython).getParent();
                FileIO.copy(containingDir + fs + "python.exe", venvLib + fs + "python.exe");
                FileIO.copy(containingDir + fs + "pythonw.exe", venvLib + fs + "pythonw.exe");
            }

            installProcess = new ProcessBuilder(hostPython, "-m", "venv", venv);
            ret = installProcess.inheritIO().start().waitFor();
            if (ret != 0) {
                String message = String.format("Could not create virtual environment, subprocess returned %s. If on Windows, make sure there is a python.exe file in %s", ret, venvLib);
                error(message);
                throw new SubprocessException(message);
            }


            List<String> command = new ArrayList<>(List.of(pythonCommand, "-m", "pip", "install"));
            command.addAll(packages);
            installProcess = new ProcessBuilder(command.toArray(new String[0]));
            ret = installProcess.inheritIO().start().waitFor();
            if (ret != 0) {
                String message = String.format("Could not install desired packages (%s)", packages);
                error(message);
                throw new SubprocessException(message);
            }

        }
        return new File(pythonCommand).getAbsolutePath();
    }

    /**
     * Install a list of packages into the environment given by python.
     * A new subprocess is spawned to perform the installation, output
     * is echoed to this process's stdout/stderr.
     * <p></p>
     * TODO add process gobbler to echo on logging system
     *
     * @param packages The list of packages to install. Must be findable by Pip
     * @throws SubprocessException If an I/O error occurs running Pip.
     */
    public static int installPipPackages(String python, List<String> packages) {
        ProcessBuilder builder = new ProcessBuilder(python, "-m", "pip", "install");
        List<String> currCommand = builder.command();
        currCommand.addAll(packages);
        try {
            return builder.inheritIO().start().waitFor();
        } catch (InterruptedException | IOException e) {
            throw new SubprocessException("Unable to install packages " + packages + " with Python command " + python, e);
        }

    }
    public static int runPythonScript(String python, File workingDirectory, String script, String... args) {
        try {
            return runPythonScriptAsync(python, workingDirectory, script, args).waitFor();
        } catch (InterruptedException e) {
                throw new SubprocessException("Unable to run script " + script + " with Python command " + python, e);
            }
    }


    public static Process runPythonScriptAsync(String python, File workingDirectory, String script, String... args) {
        ProcessBuilder builder = new ProcessBuilder(python, script);
        List<String> currCommand = builder.command();
        currCommand.addAll(List.of(args));
        try {
            return builder.inheritIO().directory(workingDirectory).start();
        } catch (IOException e) {
            throw new SubprocessException("Unable to run script " + script + " with Python command " + python, e);
        }

    }
}
