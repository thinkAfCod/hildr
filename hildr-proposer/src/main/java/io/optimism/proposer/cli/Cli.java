package io.optimism.proposer.cli;

import io.optimism.proposer.L2OutputSubmitter;
import io.optimism.proposer.config.Config;
import io.optimism.utilities.telemetry.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

/**
 * @author thinkAfCod
 * @since 0.1.1
 */
public class Cli implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Cli.class);

  private boolean enableMetrics;

  // const EnvVarPrefix = "OP_PROPOSER"
  //
  // func prefixEnvVars(name string) []string {
  //	return opservice.PrefixEnvVar(EnvVarPrefix, name)
  // }

  // txmgr flag
  // prefixEnvVars := func(name string) []string {
  //		return opservice.PrefixEnvVar(envPrefix, name)
  //	}
  //	return append([]cli.Flag{
  //		&cli.StringFlag{
  //			Name:    MnemonicFlagName,
  //			Usage:   "The mnemonic used to derive the wallets for either the service",
  //			EnvVars: prefixEnvVars("MNEMONIC"),
  //		},
  //		&cli.StringFlag{
  //			Name:    HDPathFlagName,
  //			Usage:   "The HD path used to derive the sequencer wallet from the mnemonic. The mnemonic
  // flag must also be set.",
  //			EnvVars: prefixEnvVars("HD_PATH"),
  //		},
  //		&cli.StringFlag{
  //			Name:    PrivateKeyFlagName,
  //			Usage:   "The private key to use with the service. Must not be used with mnemonic.",
  //			EnvVars: prefixEnvVars("PRIVATE_KEY"),
  //		},
  //		&cli.Uint64Flag{
  //			Name:    NumConfirmationsFlagName,
  //			Usage:   "Number of confirmations which we will wait after sending a transaction",
  //			Value:   defaults.NumConfirmations,
  //			EnvVars: prefixEnvVars("NUM_CONFIRMATIONS"),
  //		},
  //		&cli.Uint64Flag{
  //			Name:    SafeAbortNonceTooLowCountFlagName,
  //			Usage:   "Number of ErrNonceTooLow observations required to give up on a tx at a particular
  // nonce without receiving confirmation",
  //			Value:   defaults.SafeAbortNonceTooLowCount,
  //			EnvVars: prefixEnvVars("SAFE_ABORT_NONCE_TOO_LOW_COUNT"),
  //		},
  //		&cli.DurationFlag{
  //			Name:    ResubmissionTimeoutFlagName,
  //			Usage:   "Duration we will wait before resubmitting a transaction to L1",
  //			Value:   defaults.ResubmissionTimeout,
  //			EnvVars: prefixEnvVars("RESUBMISSION_TIMEOUT"),
  //		},
  //		&cli.DurationFlag{
  //			Name:    NetworkTimeoutFlagName,
  //			Usage:   "Timeout for all network operations",
  //			Value:   defaults.NetworkTimeout,
  //			EnvVars: prefixEnvVars("NETWORK_TIMEOUT"),
  //		},
  //		&cli.DurationFlag{
  //			Name:    TxSendTimeoutFlagName,
  //			Usage:   "Timeout for sending transactions. If 0 it is disabled.",
  //			Value:   defaults.TxSendTimeout,
  //			EnvVars: prefixEnvVars("TXMGR_TX_SEND_TIMEOUT"),
  //		},
  //		&cli.DurationFlag{
  //			Name:    TxNotInMempoolTimeoutFlagName,
  //			Usage:   "Timeout for aborting a tx send if the tx does not make it to the mempool.",
  //			Value:   defaults.TxNotInMempoolTimeout,
  //			EnvVars: prefixEnvVars("TXMGR_TX_NOT_IN_MEMPOOL_TIMEOUT"),
  //		},
  //		&cli.DurationFlag{
  //			Name:    ReceiptQueryIntervalFlagName,
  //			Usage:   "Frequency to poll for receipts",
  //			Value:   defaults.ReceiptQueryInterval,
  //			EnvVars: prefixEnvVars("TXMGR_RECEIPT_QUERY_INTERVAL"),
  //		},
  //	}, opsigner.CLIFlags(envPrefix)...)
  //
  // txmgr opsigner flag
  // envPrefix += "_SIGNER"
  //	flags := []cli.Flag{
  //		&cli.StringFlag{
  //			Name:    EndpointFlagName,
  //			Usage:   "Signer endpoint the client will connect to",
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "ENDPOINT"),
  //		},
  //		&cli.StringFlag{
  //			Name:    AddressFlagName,
  //			Usage:   "Address the signer is signing transactions for",
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "ADDRESS"),
  //		},
  //	}
  //	flags = append(flags, optls.CLIFlagsWithFlagPrefix(envPrefix, "signer")...)
  //
  // prefixFunc := func(flagName string) string {
  //		return strings.Trim(fmt.Sprintf("%s.%s", flagPrefix, flagName), ".")
  //	}
  //	prefixEnvVars := func(name string) []string {
  //		return opservice.PrefixEnvVar(envPrefix, name)
  //	}
  //	return []cli.Flag{
  //		&cli.StringFlag{
  //			Name:    prefixFunc(TLSCaCertFlagName),
  //			Usage:   "tls ca cert path",
  //			Value:   defaultTLSCaCert,
  //			EnvVars: prefixEnvVars("TLS_CA"),
  //		},
  //		&cli.StringFlag{
  //			Name:    prefixFunc(TLSCertFlagName),
  //			Usage:   "tls cert path",
  //			Value:   defaultTLSCert,
  //			EnvVars: prefixEnvVars("TLS_CERT"),
  //		},
  //		&cli.StringFlag{
  //			Name:    prefixFunc(TLSKeyFlagName),
  //			Usage:   "tls key",
  //			Value:   defaultTLSKey,
  //			EnvVars: prefixEnvVars("TLS_KEY"),
  //		},
  //	}

  // pprof flag
  // return []cli.Flag{
  //		&cli.BoolFlag{
  //			Name:    EnabledFlagName,
  //			Usage:   "Enable the pprof server",
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "PPROF_ENABLED"),
  //		},
  //		&cli.StringFlag{
  //			Name:    ListenAddrFlagName,
  //			Usage:   "pprof listening address",
  //			Value:   defaultListenAddr, // TODO(CLI-4159): Switch to 127.0.0.1
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "PPROF_ADDR"),
  //		},
  //		&cli.IntFlag{
  //			Name:    PortFlagName,
  //			Usage:   "pprof listening port",
  //			Value:   defaultListenPort,
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "PPROF_PORT"),
  //		},
  //	}

  // metrics flag
  // return []cli.Flag{
  //		&cli.BoolFlag{
  //			Name:    EnabledFlagName,
  //			Usage:   "Enable the metrics server",
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "METRICS_ENABLED"),
  //		},
  //		&cli.StringFlag{
  //			Name:    ListenAddrFlagName,
  //			Usage:   "Metrics listening address",
  //			Value:   defaultListenAddr, // TODO(CLI-4159): Switch to 127.0.0.1
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "METRICS_ADDR"),
  //		},
  //		&cli.IntFlag{
  //			Name:    PortFlagName,
  //			Usage:   "Metrics listening port",
  //			Value:   defaultListenPort,
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "METRICS_PORT"),
  //		},
  //	}

  // log flag
  //	return []cli.Flag{
  //		&cli.GenericFlag{
  //			Name:    LevelFlagName,
  //			Usage:   "The lowest log level that will be output",
  //			Value:   NewLvlFlagValue(log.LvlInfo),
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "LOG_LEVEL"),
  //		},
  //		&cli.GenericFlag{
  //			Name:    FormatFlagName,
  //			Usage:   "Format the log output. Supported formats: 'text', 'terminal', 'logfmt', 'json',
  // 'json-pretty',",
  //			Value:   NewFormatFlagValue(FormatText),
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "LOG_FORMAT"),
  //		},
  //		&cli.BoolFlag{
  //			Name:    ColorFlagName,
  //			Usage:   "Color the log output if in terminal mode",
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "LOG_COLOR"),
  //		},
  //	}

  // rpc flag
  //	return []cli.Flag{
  //		&cli.StringFlag{
  //			Name:    ListenAddrFlagName,
  //			Usage:   "rpc listening address",
  //			Value:   "0.0.0.0", // TODO(CLI-4159): Switch to 127.0.0.1
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "RPC_ADDR"),
  //		},
  //		&cli.IntFlag{
  //			Name:    PortFlagName,
  //			Usage:   "rpc listening port",
  //			Value:   8545,
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "RPC_PORT"),
  //		},
  //		&cli.BoolFlag{
  //			Name:    EnableAdminFlagName,
  //			Usage:   "Enable the admin API",
  //			EnvVars: opservice.PrefixEnvVar(envPrefix, "RPC_ENABLE_ADMIN"),
  //		},
  //	}

  //
  // var (
  //	// Required Flags
  //	L1EthRpcFlag = &cli.StringFlag{
  //		Name:    "l1-eth-rpc",
  //		Usage:   "HTTP provider URL for L1",
  //		EnvVars: prefixEnvVars("L1_ETH_RPC"),
  //	}
  //	RollupRpcFlag = &cli.StringFlag{
  //		Name:    "rollup-rpc",
  //		Usage:   "HTTP provider URL for the rollup node",
  //		EnvVars: prefixEnvVars("ROLLUP_RPC"),
  //	}
  //	L2OOAddressFlag = &cli.StringFlag{
  //		Name:    "l2oo-address",
  //		Usage:   "Address of the L2OutputOracle contract",
  //		EnvVars: prefixEnvVars("L2OO_ADDRESS"),
  //	}
  //
  //	// Optional flags
  //	PollIntervalFlag = &cli.DurationFlag{
  //		Name:    "poll-interval",
  //		Usage:   "How frequently to poll L2 for new blocks",
  //		Value:   6 * time.Second,
  //		EnvVars: prefixEnvVars("POLL_INTERVAL"),
  //	}
  //	AllowNonFinalizedFlag = &cli.BoolFlag{
  //		Name:    "allow-non-finalized",
  //		Usage:   "Allow the proposer to submit proposals for L2 blocks derived from non-finalized L1
  // blocks.",
  //		EnvVars: prefixEnvVars("ALLOW_NON_FINALIZED"),
  //	}
  //	// Legacy Flags
  //	L2OutputHDPathFlag = txmgr.L2OutputHDPathFlag
  // )
  //
  // var requiredFlags = []cli.Flag{
  //	L1EthRpcFlag,
  //	RollupRpcFlag,
  //	L2OOAddressFlag,
  // }
  //
  // var optionalFlags = []cli.Flag{
  //	PollIntervalFlag,
  //	AllowNonFinalizedFlag,
  //	L2OutputHDPathFlag,
  // }
  //
  // func init() {
  //	optionalFlags = append(optionalFlags, oprpc.CLIFlags(EnvVarPrefix)...)
  //	optionalFlags = append(optionalFlags, oplog.CLIFlags(EnvVarPrefix)...)
  //	optionalFlags = append(optionalFlags, opmetrics.CLIFlags(EnvVarPrefix)...)
  //	optionalFlags = append(optionalFlags, oppprof.CLIFlags(EnvVarPrefix)...)
  //	optionalFlags = append(optionalFlags, txmgr.CLIFlags(EnvVarPrefix)...)
  //
  //	Flags = append(requiredFlags, optionalFlags...)
  // }

  @Override
  public void run() {

    // listen close signal
    Signal.handle(new Signal("INT"), sig -> System.exit(0));
    Signal.handle(new Signal("TERM"), sig -> System.exit(0));

    var tracer = Logging.INSTANCE.getTracer("hildr-batcher-cli");
    var span = tracer.nextSpan().name("batcher-submitter").start();
    try (var unused = tracer.withSpan(span)) {
      // todo start metrics server

      // start l2 output submitter
      var submitter = new L2OutputSubmitter(optionToConfig());
      submitter.startAsync().awaitTerminated();
    } catch (Exception e) {
      LOGGER.error("hildr batcher: ", e);
      throw new RuntimeException(e);
    } finally {
      if (this.enableMetrics) {
        LOGGER.info("stop metrics");
      }
      span.end();
    }
  }

  private Config optionToConfig() {
    return new Config(null, null, null, null, null, null, null, null);
  }
}
