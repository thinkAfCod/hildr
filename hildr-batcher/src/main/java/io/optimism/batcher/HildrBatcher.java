package io.optimism.batcher;

import io.optimism.batcher.cli.Cli;
import picocli.CommandLine;

/**
 * Batcher main method.
 *
 * @author thinkAfCod
 * @since 0.1.1
 */
public class HildrBatcher {

    /** Constructor of HildrBatcher. */
    public HildrBatcher() {}

    /**
     * Main method of HildrBatcher.
     *
     * @param args Starts arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Cli()).execute(args);
        System.exit(exitCode);
    }
}
