/*!
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 *
 * Copyright (c) 2002-2023 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.web.http.api.resources.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileDto;
import org.pentaho.platform.api.scheduler2.IBackgroundExecutionStreamProvider;
import org.pentaho.platform.api.scheduler2.IBlockoutManager;
import org.pentaho.platform.api.scheduler2.IJobFilter;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.api.util.IPdiContentProvider;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;
import org.pentaho.platform.security.policy.rolebased.actions.AdministerSecurityAction;
import org.pentaho.platform.security.policy.rolebased.actions.SchedulerAction;
import org.pentaho.platform.web.http.api.resources.JobRequest;
import org.pentaho.platform.web.http.api.resources.JobScheduleParam;
import org.pentaho.platform.web.http.api.resources.JobScheduleRequest;
import org.pentaho.platform.web.http.api.resources.SchedulerOutputPathResolver;
import org.pentaho.platform.web.http.api.resources.SchedulerResourceUtil;
import org.pentaho.platform.web.http.api.resources.SessionResource;
import org.pentaho.platform.web.http.api.resources.proxies.BlockStatusProxy;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith( MockitoJUnitRunner.class )
public class SchedulerServiceTest {

  private static SchedulerService schedulerService;

  @Before
  public void setUp() throws SchedulerException, IOException, IllegalAccessException {
    schedulerService = spy( new SchedulerService() );
    doCallRealMethod().when( schedulerService ).createJob( any() );
    schedulerService.policy = mock( IAuthorizationPolicy.class );
    schedulerService.scheduler = mock( IScheduler.class );
    schedulerService.repository = mock( IUnifiedRepository.class );
    schedulerService.blockoutManager = mock( IBlockoutManager.class );
  }

  @After
  public void cleanup() {
    schedulerService = null;
  }

  @Test
  public void testCreateJob() throws Exception {

    List<JobScheduleParam> jobParameters = new ArrayList<>();
    JobScheduleParam jobScheduleParam1 = mock( JobScheduleParam.class );
    doReturn( "name1" ).when( jobScheduleParam1 ).getName();
    doReturn( "value1" ).when( jobScheduleParam1 ).getValue();
    jobParameters.add( jobScheduleParam1 );

    Job job = mock( Job.class );

    JobScheduleRequest scheduleRequest = mock( JobScheduleRequest.class );
    doReturn( "className" ).when( scheduleRequest ).getActionClass();
    doReturn( "jobName" ).when( scheduleRequest ).getJobName();
    doReturn( "runSafeMode" ).when( scheduleRequest ).getRunSafeMode();
    doReturn( "gatheringMetrics" ).when( scheduleRequest ).getGatheringMetrics();
    doReturn( "Basic" ).when( scheduleRequest ).getLogLevel();
    doReturn( jobParameters ).when( scheduleRequest ).getJobParameters();
    doNothing().when( scheduleRequest ).setJobName( nullable( String.class ) );
    doReturn( "timezone" ).when( scheduleRequest ).getTimeZone();
    doNothing().when( schedulerService ).updateStartDateForTimeZone( scheduleRequest );

    doReturn( true ).when( schedulerService ).isPdiFile( any( RepositoryFile.class ) );
    doReturn( false ).when( schedulerService ).isPdiFile( null );

    SchedulerOutputPathResolver schedulerOutputPathResolver = mock( SchedulerOutputPathResolver.class );
    doReturn( "outputFile" ).when( schedulerOutputPathResolver ).resolveOutputFilePath();
    doReturn( schedulerOutputPathResolver ).when( schedulerService ).getSchedulerOutputPathResolver( any( JobScheduleRequest.class ) );

    SimpleJobTrigger simpleJobTrigger = mock( SimpleJobTrigger.class );

    RepositoryFile repositoryFile = mock( RepositoryFile.class );
    doReturn( "file.ext" ).when( repositoryFile ).getName();

    Map<String, Serializable> metadata = mock( Map.class );
    doReturn( metadata ).when( schedulerService.repository ).getFileMetadata( nullable( String.class ) );
    doReturn( true ).when( metadata ).containsKey( RepositoryFile.SCHEDULABLE_KEY );
    doReturn( "true" ).when( metadata ).get( RepositoryFile.SCHEDULABLE_KEY );

    doReturn( simpleJobTrigger ).when( scheduleRequest ).getSimpleJobTrigger();
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doReturn( "file.ext" ).when( scheduleRequest ).getInputFile();
    doReturn( repositoryFile ).when( schedulerService.repository ).getFile( nullable( String.class ) );

    doReturn( true ).when( schedulerService ).getAutoCreateUniqueFilename( any( JobScheduleRequest.class ) );

    doReturn( job ).when( schedulerService.scheduler )
        .createJob( nullable( String.class ), nullable( String.class ), any( Map.class ), any( IJobTrigger.class ),
            any( IBackgroundExecutionStreamProvider.class ) );

    doReturn( Class.class ).when( schedulerService ).getAction( nullable( String.class ) );

    doReturn( job ).when( schedulerService.scheduler )
        .createJob( nullable( String.class ), any( Class.class ), any( Map.class ), any( IJobTrigger.class ) );

    //Test 1
    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = Mockito.mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() ).thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () ->
        SchedulerResourceUtil.convertScheduleRequestToJobTrigger( eq( scheduleRequest ), any( IScheduler.class ) ) ).thenCallRealMethod();
      Job returnJob = schedulerService.createJob( scheduleRequest );
      assertEquals( job, returnJob );

      //Test 2
      doReturn( "" ).when( scheduleRequest ).getJobName();

      returnJob = schedulerService.createJob( scheduleRequest );
      assertEquals( job, returnJob );

      //Test 3
      doReturn( "" ).when( scheduleRequest ).getInputFile();
      doReturn( "" ).when( scheduleRequest ).getActionClass();

      returnJob = schedulerService.createJob( scheduleRequest );
      assertEquals( job, returnJob );

      verify( scheduleRequest, times( 15 ) ).getSimpleJobTrigger();
      verify( scheduleRequest, times( 9 ) ).getInputFile();
      verify( schedulerService.policy, times( 3 ) ).isAllowed( SchedulerAction.NAME );
      verify( schedulerService.repository, times( 2 ) ).getFile( nullable( String.class ) );
      verify( scheduleRequest, times( 6 ) ).getJobName();
      verify( scheduleRequest, times( 3 ) ).setJobName( nullable( String.class ) );
      verify( scheduleRequest, times( 5 ) ).getActionClass();
      verify( schedulerService.repository, times( 2 ) ).getFileMetadata( nullable( String.class ) );
      verify( schedulerService, times( 3 ) ).isPdiFile( nullable( RepositoryFile.class ) );
      verify( schedulerService, times( 2 ) ).handlePDIScheduling( any( RepositoryFile.class ), any( HashMap.class ),
        any( HashMap.class ) );
      verify( schedulerService, times( 2 ) ).getSchedulerOutputPathResolver( any( JobScheduleRequest.class ) );
      verify( scheduleRequest, times( 5 ) ).getActionClass();
      verify( schedulerService ).getAction( nullable( String.class ) );
      verify( schedulerService, times( 3 ) ).updateStartDateForTimeZone( scheduleRequest );
    }
  }

  @Test
  public void testCreateJobException() throws Exception {

    List<JobScheduleParam> jobParameters = new ArrayList<>();
    JobScheduleParam jobScheduleParam1 = mock( JobScheduleParam.class );
    doReturn( "name1" ).when( jobScheduleParam1 ).getName();
    doReturn( "value1" ).when( jobScheduleParam1 ).getValue();
    jobParameters.add( jobScheduleParam1 );

    Job job = mock( Job.class );

    JobScheduleRequest scheduleRequest = mock( JobScheduleRequest.class );
    doReturn( "className" ).when( scheduleRequest ).getActionClass();
    doReturn( "jobName" ).when( scheduleRequest ).getJobName();
    doReturn( jobParameters ).when( scheduleRequest ).getJobParameters();
    doNothing().when( scheduleRequest ).setJobName( nullable( String.class ) );

    doReturn( true ).when( schedulerService ).isPdiFile( nullable( RepositoryFile.class ) );

    SchedulerOutputPathResolver schedulerOutputPathResolver = mock( SchedulerOutputPathResolver.class );

    SimpleJobTrigger simpleJobTrigger = mock( SimpleJobTrigger.class );

    RepositoryFile repositoryFile = mock( RepositoryFile.class );

    Map<String, Serializable> metadata = mock( Map.class );
    doReturn( metadata ).when( schedulerService.repository ).getFileMetadata( nullable( String.class ) );
    doReturn( true ).when( metadata ).containsKey( RepositoryFile.SCHEDULABLE_KEY );

    doReturn( simpleJobTrigger ).when( scheduleRequest ).getSimpleJobTrigger();
    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doReturn( "file.ext" ).when( scheduleRequest ).getInputFile();
    doReturn( repositoryFile ).when( schedulerService.repository ).getFile( nullable( String.class ) );

    try ( MockedStatic<SchedulerResourceUtil> schedulerResourceUtilMockedStatic = Mockito.mockStatic( SchedulerResourceUtil.class ) ) {
      IPdiContentProvider mockPdiContentProvider = mock( IPdiContentProvider.class );
      schedulerResourceUtilMockedStatic.when( () -> SchedulerResourceUtil.getiPdiContentProvider() )
        .thenReturn( mockPdiContentProvider );
      schedulerResourceUtilMockedStatic.when( () ->
          SchedulerResourceUtil.convertScheduleRequestToJobTrigger( eq( scheduleRequest ), any( IScheduler.class ) ) )
        .thenCallRealMethod();
      //Test 1
      try {
        schedulerService.createJob( scheduleRequest );
        fail();
      } catch ( SecurityException e ) {
        //Should catch it
      }

      //Test 2
      doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
      doReturn( "false" ).when( metadata ).get( RepositoryFile.SCHEDULABLE_KEY );

      try {
        schedulerService.createJob( scheduleRequest );
        fail();
      } catch ( IllegalAccessException e ) {
        //Should catch it
      }

      //Test 3
      doReturn( "" ).when( scheduleRequest ).getInputFile();
      doThrow( new ClassNotFoundException() ).when( schedulerService ).getAction( nullable( String.class ) );

      try {
        schedulerService.createJob( scheduleRequest );
        fail();
      } catch ( RuntimeException e ) {
        //Should catch it
      }

      verify( scheduleRequest, times( 7 ) ).getSimpleJobTrigger();
      verify( scheduleRequest, times( 3 ) ).getInputFile();
      verify( schedulerService.policy, times( 3 ) ).isAllowed( SchedulerAction.NAME );
      verify( schedulerService.repository, times( 1 ) ).getFile( nullable( String.class ) );
      verify( scheduleRequest, times( 1 ) ).getJobName();
      verify( scheduleRequest, times( 2 ) ).setJobName( nullable( String.class ) );
      verify( scheduleRequest, times( 7 ) ).getActionClass();
      verify( schedulerService.repository, times( 1 ) ).getFileMetadata( nullable( String.class ) );
      verify( schedulerService, times( 1 ) ).isPdiFile( nullable( RepositoryFile.class ) );
      verify( schedulerService, times( 1 ) ).handlePDIScheduling( nullable( RepositoryFile.class ),
        any( HashMap.class ), any( HashMap.class ) );
      verify( scheduleRequest, times( 7 ) ).getActionClass();
      verify( schedulerService ).getAction( nullable( String.class ) );
    }
  }

  @Test
  public void testTriggerNow() throws Exception {

    JobRequest jobRequest = mock( JobRequest.class );
    Job job = mock( Job.class );

    doReturn( job ).when( schedulerService.scheduler ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService.policy ).isAllowed( nullable( String.class ) );
    doNothing().when( schedulerService.scheduler ).triggerNow( nullable( String.class ) );

    //Test 1
    Job resultJob1 = schedulerService.triggerNow( jobRequest.getJobId() );

    assertEquals( job, resultJob1 );

    //Test 2
    doReturn( "test" ).when( job ).getUserName();
    doReturn( false ).when( schedulerService.policy ).isAllowed( nullable( String.class ) );

    IPentahoSession pentahoSession = mock( IPentahoSession.class );
    doReturn( "test" ).when( pentahoSession ).getName();
    doReturn( pentahoSession ).when( schedulerService ).getSession();

    Job resultJob2 = schedulerService.triggerNow( jobRequest.getJobId() );

    assertEquals( job, resultJob2 );

    verify( schedulerService.scheduler, times( 4 ) ).getJob( nullable( String.class ) );
    verify( schedulerService.scheduler, times( 2 ) ).triggerNow( nullable( String.class ) );
    verify( schedulerService.policy, times( 2 ) ).isAllowed( nullable( String.class ) );
  }

  @Test
  public void testGetContentCleanerJob() throws Exception {

    IJobFilter jobFilter = mock( IJobFilter.class );

    List<Job> jobs = new ArrayList<>();

    IPentahoSession session = mock( IPentahoSession.class );
    doReturn( session ).when( schedulerService ).getSession();
    doReturn( "sessionName" ).when( session ).getName();

    doReturn( true ).when( schedulerService.policy ).isAllowed( AdministerSecurityAction.NAME );
    doReturn( jobFilter ).when( schedulerService ).getJobFilter( anyBoolean(), nullable( String.class ) );
    doReturn( jobs ).when( schedulerService.scheduler ).getJobs( any( IJobFilter.class ) );

    //Test 1
    Job job = schedulerService.getContentCleanerJob();

    assertNull( job );

    //Test 2
    Job job1 = mock( Job.class );
    jobs.add( job1 );

    job = schedulerService.getContentCleanerJob();

    assertNotNull( job );

    verify( schedulerService, times( 2 ) ).getSession();
    verify( session, times( 2 ) ).getName();
    verify( schedulerService.policy, times( 2 ) ).isAllowed( AdministerSecurityAction.NAME );
    verify( schedulerService.scheduler, times( 2 ) ).getJobs( any( IJobFilter.class ) );
  }

  @Test
  public void testGetContentCleanerJobException() throws Exception {

    IJobFilter jobFilter = mock( IJobFilter.class );

    List<Job> jobs = new ArrayList<>();

    IPentahoSession session = mock( IPentahoSession.class );
    doReturn( session ).when( schedulerService ).getSession();
    doReturn( "sessionName" ).when( session ).getName();

    doReturn( true ).when( schedulerService.policy ).isAllowed( AdministerSecurityAction.NAME );
    doReturn( jobFilter ).when( schedulerService ).getJobFilter( anyBoolean(), nullable( String.class ) );
    doThrow( new SchedulerException( "" ) ).when( schedulerService.scheduler ).getJobs( any( IJobFilter.class ) );

    try {
      schedulerService.getContentCleanerJob();
      fail();
    } catch ( SchedulerException e ) {
      //Should catch the exception
    }

    verify( schedulerService ).getSession();
    verify( session ).getName();
    verify( schedulerService.policy ).isAllowed( AdministerSecurityAction.NAME );
    verify( schedulerService.scheduler ).getJobs( any( IJobFilter.class ) );
  }

  @Test
  public void testDoGetCanSchedule() {

    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    //Test 1
    String isAllowed = schedulerService.doGetCanSchedule();

    assertEquals( "true", isAllowed );

    //Test 2
    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    isAllowed = schedulerService.doGetCanSchedule();

    assertEquals( "false", isAllowed );

    verify( schedulerService.policy, times( 2 ) ).isAllowed( SchedulerAction.NAME );
  }

  @Test
  public void testGetState() throws SchedulerException {

    doReturn( IScheduler.SchedulerStatus.RUNNING ).when( schedulerService.scheduler ).getStatus();

    String state = schedulerService.getState();

    assertEquals( "RUNNING", state );

    verify( schedulerService.scheduler ).getStatus();
  }

  @Test
  public void testGetStateException() throws SchedulerException {

    doThrow( new SchedulerException( "" ) ).when( schedulerService.scheduler ).getStatus();

    try {
      schedulerService.getState();
      fail();
    } catch ( SchedulerException e ) {
      //Should go here
    }

    verify( schedulerService.scheduler ).getStatus();
  }

  @Test
  public void testStart() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doNothing().when( schedulerService.scheduler ).start();

    doReturn( IScheduler.SchedulerStatus.RUNNING ).when( schedulerService.scheduler ).getStatus();

    //Test 1
    String state = schedulerService.start();

    assertEquals( "RUNNING", state );

    //Test 2
    doReturn( IScheduler.SchedulerStatus.STOPPED ).when( schedulerService.scheduler ).getStatus();
    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    state = schedulerService.start();

    assertEquals( "STOPPED", state );

    verify( schedulerService.policy, times( 2 ) ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler, times( 1 ) ).start();
    verify( schedulerService.scheduler, times( 2 ) ).getStatus();
  }


  @Test
  public void testStartException() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doThrow( new SchedulerException( "" ) ).when( schedulerService.scheduler ).start();

    try {
      schedulerService.start();
      fail();
    } catch ( SchedulerException e ) {
      //Should go here
    }

    verify( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler ).start();
  }

  @Test
  public void testPause() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doNothing().when( schedulerService.scheduler ).pause();

    doReturn( IScheduler.SchedulerStatus.PAUSED ).when( schedulerService.scheduler ).getStatus();

    //Test 1
    String state = schedulerService.pause();

    assertEquals( "PAUSED", state );

    //Test 2
    doReturn( IScheduler.SchedulerStatus.RUNNING ).when( schedulerService.scheduler ).getStatus();
    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    state = schedulerService.pause();

    assertEquals( "RUNNING", state );

    verify( schedulerService.policy, times( 2 ) ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler, times( 1 ) ).pause();
    verify( schedulerService.scheduler, times( 2 ) ).getStatus();
  }

  @Test
  public void testPauseJob() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doNothing().when( schedulerService.scheduler ).pauseJob( nullable( String.class ) );
    schedulerService.pauseJob( "job-id" );
  }

  @Test
  public void testPauseJobException() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doThrow( new SchedulerException( "pause-exception" ) ).when( schedulerService.scheduler ).pauseJob( nullable( String.class ) );
    try {
      schedulerService.pauseJob( "job-id" );
    } catch ( SchedulerException e ) {
      assertEquals( "pause-exception", e.getMessage() );
    }
  }

  @Test
  public void testResumeJob() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doNothing().when( schedulerService.scheduler ).resumeJob( nullable( String.class ) );
    schedulerService.resumeJob( "job-id" );
  }

  @Test
  public void testResumeJobException() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doThrow( new SchedulerException( "pause-exception" ) ).when( schedulerService.scheduler ).resumeJob( nullable( String.class ) );
    try {
      schedulerService.resumeJob( "job-id" );
    } catch ( SchedulerException e ) {
      assertEquals( "pause-exception", e.getMessage() );
    }
  }

  @Test
  public void testRemoveJob() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doNothing().when( schedulerService.scheduler ).removeJob( nullable( String.class ) );
    schedulerService.removeJob( "job-id" );
  }

  @Test
  public void testRemoveJobException() throws SchedulerException {
    Job job = mock( Job.class );
    doReturn( job ).when( schedulerService ).getJob( nullable( String.class ) );
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doThrow( new SchedulerException( "pause-exception" ) ).when( schedulerService.scheduler ).removeJob( nullable( String.class ) );
    try {
      schedulerService.removeJob( "job-id" );
    } catch ( SchedulerException e ) {
      assertEquals( "pause-exception", e.getMessage() );
    }
  }

  @Test
  public void testPauseException() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doThrow( new SchedulerException( "" ) ).when( schedulerService.scheduler ).pause();

    try {
      schedulerService.pause();
      fail();
    } catch ( SchedulerException e ) {
      //Should go here
    }

    verify( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler ).pause();
  }

  @Test
  public void testShutdown() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doNothing().when( schedulerService.scheduler ).shutdown();

    doReturn( IScheduler.SchedulerStatus.STOPPED ).when( schedulerService.scheduler ).getStatus();

    //Test 1
    String state = schedulerService.shutdown();

    assertEquals( "STOPPED", state );

    //Test 2
    doReturn( IScheduler.SchedulerStatus.RUNNING ).when( schedulerService.scheduler ).getStatus();
    doReturn( false ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    state = schedulerService.shutdown();

    assertEquals( "RUNNING", state );

    verify( schedulerService.policy, times( 2 ) ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler, times( 1 ) ).shutdown();
    verify( schedulerService.scheduler, times( 2 ) ).getStatus();
  }


  @Test
  public void testShutdownException() throws SchedulerException {
    doReturn( true ).when( schedulerService.policy ).isAllowed( SchedulerAction.NAME );

    doThrow( new SchedulerException( "" ) ).when( schedulerService.scheduler ).shutdown();

    try {
      schedulerService.shutdown();
      fail();
    } catch ( SchedulerException e ) {
      //Should go here
    }

    verify( schedulerService.policy ).isAllowed( SchedulerAction.NAME );
    verify( schedulerService.scheduler ).shutdown();
  }

  @Test
  public void testGetJobs() throws Exception {
    IPentahoSession mockPentahoSession = mock( IPentahoSession.class );

    doReturn( mockPentahoSession ).when( schedulerService ).getSession();
    doReturn( "admin" ).when( mockPentahoSession ).getName();
    doReturn( true ).when( schedulerService ).canAdminister( mockPentahoSession );
    List<Job> mockJobs = new ArrayList<>();
    mockJobs.add( mock( Job.class ) );
    doReturn( mockJobs ).when( schedulerService.scheduler ).getJobs( any( IJobFilter.class ) );

    List<Job> jobs = schedulerService.getJobs();

    assertEquals( mockJobs, jobs );

    verify( schedulerService, times( 1 ) ).getSession();
    verify( mockPentahoSession, times( 1 ) ).getName();
    verify( schedulerService, times( 1 ) ).canAdminister( mockPentahoSession );
    verify( schedulerService.scheduler, times( 1 ) ).getJobs( any( IJobFilter.class ) );
  }

  @Test
  public void testDoGetGeneratedContentForSchedule() throws Exception {
    String lineageId = "test.prpt";

    FileService mockFileService = mock( FileService.class );
    doReturn( mockFileService ).when( schedulerService ).getFileService();

    SessionResource mockSessionResource = mock( SessionResource.class );
    doReturn( mockSessionResource ).when( schedulerService ).getSessionResource();

    String currentUserDir = "currentUserDir";
    doReturn( currentUserDir ).when( mockSessionResource ).doGetCurrentUserDir();

    List<RepositoryFileDto> mockList = mock( List.class );
    doReturn( mockList ).when( mockFileService )
        .searchGeneratedContent( currentUserDir, lineageId, QuartzScheduler.RESERVEDMAPKEY_LINEAGE_ID );

    List<RepositoryFileDto> list = schedulerService.doGetGeneratedContentForSchedule( lineageId );
    assertEquals( mockList, list );
  }

  @Test
  public void testGetJobState() throws Exception {
    JobRequest mockJobRequest = mock( JobRequest.class );

    String jobId = "jobId";
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    IPentahoSession mockSession = mock( IPentahoSession.class );
    doReturn( mockSession ).when( schedulerService ).getSession();

    Job mockJob = mock( Job.class );
    doReturn( mockJob ).when( schedulerService ).getJob( jobId );
    doReturn( Job.JobState.BLOCKED ).when( mockJob ).getState();

    String username = "username";
    doReturn( username ).when( mockJob ).getUserName();
    doReturn( username ).when( mockSession ).getName();

    // Test 1
    doReturn( true ).when( schedulerService ).isScheduleAllowed();

    Job.JobState testState = schedulerService.getJobState( mockJobRequest );
    assertEquals( Job.JobState.BLOCKED, testState );

    // Test 2
    doReturn( false ).when( schedulerService ).isScheduleAllowed();

    testState = schedulerService.getJobState( mockJobRequest );
    assertEquals( Job.JobState.BLOCKED, testState );

    verify( mockJobRequest, times( 2 ) ).getJobId();
    verify( schedulerService, times( 1 ) ).getSession();
    verify( schedulerService, times( 2 ) ).getJob( jobId );
    verify( mockJob, times( 2 ) ).getState();
    verify( mockJob, times( 1 ) ).getUserName();
    verify( mockSession, times( 1 ) ).getName();
  }

  @Test
  public void testGetJobStateError() throws Exception {
    JobRequest mockJobRequest = mock( JobRequest.class );

    String jobId = "jobId";
    doReturn( jobId ).when( mockJobRequest ).getJobId();

    IPentahoSession mockSession = mock( IPentahoSession.class );
    doReturn( mockSession ).when( schedulerService ).getSession();

    Job mockJob = mock( Job.class );
    doReturn( mockJob ).when( schedulerService ).getJob( jobId );

    String username = "username";
    doReturn( username ).when( mockJob ).getUserName();

    String sessionName = "notUsername";
    doReturn( sessionName ).when( mockSession ).getName();

    doReturn( false ).when( schedulerService ).isScheduleAllowed();

    try {
      schedulerService.getJobState( mockJobRequest );
      fail();
    } catch ( UnsupportedOperationException e ) {
      // Expected
    }
  }

  @Test
  public void testGetJobInfo() throws Exception {
    String jobId = "jobId";

    Job mockJob = mock( Job.class );
    doReturn( mockJob ).when( schedulerService ).getJob( jobId );

    IPentahoSession mockPentahoSession = mock( IPentahoSession.class );
    doReturn( mockPentahoSession ).when( schedulerService ).getSession();

    String sessionName = "sessionName";
    doReturn( sessionName ).when( mockPentahoSession ).getName();
    doReturn( sessionName ).when( mockJob ).getUserName();

    Map<String, Serializable> mockJobParams = mock( Map.class );
    doReturn( mockJobParams ).when( mockJob ).getJobParams();

    Set<String> jobParamsKeyset = new HashSet<>();
    doReturn( jobParamsKeyset ).when( mockJobParams ).keySet();

    String jobParamKey = "key";
    jobParamsKeyset.add( jobParamKey );

    String value = "value";
    String[] testArray = new String[]{value};
    doReturn( testArray ).when( mockJobParams ).get( jobParamKey );

    // Test 1
    Job testJob = schedulerService.getJobInfo( jobId );
    assertEquals( mockJob, testJob );

    // Test 2
    testJob = schedulerService.getJobInfo( jobId );
    assertEquals( mockJob, testJob );

    verify( mockJobParams, times( 2 ) ).put( eq( jobParamKey ), any( Serializable.class ) );
    verify( schedulerService, times( 2 ) ).getJob( jobId );
    verify( schedulerService, times( 2 ) ).getSession();
    verify( mockPentahoSession, times( 2 ) ).getName();
    verify( mockJob, times( 2 ) ).getUserName();
    verify( mockJob, times( 6 ) ).getJobParams();
    verify( mockJobParams, times( 2 ) ).keySet();
    verify( mockJobParams, times( 2 ) ).get( jobParamKey );
    verify( schedulerService, times( 2 ) ).canAdminister( null );
  }

  @Test
  public void testGetJobInfoError() throws Exception {
    String jobId = "jobId";

    Job mockJob = mock( Job.class );
    doReturn( mockJob ).when( schedulerService ).getJob( jobId );

    IPentahoSession mockPentahoSession = mock( IPentahoSession.class );
    doReturn( mockPentahoSession ).when( schedulerService ).getSession();

    String sessionName = "sessionName";
    doReturn( sessionName ).when( mockPentahoSession ).getName();

    String username = "username";
    doReturn( username ).when( mockJob ).getUserName();

    try {
      schedulerService.getJobInfo( jobId );
      fail();
    } catch ( RuntimeException e ) {
      // Expected
    }
  }

  @Test
  public void testIsScheduleAllowed() {

    // Test 1
    doReturn( true ).when( schedulerService ).isScheduleAllowed();

    Map<String, Serializable> metadata = mock( Map.class );

    doReturn( metadata ).when( schedulerService.repository ).getFileMetadata( nullable( String.class ) );

    doReturn( true ).when( metadata ).containsKey( RepositoryFile.SCHEDULABLE_KEY );
    doReturn( "true" ).when( metadata ).get( RepositoryFile.SCHEDULABLE_KEY );

    boolean canSchedule = schedulerService.isScheduleAllowed( nullable( String.class ) );

    assertTrue( canSchedule );

    // Test 2
    doReturn( false ).when( schedulerService ).isScheduleAllowed();

    canSchedule = schedulerService.isScheduleAllowed( nullable( String.class ) );

    assertFalse( canSchedule );

    // Test 3
    doReturn( true ).when( schedulerService ).isScheduleAllowed();
    doReturn( false ).when( metadata ).containsKey( RepositoryFile.SCHEDULABLE_KEY );

    canSchedule = schedulerService.isScheduleAllowed( nullable( String.class ) );

    assertTrue( canSchedule );

    // Test 4
    doReturn( true ).when( metadata ).containsKey( RepositoryFile.SCHEDULABLE_KEY );
    doReturn( "false" ).when( metadata ).get( RepositoryFile.SCHEDULABLE_KEY );

    canSchedule = schedulerService.isScheduleAllowed( nullable( String.class ) );

    assertFalse( canSchedule );

    verify( schedulerService, times( 4 ) ).isScheduleAllowed();
    verify( schedulerService.repository, times( 3 ) ).getFileMetadata( nullable( String.class ) );
    verify( metadata, times( 3 ) ).containsKey( RepositoryFile.SCHEDULABLE_KEY );
    verify( metadata, times( 2 ) ).get( RepositoryFile.SCHEDULABLE_KEY );
  }

  @Test
  public void testGetBlockoutJobs() {

    List<Job> jobs = new ArrayList<>();

    doReturn( jobs ).when( schedulerService.blockoutManager ).getBlockOutJobs();

    List<Job> returnJobs = schedulerService.getBlockOutJobs();

    assertEquals( returnJobs, jobs );

    verify( schedulerService.blockoutManager ).getBlockOutJobs();
  }

  @Test
  public void testHasBlockouts() {

    List<Job> jobs = new ArrayList<>();

    doReturn( jobs ).when( schedulerService.blockoutManager ).getBlockOutJobs();

    // Test 1
    boolean hasBlockouts = schedulerService.hasBlockouts();

    assertFalse( hasBlockouts );

    // Test 2
    jobs.add( mock( Job.class ) );
    hasBlockouts = schedulerService.hasBlockouts();

    assertTrue( hasBlockouts );

    verify( schedulerService.blockoutManager, times( 2 ) ).getBlockOutJobs();
  }

  @Test
  public void testAddBlockout() throws Exception {

    JobScheduleRequest jobScheduleRequest = mock( JobScheduleRequest.class );
    Job jobMock = mock( Job.class );

    JobScheduleParam jobScheduleParamMock1 = mock( JobScheduleParam.class );
    JobScheduleParam jobScheduleParamMock2 = mock( JobScheduleParam.class );

    List<JobScheduleParam> jobScheduleParams = new ArrayList<>();

    doReturn( true ).when( schedulerService ).canAdminister();
    doNothing().when( jobScheduleRequest ).setActionClass( nullable( String.class ) );
    doReturn( jobScheduleParams ).when( jobScheduleRequest ).getJobParameters();
    doReturn( jobScheduleParamMock1 ).when( schedulerService ).getJobScheduleParam( nullable( String.class ), nullable( String.class ) );
    doReturn( jobScheduleParamMock2 ).when( schedulerService ).getJobScheduleParam( nullable( String.class ), anyLong() );
    doReturn( jobMock ).when( schedulerService ).createJob( any( JobScheduleRequest.class ) );

    Job job = schedulerService.addBlockout( jobScheduleRequest );

    assertNotNull( job );
    assertEquals( 2, jobScheduleParams.size() );

    verify( schedulerService ).canAdminister();
    verify( jobScheduleRequest ).setActionClass( nullable( String.class ) );
    verify( jobScheduleRequest, times( 2 ) ).getJobParameters();
    verify( schedulerService ).createJob( any( JobScheduleRequest.class ) );
  }

  @Test
  public void testAddBlockoutException() throws Exception {

    // Test 1
    JobScheduleRequest jobScheduleRequest = mock( JobScheduleRequest.class );
    doReturn( false ).when( schedulerService ).canAdminister();

    try {
      schedulerService.addBlockout( jobScheduleRequest );
      fail();
    } catch ( IllegalAccessException e ) {
      //Should catch exception
    }

    // Test 2
    Job jobMock = mock( Job.class );

    JobScheduleParam jobScheduleParamMock1 = mock( JobScheduleParam.class );
    JobScheduleParam jobScheduleParamMock2 = mock( JobScheduleParam.class );

    List<JobScheduleParam> jobScheduleParams = new ArrayList<>();

    doReturn( true ).when( schedulerService ).canAdminister();
    doNothing().when( jobScheduleRequest ).setActionClass( nullable( String.class ) );
    doReturn( jobScheduleParams ).when( jobScheduleRequest ).getJobParameters();
    doReturn( jobScheduleParamMock1 ).when( schedulerService ).getJobScheduleParam( nullable( String.class ), nullable( String.class ) );
    doReturn( jobScheduleParamMock2 ).when( schedulerService ).getJobScheduleParam( nullable( String.class ), anyLong() );

    doThrow( new IOException() ).when( schedulerService ).createJob( jobScheduleRequest );

    try {
      schedulerService.addBlockout( jobScheduleRequest );
      fail();
    } catch ( IOException e ) {
      //Should catch exception
    }

    // Test 3
    doThrow( new SchedulerException( "" ) ).when( schedulerService ).createJob( jobScheduleRequest );

    try {
      schedulerService.addBlockout( jobScheduleRequest );
      fail();
    } catch ( SchedulerException e ) {
      //Should catch exception
    }

    verify( schedulerService, times( 3 ) ).canAdminister();
    verify( jobScheduleRequest, times( 2 ) ).setActionClass( nullable( String.class ) );
    verify( jobScheduleRequest, times( 4 ) ).getJobParameters();
    verify( schedulerService, times( 2 ) ).createJob( any( JobScheduleRequest.class ) );
  }

  @Test
  public void testUpdateBlockout() throws Exception {

    String jobId = "jobId";
    JobScheduleRequest jobScheduleRequest = mock( JobScheduleRequest.class );
    Job jobMock = mock( Job.class );

    doReturn( true ).when( schedulerService ).canAdminister();
    doReturn( true ).when( schedulerService ).removeJob( nullable( String.class ) );
    doReturn( jobMock ).when( schedulerService ).addBlockout( jobScheduleRequest );

    Job job = schedulerService.updateBlockout( jobId, jobScheduleRequest );

    assertNotNull( job );

    verify( schedulerService ).canAdminister();
    verify( schedulerService ).removeJob( nullable( String.class ) );
    verify( schedulerService ).addBlockout( jobScheduleRequest );
  }

  @Test
  public void testUpdateBlockoutException() throws Exception {

    String jobId = "jobId";
    JobScheduleRequest jobScheduleRequest = mock( JobScheduleRequest.class );
    Job job = mock( Job.class );

    // Test 1
    doReturn( false ).when( schedulerService ).canAdminister();

    try {
      schedulerService.updateBlockout( jobId, jobScheduleRequest );
      fail();
    } catch ( IllegalAccessException e ) {
      // Should catch the exception
    }

    // Test 2
    doReturn( true ).when( schedulerService ).canAdminister();
    doThrow( new SchedulerException( "" ) ).when( schedulerService ).removeJob( nullable( String.class ) );

    try {
      schedulerService.updateBlockout( jobId, jobScheduleRequest );
      fail();
    } catch ( SchedulerException e ) {
      // Should catch the exception
    }

    // Test 3
    doReturn( false ).when( schedulerService ).removeJob( nullable( String.class ) );

    try {
      schedulerService.updateBlockout( jobId, jobScheduleRequest );
      fail();
    } catch ( IllegalAccessException e ) {
      // Should catch the exception
    }

    // Test 4
    doReturn( true ).when( schedulerService ).removeJob( nullable( String.class ) );
    doThrow( new IOException() ).when( schedulerService ).addBlockout( jobScheduleRequest );

    try {
      schedulerService.updateBlockout( jobId, jobScheduleRequest );
      fail();
    } catch ( IOException e ) {
      // Should catch the exception
    }

    // Test 5
    doThrow( new SchedulerException( "" ) ).when( schedulerService ).addBlockout( jobScheduleRequest );

    try {
      schedulerService.updateBlockout( jobId, jobScheduleRequest );
      fail();
    } catch ( SchedulerException e ) {
      // Should catch the exception
    }

    verify( schedulerService, times( 5 ) ).canAdminister();
    verify( schedulerService, times( 4 ) ).removeJob( nullable( String.class ) );
    verify( schedulerService, times( 2 ) ).addBlockout( jobScheduleRequest );
  }

  @Test
  public void testWillFire() {

    IJobTrigger jobTrigger = mock( IJobTrigger.class );

    // Test 1
    doReturn( true ).when( schedulerService.blockoutManager ).willFire( jobTrigger );

    boolean willFire = schedulerService.willFire( jobTrigger );

    assertTrue( willFire );

    // Test 2
    doReturn( false ).when( schedulerService.blockoutManager ).willFire( jobTrigger );

    willFire = schedulerService.willFire( jobTrigger );

    assertFalse( willFire );

    verify( schedulerService.blockoutManager, times( 2 ) ).willFire( jobTrigger );
  }

  @Test
  public void testShouldFireNow() {

    // Test 1
    doReturn( true ).when( schedulerService.blockoutManager ).shouldFireNow();

    boolean shouldFireNow = schedulerService.shouldFireNow();

    assertTrue( shouldFireNow );

    // Test 2
    doReturn( false ).when( schedulerService.blockoutManager ).shouldFireNow();

    shouldFireNow = schedulerService.shouldFireNow();

    assertFalse( shouldFireNow );

    verify( schedulerService.blockoutManager, times( 2 ) ).shouldFireNow();

  }

  @Test
  public void testGetBlockStatus() throws Exception {

    JobScheduleRequest jobScheduleRequestMock = mock( JobScheduleRequest.class );
    BlockStatusProxy blockStatusProxyMock = mock( BlockStatusProxy.class );
    IJobTrigger jobTrigger = mock( IJobTrigger.class );

    doReturn( jobTrigger ).when( schedulerService ).convertScheduleRequestToJobTrigger( jobScheduleRequestMock );
    doReturn( true ).when( schedulerService.blockoutManager ).isPartiallyBlocked( jobTrigger );
    doReturn( true ).when( schedulerService.blockoutManager ).willFire( jobTrigger );
    doReturn( blockStatusProxyMock ).when( schedulerService ).getBlockStatusProxy( anyBoolean(), anyBoolean() );

    // Test 1
    BlockStatusProxy blockStatusProxy = schedulerService.getBlockStatus( jobScheduleRequestMock );

    assertNotNull( blockStatusProxy );

    // Test 2
    doReturn( false ).when( schedulerService.blockoutManager ).isPartiallyBlocked( jobTrigger );

    blockStatusProxy = schedulerService.getBlockStatus( jobScheduleRequestMock );

    assertNotNull( blockStatusProxy );

    verify( schedulerService, times( 2 ) ).convertScheduleRequestToJobTrigger( jobScheduleRequestMock );
    verify( schedulerService.blockoutManager, times( 2 ) ).isPartiallyBlocked( jobTrigger );
    verify( schedulerService, times( 2 ) ).getBlockStatusProxy( anyBoolean(), anyBoolean() );
    verify( schedulerService.blockoutManager, times( 1 ) ).willFire( jobTrigger );
  }

  @Test
  public void testGetBlockStatusException() throws Exception {

    JobScheduleRequest jobScheduleRequestMock = mock( JobScheduleRequest.class );

    doThrow( new SchedulerException( "" ) ).when( schedulerService ).convertScheduleRequestToJobTrigger( jobScheduleRequestMock );

    try {
      schedulerService.getBlockStatus( jobScheduleRequestMock );
      fail();
    } catch ( SchedulerException e ) {
      // Should catch the exception
    }

    verify( schedulerService ).convertScheduleRequestToJobTrigger( jobScheduleRequestMock );
  }
}
