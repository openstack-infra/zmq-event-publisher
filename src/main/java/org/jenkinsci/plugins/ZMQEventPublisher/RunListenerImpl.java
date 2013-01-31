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
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.util.DaemonThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Listener to publish Jenkins build events through ZMQ
 */
@Extension
public class RunListenerImpl extends RunListener<Run> {
    public static final Logger LOGGER =
        Logger.getLogger(RunListenerImpl.class.getName());
    private final LinkedBlockingQueue<String> queue =
        new LinkedBlockingQueue<String>(queueLength);
    // ZMQ has a high water mark of 1000 events.
    private static final int queueLength = 1024;
    private static final DaemonThreadFactory threadFactory =
        new DaemonThreadFactory();
    private ZMQRunnable ZMQRunner;
    private Thread thread;

    public RunListenerImpl() {
        super(Run.class);
        ZMQRunner = new ZMQRunnable(queue);
        thread = threadFactory.newThread(ZMQRunner);
        thread.start();
    }

    @Override
    public void onCompleted(Run build, TaskListener listener) {
        String event = "onCompleted";
        String json = Phase.COMPLETED.handlePhase(build, getStatus(build), listener);
        sendEvent(event, json);
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
        sendEvent(event, json);
    }

    @Override
    public void onStarted(Run build, TaskListener listener) {
        String event = "onStarted";
        String json = Phase.STARTED.handlePhase(build, getStatus(build), listener);
        sendEvent(event, json);
    }

    private void sendEvent(String event, String json) {
        if (json != null) {
            event = event + " " + json;
            // Offer the event. If the queue is full this will not block.
            // We may drop events but this should prevent starvation in
            // the calling Jenkins threads.
            if (!queue.offer(event)) {
                LOGGER.log(Level.INFO,
                    "Unable to add event to ZMQ queue.");
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
