/*
 * Copyright 2013 Hewlett-Packard Development Company, L.P.
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

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.List;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jenkinsci.plugins.ZMQEventPublisher.model.BuildState;;
import org.jenkinsci.plugins.ZMQEventPublisher.model.JobState;;

public enum Phase {
    STARTED, COMPLETED, FINISHED;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String handlePhase(Run run, String status, TaskListener listener) {
        HudsonNotificationProperty property = (HudsonNotificationProperty) run.getParent().getProperty(HudsonNotificationProperty.class);
        if (property != null) {
            HudsonNotificationProperty.HudsonNotificationPropertyDescriptor globalProperty = property.getDescriptor();
            if (globalProperty.isGloballyEnabled() || property.isEnabled()) {
                return buildMessage(run.getParent(), run, status);
            }
        }
        return null;
    }

    private Gson gson = new GsonBuilder().setFieldNamingPolicy(
        FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private String buildMessage(Job job, Run run, String status) {
        JobState jobState = new JobState();
        jobState.setName(job.getName());
        jobState.setUrl(job.getUrl());
        BuildState buildState = new BuildState();
        buildState.setNumber(run.number);
        buildState.setUrl(run.getUrl());
        buildState.setPhase(this);
        buildState.setStatus(status);

        String rootUrl = Hudson.getInstance().getRootUrl();
        if (rootUrl != null) {
            buildState.setFullUrl(rootUrl + run.getUrl());
        }

        jobState.setBuild(buildState);

        ParametersAction paramsAction = run.getAction(ParametersAction.class);
        if (paramsAction != null && run instanceof AbstractBuild) {
            AbstractBuild build = (AbstractBuild) run;
            EnvVars env = new EnvVars();
            for (ParameterValue value : paramsAction.getParameters())
                if (!value.isSensitive())
                    value.buildEnvVars(build, env);
            buildState.setParameters(env);
        }

        return gson.toJson(jobState);
    }
}
