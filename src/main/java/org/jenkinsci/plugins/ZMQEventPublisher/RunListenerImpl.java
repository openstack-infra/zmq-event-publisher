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

import org.zeromq.ZMQ;

/*
 * Listener to publish Jenkins build events through ZMQ
 */
@Extension
public class RunListenerImpl extends RunListener<Run> {
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

    private void bindSocket(Run build) {
        int tmpPort = getPort(build);
        if (publisher == null) {
            port = tmpPort;
            publisher = context.socket(ZMQ.PUB);
            bind_addr = String.format("tcp://*:%d", port);
            publisher.bind(bind_addr);
        }
        else if (tmpPort != port) {
            publisher.unbind(bind_addr);
            publisher.close();
            publisher = context.socket(ZMQ.PUB);
            port = tmpPort;
            bind_addr = String.format("tcp://*:%d", port);
            publisher.bind(bind_addr);
        }
    }

    @Override
    public void onCompleted(Run build, TaskListener listener) {
        String event = "onCompleted";
        String json = Phase.COMPLETED.handlePhase(build, getStatus(build), listener);
        if (json != null) {
            bindSocket(build);
            event = event + " " + json;
            publisher.send(event.getBytes(), 0);
        }
    }

    /*
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
        if (json != null) {
            bindSocket(build);
            event = event + " " + json;
            publisher.send(event.getBytes(), 0);
        }
    }

    @Override
    public void onStarted(Run build, TaskListener listener) {
        String event = "onStarted";
        String json = Phase.STARTED.handlePhase(build, getStatus(build), listener);
        if (json != null) {
            bindSocket(build);
            event = event + " " + json;
            publisher.send(event.getBytes(), 0);
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
