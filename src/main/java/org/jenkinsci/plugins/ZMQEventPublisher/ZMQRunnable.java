/*
 * Copyright 2013 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.jenkinsci.plugins.ZMQEventPublisher;

import jenkins.model.Jenkins;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jeromq.ZMQ;
import org.jeromq.ZMQException;

public class ZMQRunnable implements Runnable {
    public static final Logger LOGGER = Logger.getLogger(ZMQRunnable.class.getName());

    private static final String bind_addr = "tcp://*:%d";
    private int port;

    private final LinkedBlockingQueue<String> queue;
    private final ZMQ.Context context;
    private ZMQ.Socket publisher;

    public ZMQRunnable(LinkedBlockingQueue<String> queue) {
        this.queue = queue;
        context = ZMQ.context(1);
        bindSocket();
    }

    private int getPort() {
        Jenkins jenkins = Jenkins.getInstance();
        HudsonNotificationProperty.HudsonNotificationPropertyDescriptor globalProperty =
            (HudsonNotificationProperty.HudsonNotificationPropertyDescriptor)
                jenkins.getDescriptor(HudsonNotificationProperty.class);
        if (globalProperty != null) {
            return globalProperty.getPort();
        }
        return 8888;
    }

    private void bindSocket() {
        int tmpPort = getPort();
        if (publisher == null) {
            port = tmpPort;
            LOGGER.log(Level.INFO,
                String.format("Binding ZMQ PUB to port %d", port));
            publisher = bindSocket(port);
        }
        else if (tmpPort != port) {
            LOGGER.log(Level.INFO,
                String.format("Changing ZMQ PUB port from %d to %d", port, tmpPort));
            try {
                publisher.close();
            } catch (ZMQException e) {
                /* Let the garbage collector sort out cleanup */
                LOGGER.log(Level.INFO,
                    "Unable to close ZMQ PUB socket. " + e.toString(), e);
            }
            port = tmpPort;
            publisher = bindSocket(port);
        }
    }

    private ZMQ.Socket bindSocket(int port) {
        ZMQ.Socket socket;
        try {
            socket = context.socket(ZMQ.PUB);
            socket.bind(String.format(bind_addr, port));
        } catch (ZMQException e) {
            LOGGER.log(Level.SEVERE,
                "Unable to bind ZMQ PUB socket. " + e.toString(), e);
            socket = null;
        }
        return socket;
    }

    public void run() {
        String event;
        while(true) {
            try {
                event = queue.take();
                bindSocket();
                if (publisher != null) {
                    try {
                        LOGGER.log(Level.FINE, "Publishing ZMQ event: " + event);
                        publisher.send(event.getBytes(), 0);
                    } catch (ZMQException e) {
                        LOGGER.log(Level.INFO,
                            "Unable to send event. " + e.toString(), e);
                    }
                } else {
                    LOGGER.log(Level.WARNING, "ZMQ Publisher is NULL");
                }
            }
            // Catch all exceptions so that this thread does not die.
            catch (Exception e) {
                LOGGER.log(Level.SEVERE,
                    "Unhandled exception publishing ZMQ events " + e.toString(), e);
            }
        }
    }
}
