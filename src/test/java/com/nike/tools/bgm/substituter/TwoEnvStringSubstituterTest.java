package com.nike.tools.bgm.substituter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.tx.TwoEnvLoader;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TwoEnvStringSubstituterTest
{
  private static final Environment FAKE_LIVE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Environment FAKE_STAGE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(1);
  private static final Map<String, String> EXTRA = new HashMap<String, String>()
  {{
      put("%{extra}", "Extra Substitution Value");
    }};

  @InjectMocks
  private TwoEnvStringSubstituter twoEnvStringSubstituter = new TwoEnvStringSubstituter(FAKE_LIVE_ENV.getEnvName(),
      FAKE_STAGE_ENV.getEnvName(), EXTRA);

  @Mock
  private TwoEnvLoader mockTwoEnvLoader;

  @Before
  public void setUp()
  {
    when(mockTwoEnvLoader.getLiveApplicationVm()).thenReturn(FAKE_LIVE_ENV.getApplicationVms().get(0));
    when(mockTwoEnvLoader.getStageApplicationVm()).thenReturn(FAKE_STAGE_ENV.getApplicationVms().get(0));
    when(mockTwoEnvLoader.getLivePhysicalDatabase()).thenReturn(FAKE_LIVE_ENV.getLogicalDatabases().get(0).getPhysicalDatabase());
    when(mockTwoEnvLoader.getStagePhysicalDatabase()).thenReturn(FAKE_STAGE_ENV.getLogicalDatabases().get(0).getPhysicalDatabase());
  }

  /**
   * Substitution should support these TwoEnv %{..} variables: liveEnv, stageEnv, applicationVmMap, physicalDbMap.
   * And also support Extra.
   */
  @Test
  public void testSubstituteVariables()
  {
    String command = "run stuff with LIVE_ENV=%{liveEnv}; STAGE_ENV=%{stageEnv}; APPLICATION_VM_MAP=%{applicationVmMap}; PHYSICAL_DB_MAP=%{physicalDbMap}; EXTRA=%{extra}!";
    String result = twoEnvStringSubstituter.substituteVariables(command);
    assertTrue(result.contains("LIVE_ENV=" + FAKE_LIVE_ENV.getEnvName()));
    assertTrue(result.contains("STAGE_ENV=" + FAKE_STAGE_ENV.getEnvName()));
    ApplicationVm liveApplicationVm = FAKE_LIVE_ENV.getApplicationVms().get(0);
    ApplicationVm stageApplicationVm = FAKE_STAGE_ENV.getApplicationVms().get(0);
    assertTrue(Pattern.compile("APPLICATION_VM_MAP=" + liveApplicationVm.getHostname() + ".*" + stageApplicationVm.getHostname() + ".*;")
        .matcher(result).find());
    PhysicalDatabase livePhysicalDatabase = FAKE_LIVE_ENV.getLogicalDatabases().get(0).getPhysicalDatabase();
    PhysicalDatabase stagePhysicalDatabase = FAKE_STAGE_ENV.getLogicalDatabases().get(0).getPhysicalDatabase();
    assertTrue(Pattern.compile("PHYSICAL_DB_MAP=" + livePhysicalDatabase.getInstanceName() + ".*" + stagePhysicalDatabase.getInstanceName() + ".*;")
        .matcher(result).find());
    assertTrue(result.contains("EXTRA=Extra Substitution Value"));
  }

}