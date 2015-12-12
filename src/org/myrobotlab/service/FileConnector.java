package org.myrobotlab.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.document.connector.ConnectorState;
import org.myrobotlab.service.interfaces.DocumentPublisher;

public class FileConnector extends AbstractConnector implements DocumentPublisher,FileVisitor<Path> {

	private static final long serialVersionUID = 1L;
	private String directory;
	// TODO: add wildcard includes/excludes
	// TODO: add file path includes/excludes
	private boolean interrupted = false;

	public FileConnector(String name) {
		super(name);
		// no overruns!
		this.getOutbox().setBlocking(true);
	}

	@Override
	public void startCrawling() {
		state = ConnectorState.RUNNING;
		Path startPath = Paths.get(directory);
		try {
			Files.walkFileTree(startPath, this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		state = ConnectorState.STOPPED;
	}

	@Override
	public void stopCrawling() {
		interrupted = true;
		state = ConnectorState.INTERRUPTED;
	}

	@Override
	public String getDescription() {
		return "This connector will scan all the files in a directory and production documents.";
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		System.out.println(file);
		if (interrupted) 
			return FileVisitResult.TERMINATE;
		String docId = getDocIdPrefix() + file.toFile().getAbsolutePath();
		Document doc = new Document(docId);		
		doc.setField("last_modified", attrs.lastModifiedTime());
		doc.setField("created_date", attrs.creationTime());
		doc.setField("filename",  file.toFile().getAbsolutePath());	
		doc.setField("size", attrs.size());
		// TODO: potentially add a byte array of the file
		// or maybe an input stream or other handle to the file.
		feed(doc);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		throw exc;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		if (exc != null) {
			throw exc;
		}
		return FileVisitResult.CONTINUE;

	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}


}
