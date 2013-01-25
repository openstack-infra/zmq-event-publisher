/*
 * Copyright 2013 Hewlett-Packard Development Company, L.P.
 * Copyright Authors of the Jenkins Notification Plugin
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

import hudson.Extension;
import hudson.EnvVars;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

/*
 * Listener to publish Jenkins build events through ZMQ
 */
@Extension
public class RunListenerImpl extends RunListener<Run> {
    public static final Logger LOGGER = Logger.getLogger(RunListenerImpl.class.getName());

    private int port;
    private String bind_addr;
    private ZMQ.Context context;
    private ZMQ.Socket publisher;

    public RunListenerImpl() {
        super(Run.class);
        context = ZMQ.context(1);
    }

    private int getPort(Run build) {
        HudsonNotificationProperty property = (HudsonNotificationProperty) build.getParent().getProperty(HudsonNotificationProperty.class);
        if (property != null) {
            HudsonNotificationProperty.HudsonNotificationPropertyDescriptor globalProperty = property.getDescriptor();
            return globalProperty.getPort();
        }
        return 8888;
    }

    private ZMQ.Socket bindSocket(Run build) {
        int tmpPort = getPort(build);
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
                publisher.unbind(bind_addr);
                publisher.close();
            } catch (ZMQException e) {
                /* Let the garbage collector sort out cleanup */
                LOGGER.log(Level.INFO,
                    "Unable to close ZMQ PUB socket. " + e.toString(), e);
            }
            port = tmpPort;
            publisher = bindSocket(port);
        }
        return publisher;
    }

    private ZMQ.Socket bindSocket(int port) {
        ZMQ.Socket socket;
        try {
            socket = context.socket(ZMQ.PUB);
            bind_addr = String.format("tcp://*:%d", port);
            socket.bind(bind_addr);
        } catch (ZMQException e) {
            LOGGER.log(Level.SEVERE,
                "Unable to bind ZMQ PUB socket. " + e.toString(), e);
            socket = null;
        }
        return socket;
    }

    @Override
    public void onCompleted(Run build, TaskListener listener) {
        String event = "onCompleted";
        String json = Phase.COMPLETED.handlePhase(build, getStatus(build), listener);
        sendEvent(build, event, json);
    }

    /* Currently not emitting onDeleted events. This should be fixed.
    @Override
    public void onDeleted(Run build) {
        String update = String.format("onDeleted");
        bindSocket(build);
        publisher.send(update.getBytes(), 0);
    }
    */

    @Override
    public void onFinalized(Run build) {
        String event = "onFinalized";
        String json = Phase.FINISHED.handlePhase(build, getStatus(build), TaskListener.NULL);
        sendEvent(build, event, json);
    }

    @Override
    public void onStarted(Run build, TaskListener listener) {
        String event = "onStarted";
        String json = Phase.STARTED.handlePhase(build, getStatus(build), listener);
        sendEvent(build, event, json);
    }

    private void sendEvent(Run build, String event, String json) {
        ZMQ.Socket socket;
        if (json != null) {
            socket = bindSocket(build);
            if (socket != null) {
                event = event + " " + json;
                try {
                    socket.send(event.getBytes(), 0);
                } catch (ZMQException e) {
                    LOGGER.log(Level.INFO,
                        "Unable to send event. " + e.toString(), e);
                }
            }
        }
    }

    private String getStatus(Run r) {
        Result result = r.getResult();
        String status = null;
        if (result != null) {
            status = result.toString();
        }
        return status;
    }
}
