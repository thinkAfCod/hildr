/*
 * Copyright 2023 q315xia@163.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.optimism.proposer.config;

import org.web3j.protocol.Web3j;

/**
 * The proposer config.
 *
 * @param l1EthRpc The HTTP URL for L1.
 * @param rollupRpc The HTTP URL for the rollup node.
 * @param l2OutputOracleAddr The L2OutputOracle contract address.
 * @param pollInterval The delay between querying L2 for more transaction and creating a new batch.
 * @param networkTimeout
 * @param allowNonFinalized set to true to propose outputs for L2 blocks derived from non-finalized
 *     L1 data
 * @author thinkAfCod
 * @since 0.1.1
 */
public record Config(
    String l1EthRpc,
    String l2EthRpc,
    String rollupRpc,
    Long l2ChainId,
    String l2Signer,
    String l2OutputOracleAddr,
    Long pollInterval,
    Long networkTimeout,
    Web3j l1Client,
    Web3j rollUpClient,
    Boolean allowNonFinalized) {

  //
  //
  //	// PollInterval is the delay between querying L2 for more transaction
  //	// and creating a new batch.
  //	PollInterval time.Duration
  //
  //	// AllowNonFinalized can be set to true to propose outputs
  //	// for L2 blocks derived from non-finalized L1 data.
  //	AllowNonFinalized bool
}
