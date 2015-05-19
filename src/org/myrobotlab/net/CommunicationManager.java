/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.net;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;

import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class CommunicationManager implements Serializable, CommunicationInterface {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(CommunicationManager.class);
	Service myService = null;
	Outbox outbox = null;

	static HashMap<URI, URI> mrlToProtocol = new HashMap<URI, URI>();

	public CommunicationManager(Service myService) {
		this.myService = myService;
		this.outbox = myService.getOutbox();
		outbox.setCommunicationManager(this);
	}

	// FIXME - put in Runtime
	@Override
	public void addRemote(URI mrlHost, URI protocolKey) {
		mrlToProtocol.put(mrlHost, protocolKey);
	}

	/**
	 * mrl:/
	 */
	public Gateway getComm(URI uri) {
		if (uri.getScheme().equals(Encoder.SCHEME_MRL)) {
			Gateway gateway = (Gateway) Runtime.getService(uri.getHost());
			return gateway;
		} else {
			log.error(String.format("%s not SCHEME_MRL", uri));
			return null;
		}
	}

	@Override
	final public void send(final Message msg) {

		ServiceInterface sw = Runtime.getService(msg.getName());
		if (sw == null) {
			log.error(String.format("could not find service %s to process %s from sender %s - tearing down route", msg.name, msg.method, msg.sender));
			ServiceInterface sender = Runtime.getService(msg.sender);
			if (sender != null) {
				sender.removeListener(msg.sendingMethod, msg.getName(), msg.method);
			}
			return;
		}

		URI host = sw.getInstanceId();
		if (host == null) {
			// local message
			// log.info(String.format("local %s.%s->%s/%s.%s(%s)", msg.sender,
			// msg.sendingMethod, sw.getHost(), msg.name, msg.method,
			// Encoder.getParameterSignature(msg.data)));
			sw.in(msg);
		} else {
			// remote message
			// log.info(String.format("remote %s.%s->%s/%s.%s(%s)", msg.sender,
			// msg.sendingMethod, sw.getHost(), msg.name, msg.method,
			// Encoder.getParameterSignature(msg.data)));

			URI protocolKey = mrlToProtocol.get(host);
			getComm(host).sendRemote(protocolKey, msg);
		}
	}

	@Override
	final public void send(final URI uri, final Message msg) {
		getComm(uri).sendRemote(uri, msg);
	}

}
