package com.thalmic.myo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.thalmic.myo.enums.LockingPolicy;

public final class Hub {
	private static final File TEMP_DIRECTORY_LOCATION = new File(System.getProperty("java.io.tmpdir"));
	private long nativeHandle;
	private final String applicationIdentifier;

	public Hub() {
		this("");
	}

	public Hub(String applicationIdentifier) {
		this.applicationIdentifier = applicationIdentifier;
		loadJniResources();
		initialize(applicationIdentifier);
	}

	private native void initialize(String applicationIdentifier);

	public native Myo waitForMyo(int timeout);

	public native void addListener(DeviceListener listener);

	public native void removeListener(DeviceListener listener);

	public native void run(int duration);

	public native void runOnce(int duration);

	public void setLockingPolicy(LockingPolicy lockingPolicy) {
		setLockingPolicy(lockingPolicy.ordinal());
	}

	private native void setLockingPolicy(int lockingPolicy);

	private final void loadJniResources() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("mac")) {
			boolean wasLoadSuccessful = loadOSXResourceFromSysPath();
			if (!wasLoadSuccessful) {
				wasLoadSuccessful = copyAndLoadOSXFromTemp();
				if (!wasLoadSuccessful) {
					throw new UnsatisfiedLinkError("Could Not Load myo and myo-java libs");
				}
			}

		} else if (osName.contains("win")) {
			boolean wasLoadSuccessful = loadX64ResourcesFromSysPath();
			if (!wasLoadSuccessful) {
				wasLoadSuccessful = loadWin32ResourcesFromSysPath();
				if (!wasLoadSuccessful) {
					setLibDirectory();

					wasLoadSuccessful = copyAndLoadX64FromTemp();
					if (!wasLoadSuccessful) {
						wasLoadSuccessful = copyAndLoadWin32FromTemp();
						if (!wasLoadSuccessful) {
							throw new UnsatisfiedLinkError("Could Not Load myo and myo-java libs");
						}
					}
				}
			}
		} else {
			System.err.println("Your Operating System is not supported at this time.");
		}
	}

	private void setLibDirectory() {
		try {
			System.setProperty("java.library.path", TEMP_DIRECTORY_LOCATION.getAbsolutePath());
			Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void unzipFile(InputStream inputStream, File destDirectory) throws IOException {
		if (!destDirectory.exists()) {
			destDirectory.mkdir();
		}
		try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
			ZipEntry zipEntry = null;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				File newFileFromZip = new File(destDirectory, zipEntry.getName());
				newFileFromZip.deleteOnExit();
				if (zipEntry.isDirectory()) {
					newFileFromZip.mkdir();
				} else {
					try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(newFileFromZip))) {
						byte[] bytesIn = new byte[4096];
						int read = 0;
						while ((read = zipInputStream.read(bytesIn)) != -1) {
							outputStream.write(bytesIn, 0, read);
						}
					}
				}
				zipInputStream.closeEntry();
			}
		}
	}

	private boolean copyAndLoadOSXFromTemp() {
		try {

			File macMyoTempFile = new File(TEMP_DIRECTORY_LOCATION, "libmyo.jnilib");
			try (InputStream macMyoInputStream = this.getClass().getResourceAsStream("/osx/libmyo.jnilib")) {
				Files.copy(macMyoInputStream, macMyoTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			macMyoTempFile.deleteOnExit();

			try (InputStream macMyoLibInputStream = this.getClass().getResourceAsStream("/osx/myo.zip")) {
				unzipFile(macMyoLibInputStream, TEMP_DIRECTORY_LOCATION);
			}

			setLibDirectory();
			System.loadLibrary("myo");
			return true;
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
			String errorMessage = String.format("Unable to load %s from directory %s", "libmyo.jnilib", TEMP_DIRECTORY_LOCATION);
			System.err.println(errorMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean loadOSXResourceFromSysPath() throws UnsatisfiedLinkError {
		try {
			System.loadLibrary("myo");
			return true;
		} catch (UnsatisfiedLinkError e) {
			String errorMessage = String.format("Unable to load myo from system directories.");
			System.err.println(errorMessage);
		}
		return false;
	}

	private boolean loadX64ResourcesFromSysPath() throws UnsatisfiedLinkError {
		try {
			System.loadLibrary("myo64");
			System.loadLibrary("JNIJavaMyoLib");
			return true;
		} catch (UnsatisfiedLinkError e) {
			String errorMessage = String.format("Unable to load myo64 from system directories.");
			System.err.println(errorMessage);
		}
		return false;
	}

	private boolean loadWin32ResourcesFromSysPath() throws UnsatisfiedLinkError {
		try {
			System.loadLibrary("myo32");
			System.loadLibrary("JNIJavaMyoLib");
			return true;
		} catch (UnsatisfiedLinkError e) {
			String errorMessage = String.format("Unable to load myo32 from system directories.");
			System.err.println(errorMessage);
		}
		return false;
	}

	private boolean copyAndLoadX64FromTemp() {
		try {
			File myo64DllTempFile = new File(TEMP_DIRECTORY_LOCATION, "myo64.dll");
			myo64DllTempFile.deleteOnExit();
			try (InputStream myo64DllInputStream = this.getClass().getResourceAsStream("/x64/myo64.dll")) {
				Files.copy(myo64DllInputStream, myo64DllTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}

			File jniJavaMyoLibDllTempFile = new File(TEMP_DIRECTORY_LOCATION, "JNIJavaMyoLib64.dll");
			jniJavaMyoLibDllTempFile.deleteOnExit();
			try (InputStream jniJavaMyoLibDllInputStream = this.getClass().getResourceAsStream("/x64/JNIJavaMyoLib.dll")) {
				Files.copy(jniJavaMyoLibDllInputStream, jniJavaMyoLibDllTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}

			System.loadLibrary("myo64");
			System.loadLibrary("JNIJavaMyoLib64");
			return true;
		} catch (UnsatisfiedLinkError e) {
			String errorMessage = String.format("Unable to load %s from directory %s.", "myo64.dll", TEMP_DIRECTORY_LOCATION);
			System.err.println(errorMessage);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	private boolean copyAndLoadWin32FromTemp() {
		try {
			File myo32DllTempFile = new File(TEMP_DIRECTORY_LOCATION, "myo32.dll");
			myo32DllTempFile.deleteOnExit();
			try (InputStream myo32DllInputStream = this.getClass().getResourceAsStream("/Win32/myo32.dll")) {
				Files.copy(myo32DllInputStream, myo32DllTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}

			File jniJavaMyoLibDllTempFile = new File(TEMP_DIRECTORY_LOCATION, "JNIJavaMyoLib32.dll");
			jniJavaMyoLibDllTempFile.deleteOnExit();

			try (InputStream jniJavaMyoLibDllInputStream = this.getClass().getResourceAsStream("/Win32/JNIJavaMyoLib.dll")) {
				Files.copy(jniJavaMyoLibDllInputStream, jniJavaMyoLibDllTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}

			System.loadLibrary("myo32");
			System.loadLibrary("JNIJavaMyoLib32");
			return true;
		} catch (UnsatisfiedLinkError e) {
			String errorMessage = String.format("Unable to load %s from directory %s.", "myo32.dll", TEMP_DIRECTORY_LOCATION);
			System.err.println(errorMessage);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

}