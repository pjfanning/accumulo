/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.accumulo.manager.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.fate.AdminUtil;
import org.apache.accumulo.fate.ReadOnlyStore;
import org.apache.accumulo.fate.ZooStore;
import org.apache.accumulo.fate.zookeeper.ServiceLock;
import org.apache.accumulo.fate.zookeeper.ZooReaderWriter;
import org.apache.accumulo.manager.Manager;
import org.apache.accumulo.server.ServerContext;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * A utility to administer FATE operations
 */
public class FateAdmin {

  static class TxOpts {
    @Parameter(description = "<txid>...", required = true)
    List<String> txids = new ArrayList<>();
  }

  @Parameters(commandDescription = "Stop an existing FATE by transaction id")
  static class FailOpts extends TxOpts {}

  @Parameters(commandDescription = "Delete an existing FATE by transaction id")
  static class DeleteOpts extends TxOpts {}

  @Parameters(commandDescription = "List the existing FATE transactions")
  static class PrintOpts {}

  public static void main(String[] args) throws Exception {
    Help opts = new Help();
    JCommander jc = new JCommander(opts);
    jc.setProgramName(FateAdmin.class.getName());
    LinkedHashMap<String,TxOpts> txOpts = new LinkedHashMap<>(2);
    txOpts.put("fail", new FailOpts());
    txOpts.put("delete", new DeleteOpts());
    for (Entry<String,TxOpts> entry : txOpts.entrySet()) {
      jc.addCommand(entry.getKey(), entry.getValue());
    }
    jc.addCommand("print", new PrintOpts());
    jc.parse(args);
    if (opts.help || jc.getParsedCommand() == null) {
      jc.usage();
      System.exit(1);
    }

    System.err.printf("This tool has been deprecated%nFATE administration now"
        + " available within 'accumulo shell'%n$ fate fail <txid>... | delete"
        + " <txid>... | print [<txid>...]%n%n");

    AdminUtil<Manager> admin = new AdminUtil<>(true);

    try (var context = new ServerContext(SiteConfiguration.auto())) {
      final String zkRoot = context.getZooKeeperRoot();
      var zLockManagerPath = ServiceLock.path(zkRoot + Constants.ZMANAGER_LOCK);
      var zTableLocksPath = ServiceLock.path(zkRoot + Constants.ZTABLE_LOCKS);
      String path = zkRoot + Constants.ZFATE;
      ZooReaderWriter zk = context.getZooReaderWriter();
      ZooStore<Manager> zs = new ZooStore<>(path, zk);

      if (jc.getParsedCommand().equals("fail")) {
        for (String txid : txOpts.get(jc.getParsedCommand()).txids) {
          if (!admin.prepFail(zs, zk, zLockManagerPath, txid)) {
            System.exit(1);
          }
        }
      } else if (jc.getParsedCommand().equals("delete")) {
        for (String txid : txOpts.get(jc.getParsedCommand()).txids) {
          if (!admin.prepDelete(zs, zk, zLockManagerPath, txid)) {
            System.exit(1);
          }
          admin.deleteLocks(zk, zTableLocksPath, txid);
        }
      } else if (jc.getParsedCommand().equals("print")) {
        admin.print(new ReadOnlyStore<>(zs), zk, zTableLocksPath);
      }
    }
  }
}
