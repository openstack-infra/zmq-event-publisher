/*
 * Copyright Authors of the Jenkins Notification Plugin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.ZMQEventPublisher;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.AbstractProject;
import hudson.util.FormValidation;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class HudsonNotificationProperty extends
        JobProperty<AbstractProject<?, ?>> {

    final public boolean enabled;

    @DataBoundConstructor
    public HudsonNotificationProperty(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public HudsonNotificationPropertyDescriptor getDescriptor() {
        return (HudsonNotificationPropertyDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class HudsonNotificationPropertyDescriptor extends JobPropertyDescriptor {

        private boolean globallyEnabled;
        private int port;

        public HudsonNotificationPropertyDescriptor() {
            globallyEnabled = false;
            port = 8888;
            load();
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends Job> jobType) {
            return true;
        }

        public String getDisplayName() {
            return "Jenkins ZMQ Event Publisher";
        }

        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
            globallyEnabled = json.getBoolean("globallyEnabled");
            port = json.getInt("port");
            save();
            return true;
        }

        public boolean isGloballyEnabled() {
            return globallyEnabled;
        }

        public int getPort() {
            return port;
        }
    }
}
