/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.cf.loggregator;

import java.util.concurrent.*;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loggregator Client Socket
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class LoggregatorSocket {

	private final Logger logger = LoggerFactory.getLogger("org.eclipse.orion.server.cf"); //$NON-NLS-1$
	private final CountDownLatch closeLatch;
	private LoggregatorListener listener;

	public LoggregatorSocket(LoggregatorListener listener) {
		this.closeLatch = new CountDownLatch(1);
		this.listener = listener;
	}

	public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
		return this.closeLatch.await(duration, unit);
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		logger.debug(NLS.bind("Connection closed: {0} - {1}", statusCode, reason));
		this.closeLatch.countDown();
	}

	@OnWebSocketConnect
	public void onConnect(final Session session) {
		logger.debug(NLS.bind("Conected: {0}", session));
		try {
			Future<Void> fut;
			fut = session.getRemote().sendStringByFuture("keep alive");
			fut.get(25, TimeUnit.SECONDS);
			fut = session.getRemote().sendStringByFuture("keep alive");
			fut.get(25, TimeUnit.SECONDS);
		} catch (Throwable t) {
			logger.error("Error while connecting", t);
		}
	}

	@OnWebSocketMessage
	public void onMessage(String msg) {
		logger.debug(NLS.bind("Got message: {0}", msg));
		listener.add(msg);
	}
}