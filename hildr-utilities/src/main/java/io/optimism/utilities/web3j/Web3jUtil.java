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

package io.optimism.utilities.web3j;

import io.optimism.utilities.telemetry.TracerTaskWrapper;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jdk.incubator.concurrent.StructuredTaskScope;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

/**
 * @author thinkAfCod
 * @since 0.1.1
 */
public class Web3jUtil {

  public static BigInteger getTxCount(final Web3j client, final String fromAddr) {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      var countFuture =
          scope.fork(
              TracerTaskWrapper.wrap(
                  () -> {
                    var countReq =
                        client.ethGetTransactionCount(
                            fromAddr, DefaultBlockParameterName.LATEST);
                    return countReq.send().getTransactionCount();
                  }));
      scope.join();
      scope.throwIfFailed();
      return countFuture.resultNow();
    } catch (InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      throw new Web3jCallException("get tx count failed", e);
    }
  }

  public static EthGetTransactionReceipt getTxReceipt(final Web3j client, final String txHash) {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      var receiptFuture = scope.fork(() -> client.ethGetTransactionReceipt(txHash).send());
      scope.join();
      scope.throwIfFailed();
      return receiptFuture.resultNow();
    } catch (InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      throw new Web3jCallException("failed to get TxReceipt", e);
    }
  }

  public static EthBlock pollBlock(
      final Web3j client,
      final DefaultBlockParameter parameter,
      final boolean returnFullTransactionObjects) {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      var receiptFuture = scope.fork(
          () -> client.ethGetBlockByNumber(parameter, returnFullTransactionObjects).send());
      scope.join();
      scope.throwIfFailed();
      return receiptFuture.resultNow();
    } catch (InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      throw new Web3jCallException("failed to get block by number", e);
    }
  }

  public static List<Type> executeContract(
      final Web3j client, String fromAddr, String contractAddr, final Function function) {
    String fnData = FunctionEncoder.encode(function);
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      Future<String> fut = scope.fork(() -> {
        return client.ethCall(
            Transaction.createEthCallTransaction(fromAddr, contractAddr, fnData),
            DefaultBlockParameterName.LATEST).send().getValue();
      });
      scope.join();
      scope.throwIfFailed();
      return FunctionReturnDecoder.decode(fut.get(), function.getOutputParameters());
    } catch (InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      throw new Web3jCallException(e);
    }
  }

  public static EthGetTransactionReceipt executeContractReturnReceipt(
      final Web3j client,
      final RawTransaction tx,
      final long chainId,
      final Credentials credentials) {

    byte[] sign = TransactionEncoder.signMessage(tx, chainId, credentials);
    var signTxHexValue = Numeric.toHexString(sign);
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      Future<EthGetTransactionReceipt> fork = scope.fork(() -> {
        EthSendTransaction txResp = client.ethSendRawTransaction(signTxHexValue).send();
        if (txResp == null) {
          throw new Web3jCallException("call contract tx response is null");
        }
        if (txResp.hasError()) {
          throw new Web3jCallException(
              String.format("call contract tx has error: code = %d, msg = %s, data = %s",
                  txResp.getError().getCode(),
                  txResp.getError().getMessage(),
                  txResp.getError().getData()));
        }
        String txHashLocal = Hash.sha3(signTxHexValue);
        String txHashRemote = txResp.getTransactionHash();
        if (txHashLocal.equals(txHashRemote)) {
          throw new Web3jCallException(
              String.format("tx has mismatch: txHashLocal = %s, txHashRemote = %s",
                  txHashLocal, txHashRemote));
        }
        return Web3jUtil.getTxReceipt(client, txHashLocal);
      });
      scope.join();
      scope.throwIfFailed();
      return fork.get();
    } catch (InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      throw new Web3jCallException(e);
    }
  }


}
