/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.core.client.mapreduce.lib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.mock.MockInstance;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * 
 */
public class ConfiguratorBaseTest {

  private static enum PrivateTestingEnum {
    SOMETHING, SOMETHING_ELSE
  }

  @Test
  public void testEnumToConfKey() {
    assertEquals(this.getClass().getSimpleName() + ".PrivateTestingEnum.Something",
        ConfiguratorBase.enumToConfKey(this.getClass(), PrivateTestingEnum.SOMETHING));
    assertEquals(this.getClass().getSimpleName() + ".PrivateTestingEnum.SomethingElse",
        ConfiguratorBase.enumToConfKey(this.getClass(), PrivateTestingEnum.SOMETHING_ELSE));
  }

  @Test
  public void testSetConnectorInfoClassOfQConfigurationStringAuthenticationToken() throws AccumuloSecurityException {
    Configuration conf = new Configuration();
    assertFalse(ConfiguratorBase.isConnectorInfoSet(this.getClass(), conf));
    ConfiguratorBase.setConnectorInfo(this.getClass(), conf, "testUser", new PasswordToken("testPassword"));
    assertTrue(ConfiguratorBase.isConnectorInfoSet(this.getClass(), conf));
    assertEquals("testUser", ConfiguratorBase.getPrincipal(this.getClass(), conf));
    AuthenticationToken token = ConfiguratorBase.getAuthenticationToken(this.getClass(), conf);
    assertEquals(PasswordToken.class, token.getClass());
    assertEquals(new PasswordToken("testPassword"), token);
    assertEquals(
        "inline:" + PasswordToken.class.getName() + ":" + Base64.encodeBase64String(AuthenticationTokenSerializer.serialize(new PasswordToken("testPassword"))),
        conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.ConnectorInfo.TOKEN)));
  }

  @Test
  public void testSetConnectorInfoClassOfQConfigurationStringString() throws AccumuloSecurityException {
    Configuration conf = new Configuration();
    assertFalse(ConfiguratorBase.isConnectorInfoSet(this.getClass(), conf));
    ConfiguratorBase.setConnectorInfo(this.getClass(), conf, "testUser", "testFile");
    assertTrue(ConfiguratorBase.isConnectorInfoSet(this.getClass(), conf));
    assertEquals("testUser", ConfiguratorBase.getPrincipal(this.getClass(), conf));
    assertEquals("file:testFile", conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.ConnectorInfo.TOKEN)));
  }

  @Test
  public void testSetZooKeeperInstance() {
    Configuration conf = new Configuration();
    ConfiguratorBase.setZooKeeperInstance(this.getClass(), conf, "testInstanceName", "testZooKeepers");
    assertEquals("testInstanceName", conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.NAME)));
    assertEquals("testZooKeepers", conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.ZOO_KEEPERS)));
    assertEquals(ZooKeeperInstance.class.getSimpleName(), conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.TYPE)));
    // TODO uncomment this after ACCUMULO-1699
    // Instance instance = ConfiguratorBase.getInstance(this.getClass(), conf);
    // assertEquals(ZooKeeperInstance.class.getName(), instance.getClass().getName());
  }

  @Test
  public void testSetMockInstance() {
    Configuration conf = new Configuration();
    ConfiguratorBase.setMockInstance(this.getClass(), conf, "testInstanceName");
    assertEquals("testInstanceName", conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.NAME)));
    assertEquals(null, conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.ZOO_KEEPERS)));
    assertEquals(MockInstance.class.getSimpleName(), conf.get(ConfiguratorBase.enumToConfKey(this.getClass(), ConfiguratorBase.InstanceOpts.TYPE)));
    Instance instance = ConfiguratorBase.getInstance(this.getClass(), conf);
    assertEquals(MockInstance.class.getName(), instance.getClass().getName());
  }

  @Test
  public void testSetLogLevel() {
    Configuration conf = new Configuration();
    Level currentLevel = Logger.getLogger(this.getClass()).getLevel();

    ConfiguratorBase.setLogLevel(this.getClass(), conf, Level.DEBUG);
    Logger.getLogger(this.getClass()).setLevel(currentLevel);
    assertEquals(Level.DEBUG, ConfiguratorBase.getLogLevel(this.getClass(), conf));

    ConfiguratorBase.setLogLevel(this.getClass(), conf, Level.INFO);
    Logger.getLogger(this.getClass()).setLevel(currentLevel);
    assertEquals(Level.INFO, ConfiguratorBase.getLogLevel(this.getClass(), conf));

    ConfiguratorBase.setLogLevel(this.getClass(), conf, Level.FATAL);
    Logger.getLogger(this.getClass()).setLevel(currentLevel);
    assertEquals(Level.FATAL, ConfiguratorBase.getLogLevel(this.getClass(), conf));
  }

}
