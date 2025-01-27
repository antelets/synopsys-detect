/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.workflow.project;

import com.synopsys.integration.detect.configuration.enumeration.DefaultVersionNameScheme;

public class ProjectNameVersionOptions {
    public final String sourcePathName;
    public final String overrideProjectName;
    public final String overrideProjectVersionName;

    public ProjectNameVersionOptions(final String sourcePathName, final String overrideProjectName, final String overrideProjectVersionName) {
        this.sourcePathName = sourcePathName;
        this.overrideProjectName = overrideProjectName;
        this.overrideProjectVersionName = overrideProjectVersionName;
    }
}
