package org.jenkinsci.plugins.ZMQEventPublisher;

import hudson.Extension;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import org.zeromq.ZMQ;

import java.io.IOException;
import java.lang.InterruptedException;

/*
 * Listener to publish Jenkins build events through ZMQ
 *
 * @author Clark Boylan
 */
@Extension
public class RunListenerImpl extends RunListener<AbstractBuild> {
    private final int port;
    private ZMQ.Context context;
    private ZMQ.Socket publisher;
    private final String bind_addr;

    public RunListenerImpl() {
        super(AbstractBuild.class);
        this.port = 8888;
        this.context = ZMQ.context(1);
        this.publisher = context.socket(ZMQ.PUB);

        bind_addr = String.format("tcp://*:%d", this.port);
        this.publisher.bind(bind_addr);
    }

    @Override
    public void onCompleted(AbstractBuild build, TaskListener listener) {
        String update = String.format("onCompleted");
        try {
        EnvVars env = build.getEnvironment(listener);
        update = update + " " + env.toString();
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        this.publisher.send(update.getBytes(), 0);
    }

    @Override
    public void onDeleted(AbstractBuild build) {
        String update = String.format("onDeleted");
        this.publisher.send(update.getBytes(), 0);
    }

    @Override
    public void onFinalized(AbstractBuild build) {
        String update = String.format("onFinalized");
        this.publisher.send(update.getBytes(), 0);
    }

    @Override
    public void onStarted(AbstractBuild build, TaskListener listener) {
        String update = String.format("onStarted");
        try {
        EnvVars env = build.getEnvironment(listener);
        update = update + " " + env.toString();
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        this.publisher.send(update.getBytes(), 0);
    }
}
