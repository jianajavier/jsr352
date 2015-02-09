/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.purgeInMemoryRepository;

import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.Assert;
import org.junit.Test;

public class PurgeInMemoryRepositoryIT extends AbstractIT {
    private static final long purgeSleepMillis = 2000;
    static final String prepurgeXml = "prepurge.xml";
    static final String purgeInMemoryRepositoryXml = "purgeInMemoryRepository.xml";

    @Test
    public void purgeAllJobs() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        params.setProperty("purgeAllJobs", "true");
        startAndVerifyPurgeJob();

        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge2JobExecutionId));
    }

    @Test
    public void jobExecutionIds() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //non-existent job execution id 999 will be ignored
        params.setProperty("jobExecutionIds", String.format("%s,%s,%s", prepurge1JobExecutionId, prepurge2JobExecutionId, 999));
        startAndVerifyPurgeJob();

        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge2JobExecutionId));
    }


    @Test
    public void numberOfRecentJobExecutionsToKeep() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //2 job executions to keep:
        //the purge job itself
        //prepurge2
        params.setProperty("numberOfRecentJobExecutionsToKeep", "2");
        startAndVerifyPurgeJob();

        Assert.assertEquals(null, jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNotNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
    }

    @Test
    public void jobExecutionIdFrom() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions whose id >= prepurge2JobExecutionId
        params.setProperty("jobExecutionIdFrom", String.valueOf(prepurge2JobExecutionId));
        startAndVerifyPurgeJob();

        Assert.assertNotNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
    }

    @Test
    public void jobExecutionIdFromIncludeRunningOnes() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions whose id >= prepurge2JobExecutionId
        params.setProperty("jobExecutionIdFrom", String.valueOf(prepurge2JobExecutionId));
        //include running job executions
        params.setProperty("keepRunningJobExecutions", String.valueOf(false));

        startJob(purgeInMemoryRepositoryXml);
        Thread.sleep(purgeSleepMillis);
        final long purgeJobExecutionId = prepurge2JobExecutionId + 1;
        Assert.assertNull(jobOperator.getJobExecution(purgeJobExecutionId));

        Assert.assertNotNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
    }

    @Test
    public void jobExecutionIdTo() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions whose id <= prepurge2JobExecutionId
        params.setProperty("jobExecutionIdTo", String.valueOf(prepurge2JobExecutionId));
        startAndVerifyPurgeJob();

        Assert.assertNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
    }

    @Test
    public void jobExecutionIdFromTo() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();
        final long prepurge3JobExecutionId = prepurge();

        //purge job executions whose id between prepurge2JobExecutionId and prepurge3JobExecutionId
        params.setProperty("jobExecutionIdFrom", String.valueOf(prepurge2JobExecutionId));
        params.setProperty("jobExecutionIdTo", String.valueOf(prepurge3JobExecutionId));
        startAndVerifyPurgeJob();

        Assert.assertNotNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge3JobExecutionId));
    }

    @Test
    public void withinPastMinutes() throws Exception {
        final long prepurge1JobExecutionId = prepurge();
        final long prepurge2JobExecutionId = prepurge();

        //purge job executions which ended within past 5 minutes
        params.setProperty("withinPastMinutes", "5");
        //keepRunningJobExecutions batch property defaults to true, the following sets it to its default value
        params.setProperty("keepRunningJobExecutions", String.valueOf(true));
        startAndVerifyPurgeJob();

        Assert.assertNull(jobOperator.getJobExecution(prepurge1JobExecutionId));
        Assert.assertNull(jobOperator.getJobExecution(prepurge2JobExecutionId));
    }


    public long prepurge() throws Exception {
        startJob(prepurgeXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        System.out.printf("%s job execution id: %s, status: %s%n", prepurgeXml, jobExecutionId, jobExecution.getBatchStatus());
        return jobExecutionId;
    }

    public void startAndVerifyPurgeJob() throws Exception {
        startJob(purgeInMemoryRepositoryXml);
        awaitTermination();

        //the current job will not be purged, and should complete
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        Assert.assertNotNull(jobOperator.getJobExecution(jobExecutionId));
    }
}