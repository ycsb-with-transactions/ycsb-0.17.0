/**
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.yahoo.ycsb.workloads;

import java.util.Properties;
import com.yahoo.ycsb.*;

/**
 * The core benchmark scenario. Represents a set of clients doing simple CRUD
 * operations. The relative proportion of different kinds of operations, and
 * other properties of the workload, are controlled by parameters specified at
 * runtime.
 * 
 */
public class ClosedEconomyWorkload extends Workload {
  /**
   * Initialize the scenario. Create any generators and other shared objects here.
   * Called once, in the main client thread, before any operations are started.
   */
  public void init(Properties p) throws WorkloadException {
      // TODO: implement me
  }
      
  /**
   * Cleanup the scenario. Called once, in the main client thread, after all operations have completed.
   */
  public void cleanup() throws WorkloadException {
  }

  /**
   * Do one insert operation. Because it will be called concurrently from multiple client threads, this
   * function must be thread safe. However, avoid synchronized, or the threads will block waiting for each
   * other, and it will be difficult to reach the target throughput. Ideally, this function would have no side
   * effects other than DB operations and mutations on threadstate. Mutations to threadstate do not need to be
   * synchronized, since each thread has its own threadstate instance.
   */
  public boolean doInsert(DB db, Object threadstate) {
      // TODO: implement me
  }

  /**
   * Do one delete operation. Because it will be called concurrently from
   * multiple client threads, this function must be thread safe. However,
   * avoid synchronized, or the threads will block waiting for each other, and
   * it will be difficult to reach the target throughput. Ideally, this
   * function would have no side effects other than DB operations.
   * 
   */
  public boolean doDelete(DB db, Object threadstate) {
      // TODO: implement me
  }

  /**
   * Do one transaction operation. Because it will be called concurrently from multiple client threads, this
   * function must be thread safe. However, avoid synchronized, or the threads will block waiting for each
   * other, and it will be difficult to reach the target throughput. Ideally, this function would have no side
   * effects other than DB operations and mutations on threadstate. Mutations to threadstate do not need to be
   * synchronized, since each thread has its own threadstate instance.
   * 
   * @return false if the workload knows it is done for this thread. Client will terminate the thread. 
   * Return true otherwise. Return true for workloads that rely on operationcount. For workloads that read
   * traces from a file, return true when there are more to do, false when you are done.
   */
  public boolean doTransaction(DB db, Object threadstate) {
      // TODO: implement me
  }

  public boolean doTransactionRead(DB db) {
      // TODO: implement me
  }

  public boolean doTransactionReadModifyWrite(DB db) {
      // TODO: implement me
  }

  public boolean doTransactionScan(DB db) {
      // TODO: implement me
  }

  public boolean doTransactionUpdate(DB db) {
      // TODO: implement me
  }

  public boolean doTransactionInsert(DB db) {
      // TODO: implement me
  }

  /**
   * Allows scheduling a request to stop the workload.
   */
  public void requestStop() {
      stopRequested.set(true);
  }

  /**
   * Check the status of the stop request flag.
   * @return true if stop was requested, false otherwise.
   */
  public boolean isStopRequested() {
      return stopRequested.get();
  }

  /**
   * Perform validation of the database db after the workload has executed.
   * 
   * @return false if the workload left the database in an inconsistent state, true if it is consistent.
   * @throws WorkloadException 
   */
  public boolean validate(DB db) throws WorkloadException {
      // TODO: implement me
  }
}