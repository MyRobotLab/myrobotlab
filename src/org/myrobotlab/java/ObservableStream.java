package org.myrobotlab.java;
/*
 * Copyright (c) 1998, Subrahmanyam Allamaraju. All Rights Reserved.
 * 
 * Permission to use, copy, modify, and distribute this software for
 * NON-COMMERCIAL purposes and without fee is hereby granted provided that this
 * copyright notice appears in all copies.
 *
 * This software is intended for demonstration purposes only, and comes without
 * any explicit or implicit warranty.
 *
 * Send all queries about this software to sallamar@cvimail.cv.com
 *
 */

import java.io.ByteArrayOutputStream;
import java.util.Vector;

public class ObservableStream extends ByteArrayOutputStream 
{
    Vector streamObservers = new Vector();
    
    void addStreamObserver(StreamObserver o) 
	{
	    streamObservers.addElement(o);
	}
    
    void removeStreamObserver(StreamObserver o) 
	{
	    streamObservers.removeElement(o);
	}
        
    void notifyObservers() 
	{
	    for(int i = 0; i < streamObservers.size(); i++)
		((StreamObserver) streamObservers.elementAt(i)).streamChanged();
	}
    
    public void write(byte[] b, int off, int len) 
	{
	    super.write(b, off, len);
	    notifyObservers();
	}

}
