/**
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.detect.lifecycle.run.operation;

import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.bdio2.Bdio2Factory;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.detect.configuration.DetectConfigurationFactory;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.lifecycle.run.RunContext;
import com.synopsys.integration.detect.lifecycle.run.RunOptions;
import com.synopsys.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.AggregateOptionsOperation;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.BdioFileGenerationOperation;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.BinaryScanOperation;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.CodeLocationOperation;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.CodeLocationResultOperation;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.FullScanPostProcessingOperation;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.ImpactAnalysisOperation;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.ProjectCreationOperation;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.ProjectDecisionOperation;
import com.synopsys.integration.detect.lifecycle.run.operation.blackduck.SignatureScanOperation;
import com.synopsys.integration.detect.tool.binaryscanner.BinaryScanOptions;
import com.synopsys.integration.detect.tool.impactanalysis.BlackDuckImpactAnalysisTool;
import com.synopsys.integration.detect.tool.impactanalysis.ImpactAnalysisOptions;
import com.synopsys.integration.detect.tool.impactanalysis.service.ImpactAnalysisBatchRunner;
import com.synopsys.integration.detect.tool.impactanalysis.service.ImpactAnalysisUploadService;
import com.synopsys.integration.detect.tool.signaturescanner.BlackDuckSignatureScannerOptions;
import com.synopsys.integration.detect.tool.signaturescanner.BlackDuckSignatureScannerTool;
import com.synopsys.integration.detect.util.filter.DetectToolFilter;
import com.synopsys.integration.detect.workflow.bdio.BdioManager;
import com.synopsys.integration.detect.workflow.blackduck.BlackDuckPostOptions;
import com.synopsys.integration.detect.workflow.blackduck.DetectCustomFieldService;
import com.synopsys.integration.detect.workflow.blackduck.DetectProjectServiceOptions;
import com.synopsys.integration.detect.workflow.blackduck.codelocation.CodeLocationResultCalculator;
import com.synopsys.integration.detect.workflow.project.ProjectNameVersionDecider;
import com.synopsys.integration.detect.workflow.project.ProjectNameVersionOptions;
import com.synopsys.integration.util.IntegrationEscapeUtil;
import com.synopsys.integration.util.NoThreadExecutorService;

public class OperationFactory {
    private final RunContext runContext;
    private final RunOptions runOptions;

    public OperationFactory(RunContext runContext) {
        this.runContext = runContext;
        this.runOptions = runContext.createRunOptions();
    }

    public final PolarisOperation createPolarisOperation() {
        return new PolarisOperation(runContext.getProductRunData(), runContext.getDetectConfiguration(), runContext.getDirectoryManager(), runOptions.getDetectToolFilter(), runContext.getEventSystem());
    }

    public final DockerOperation createDockerOperation() {
        return new DockerOperation(runContext.getDirectoryManager(), runContext.getEventSystem(), runContext.getDetectDetectableFactory(), runOptions.getDetectToolFilter(), runContext.getExtractionEnvironmentProvider(),
            runContext.getCodeLocationConverter());
    }

    public final BazelOperation createBazelOperation() {
        return new BazelOperation(runContext.getDirectoryManager(), runContext.getEventSystem(), runContext.getDetectDetectableFactory(), runOptions.getDetectToolFilter(), runContext.getExtractionEnvironmentProvider(),
            runContext.getCodeLocationConverter());
    }

    public final DetectorOperation createDetectorOperation() {
        return new DetectorOperation(runContext.getDetectConfiguration(), runContext.getDetectConfigurationFactory(), runContext.getDirectoryManager(), runContext.getEventSystem(), runContext.getDetectDetectableFactory(),
            runOptions.getDetectToolFilter(),
            runContext.getExtractionEnvironmentProvider(), runContext.getCodeLocationConverter());
    }

    public final FullScanOperation createFullScanOperation(boolean hasPriorOperationsSucceeded) {
        ImpactAnalysisOptions impactAnalysisOptions = runContext.getDetectConfigurationFactory().createImpactAnalysisOptions();
        return new FullScanOperation(runContext.getDetectContext(), runContext.getDetectInfo(), runContext.getProductRunData(), runContext.getDirectoryManager(), runContext.getEventSystem(), runContext.getDetectConfigurationFactory(),
            runOptions.getDetectToolFilter(),
            runContext.getCodeLocationNameManager(), runContext.getBdioCodeLocationCreator(), runOptions, hasPriorOperationsSucceeded, impactAnalysisOptions);

    }

    public final RapidScanOperation createRapidScanOperation() {
        return new RapidScanOperation(runContext.getProductRunData(), runContext.getGson(), runContext.getEventSystem(), runContext.getDirectoryManager(), runContext.getDetectConfigurationFactory().findTimeoutInSeconds());
    }

    public final AggregateOptionsOperation createAggregateOptionsOperation() {
        return new AggregateOptionsOperation(runOptions);
    }

    public final BdioFileGenerationOperation createBdioFileGenerationOperation() {
        BdioManager bdioManager = new BdioManager(runContext.getDetectInfo(), new SimpleBdioFactory(), new ExternalIdFactory(), new Bdio2Factory(), new IntegrationEscapeUtil(), runContext.getCodeLocationNameManager(),
            runContext.getBdioCodeLocationCreator(), runContext.getDirectoryManager());
        return new BdioFileGenerationOperation(runOptions, runContext.getDetectConfigurationFactory().createBdioOptions(), bdioManager, runContext.getEventSystem());
    }

    public final BinaryScanOperation createBinaryScanOperation() {
        BlackDuckRunData blackDuckRunData = runContext.getProductRunData().getBlackDuckRunData();
        BinaryScanOptions binaryScanOptions = runContext.getDetectConfigurationFactory().createBinaryScanOptions();

        return new BinaryScanOperation(blackDuckRunData, runOptions.getDetectToolFilter(), binaryScanOptions, runContext.getEventSystem(), runContext.getDirectoryManager(), runContext.getCodeLocationNameManager());
    }

    public final CodeLocationOperation createCodeLocationOperation() {
        return new CodeLocationOperation(runContext.getProductRunData());
    }

    public final CodeLocationResultOperation createCodeLocationResultOperation() {
        return new CodeLocationResultOperation(runContext.getProductRunData(), new CodeLocationResultCalculator(), runContext.getEventSystem());
    }

    public final FullScanPostProcessingOperation createFullScanPostProcessingOperation() {
        BlackDuckRunData blackDuckRunData = runContext.getProductRunData().getBlackDuckRunData();
        DetectConfigurationFactory detectConfigurationFactory = runContext.getDetectConfigurationFactory();
        BlackDuckPostOptions blackDuckPostOptions = detectConfigurationFactory.createBlackDuckPostOptions();
        Long timeoutInSeconds = detectConfigurationFactory.findTimeoutInSeconds();

        return new FullScanPostProcessingOperation(runContext.getProductRunData(), runOptions.getDetectToolFilter(), blackDuckPostOptions, runContext.getEventSystem(), timeoutInSeconds);
    }

    public final ImpactAnalysisOperation createImpactAnalysisOperation() {
        BlackDuckRunData blackDuckRunData = runContext.getProductRunData().getBlackDuckRunData();
        ImpactAnalysisOptions impactAnalysisOptions = runContext.getDetectConfigurationFactory().createImpactAnalysisOptions();
        BlackDuckImpactAnalysisTool blackDuckImpactAnalysisTool;
        if (runContext.getProductRunData().shouldUseBlackDuckProduct() && blackDuckRunData.isOnline()) {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            ImpactAnalysisBatchRunner impactAnalysisBatchRunner = new ImpactAnalysisBatchRunner(blackDuckServicesFactory.getLogger(), blackDuckServicesFactory.getBlackDuckApiClient(), new NoThreadExecutorService(),
                blackDuckServicesFactory.getGson());
            ImpactAnalysisUploadService impactAnalysisUploadService = new ImpactAnalysisUploadService(impactAnalysisBatchRunner, blackDuckServicesFactory.createCodeLocationCreationService());
            blackDuckImpactAnalysisTool = BlackDuckImpactAnalysisTool
                                              .ONLINE(runContext.getDirectoryManager(), runContext.getCodeLocationNameManager(), impactAnalysisOptions, blackDuckServicesFactory.getBlackDuckApiClient(), impactAnalysisUploadService,
                                                  blackDuckServicesFactory.createCodeLocationService(), runContext.getEventSystem());
        } else {
            blackDuckImpactAnalysisTool = BlackDuckImpactAnalysisTool.OFFLINE(runContext.getDirectoryManager(), runContext.getCodeLocationNameManager(), impactAnalysisOptions, runContext.getEventSystem());
        }
        return new ImpactAnalysisOperation(runOptions.getDetectToolFilter(), blackDuckImpactAnalysisTool);
    }

    public final ProjectCreationOperation createProjectCreationOperation() throws DetectUserFriendlyException {
        DetectProjectServiceOptions options = runContext.getDetectConfigurationFactory().createDetectProjectServiceOptions();
        BlackDuckRunData blackDuckRunData = runContext.getProductRunData().getBlackDuckRunData();
        DetectCustomFieldService detectCustomFieldService = new DetectCustomFieldService();

        return new ProjectCreationOperation(runContext.getProductRunData(), runOptions, options, detectCustomFieldService);
    }

    public final ProjectDecisionOperation createProjectDecisionOperation() {
        ProjectNameVersionOptions projectNameVersionOptions = runContext.getDetectConfigurationFactory().createProjectNameVersionOptions(runContext.getDirectoryManager().getSourceDirectory().getName());
        ProjectNameVersionDecider projectNameVersionDecider = new ProjectNameVersionDecider(projectNameVersionOptions);
        return new ProjectDecisionOperation(runOptions, projectNameVersionDecider);
    }

    public final SignatureScanOperation createSignatureScanOperation() throws DetectUserFriendlyException {
        DetectToolFilter detectToolFilter = runOptions.getDetectToolFilter();
        BlackDuckRunData blackDuckRunData = runContext.getProductRunData().getBlackDuckRunData();
        BlackDuckSignatureScannerOptions blackDuckSignatureScannerOptions = runContext.getDetectConfigurationFactory().createBlackDuckSignatureScannerOptions();
        BlackDuckSignatureScannerTool blackDuckSignatureScannerTool = new BlackDuckSignatureScannerTool(blackDuckSignatureScannerOptions, runContext.getDetectContext());

        return new SignatureScanOperation(blackDuckRunData, detectToolFilter, blackDuckSignatureScannerTool, runContext.getEventSystem());
    }

}