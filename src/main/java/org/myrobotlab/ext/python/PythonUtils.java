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
            String python;
            ProcessBuilder installProcess;
            int ret;
            if (useBundledPython) {
                python = Loader.load(org.bytedeco.cpython.python.class);
                String venvLib = new File(python).getParent() + fs + "lib" + fs + "venv" + fs + "scripts" + fs + "nt";
                if (Platform.getLocalInstance().isWindows()) {
                    // Super hacky workaround, venv works differently on Windows and requires these two
                    // files, but they are not distributed in bare-bones Python or in any pip packages.
                    // So we copy them where it expects, and it seems to work now
                    String containingDir = new File(python).getParent();
                    FileIO.copy(containingDir + fs + "python.exe", venvLib + fs + "python.exe");
                    FileIO.copy(containingDir + fs + "pythonw.exe", venvLib + fs + "pythonw.exe");
                }

                 installProcess = new ProcessBuilder(python, "-m", "venv", venv);
                ret = installProcess.inheritIO().start().waitFor();
                if (ret != 0) {
                    String message = String.format("Could not create virtual environment, subprocess returned %s. If on Windows, make sure there is a python.exe file in %s", ret, venvLib);
                    error(message);
                    throw new SubprocessException(message);
                }
            } else {
                python = SYSTEM_PYTHON_COMMAND;
            }

            List<String> command = new ArrayList<>(List.of(python, "-m", "pip", "install"));
            command.addAll(packages);
            installProcess = new ProcessBuilder(command.toArray(new String[0]));
            ret = installProcess.inheritIO().start().waitFor();
            if (ret != 0) {
                String message = String.format("Could not install desired packages (%s)", packages);
                error(message);
                throw new SubprocessException(message);
            }

        }
        return pythonCommand;
    }
}
