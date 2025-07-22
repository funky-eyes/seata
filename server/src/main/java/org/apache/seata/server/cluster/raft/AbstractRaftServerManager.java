package org.apache.seata.server.cluster.raft;


import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.rpc.CliClientService;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


import static java.io.File.separator;
import static org.apache.seata.common.ConfigurationKeys.*;
import static org.apache.seata.common.DefaultValues.DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static org.apache.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;

/**
 * Abstract base class for Raft server managers
 */
public abstract class AbstractRaftServerManager<T> implements RaftInit {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRaftServerManager.class);
    protected static final org.apache.seata.config.Configuration CONFIG = ConfigurationFactory.getInstance();

    protected final AtomicBoolean initialized = new AtomicBoolean(false);

    // Singleton CLI services
    private static final CliService CLI_SERVICE = RaftServiceFactory.createAndInitCliService(new CliOptions());
    private static final CliClientService CLI_CLIENT_SERVICE;

    static {
        CLI_CLIENT_SERVICE = new CliClientServiceImpl();
        CLI_CLIENT_SERVICE.init(new CliOptions());
    }

    /**
     * Get the configuration for this specific Raft server type
     */
    protected abstract RaftServerConfig getRaftServerConfig();

    /**
     * Create the specific Raft server instance
     */
    protected abstract T createRaftServer(String dataPath, String group, PeerId serverId,
                                          NodeOptions nodeOptions, RpcServer rpcServer) throws IOException;

    /**
     * Store the created Raft server in the appropriate map
     */
    protected abstract void storeRaftServer(String group, T raftServer);

    /**
     * Check if the mode is enabled for this server type
     */
    protected abstract boolean isModeEnabled();

    /**
     * Get warning message for experimental features
     */
    protected abstract String getExperimentalWarningMessage();

    /**
     * Validate mode-specific requirements
     */
    protected abstract void validateModeRequirements();

    @Override
    public void init() {
        if (initialized.compareAndSet(false, true)) {
            try {
                RaftServerConfig config = getRaftServerConfig();
                String initConfStr = CONFIG.getConfig(config.getConfigKey());

                if (StringUtils.isBlank(initConfStr)) {
                    if (isModeEnabled()) {
                        throw new IllegalArgumentException(
                                "Mode enabled but missing config: " + config.getConfigKey());
                    }
                    return;
                } else {
                    if (isModeEnabled()) {
                        validateModeRequirements();
                    }
                    LOGGER.warn(getExperimentalWarningMessage());
                }

                final Configuration initConf = new Configuration();
                if (!initConf.parse(initConfStr)) {
                    throw new IllegalArgumentException("fail to parse initConf:" + initConfStr);
                }

                PeerId serverId = resolveServerId(initConf, config);
                String dataPath = buildDataPath(serverId, config);
                String group = CONFIG.getConfig(config.getGroupConfigKey(), config.getDefaultGroup());

                // Use shared RPC server
                RpcServer rpcServer = SharedRpcServerManager.getOrCreateSharedRpcServer(serverId);
                T raftServer = createRaftServer(dataPath, group, serverId, initNodeOptions(initConf), rpcServer);

                storeRaftServer(group, raftServer);

            } catch (IOException e) {
                initialized.set(false);
                throw new IllegalArgumentException("fail init raft cluster:" + e.getMessage(), e);
            }
        }
    }

    /**
     * Resolve the server ID based on configuration and available peers
     */
    private PeerId resolveServerId(Configuration initConf, RaftServerConfig config) {
        int port = Integer.parseInt(System.getProperty(config.getPortProperty(), "0"));
        PeerId serverId = null;
        String host = XID.getIpAddress();

        if (port <= 0) {
            // Highly available deployments require different nodes
            for (PeerId peer : initConf.getPeers()) {
                List<String> peerIps = NetUtil.getHostByName(peer.getIp());
                for (String peerIp : peerIps) {
                    if (StringUtils.equals(peerIp, host)) {
                        if (serverId != null) {
                            throw new IllegalArgumentException(config.getDuplicateIpErrorMessage());
                        }
                        serverId = peer;
                        break;
                    }
                }
            }
        } else {
            // Local debugging use
            serverId = new PeerId(host, port);
        }

        if (serverId == null) {
            throw new IllegalArgumentException("Could not resolve server ID for host: " + host);
        }

        return serverId;
    }

    /**
     * Build the data path for the Raft server
     */
    private String buildDataPath(PeerId serverId, RaftServerConfig config) {
        return CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR)
                + separator + config.getDataPathSuffix() + separator + serverId.getPort();
    }

    /**
     * Initialize Raft options from configuration
     */
    protected static RaftOptions initRaftOptions() {
        RaftOptions raftOptions = new RaftOptions();
        raftOptions.setApplyBatch(CONFIG.getInt(SERVER_RAFT_APPLY_BATCH, raftOptions.getApplyBatch()));
        raftOptions.setMaxAppendBufferSize(
                CONFIG.getInt(SERVER_RAFT_MAX_APPEND_BUFFER_SIZE, raftOptions.getMaxAppendBufferSize()));
        raftOptions.setDisruptorBufferSize(
                CONFIG.getInt(SERVER_RAFT_DISRUPTOR_BUFFER_SIZE, raftOptions.getDisruptorBufferSize()));
        raftOptions.setMaxReplicatorInflightMsgs(
                CONFIG.getInt(SERVER_RAFT_MAX_REPLICATOR_INFLIGHT_MSGS, raftOptions.getMaxReplicatorInflightMsgs()));
        raftOptions.setSync(CONFIG.getBoolean(SERVER_RAFT_SYNC, raftOptions.isSync()));
        return raftOptions;
    }

    /**
     * Initialize node options from configuration
     */
    protected static NodeOptions initNodeOptions(Configuration initConf) {
        NodeOptions nodeOptions = new NodeOptions();
        // enable the CLI service.
        nodeOptions.setDisableCli(false);
        // snapshot should be made every 600 seconds
        int snapshotInterval = CONFIG.getInt(SERVER_RAFT_SNAPSHOT_INTERVAL, 60 * 10);
        nodeOptions.setSnapshotIntervalSecs(snapshotInterval);
        nodeOptions.setRaftOptions(initRaftOptions());
        // set the election timeout to 1 second
        nodeOptions.setElectionTimeoutMs(
                CONFIG.getInt(SERVER_RAFT_ELECTION_TIMEOUT_MS, DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS));
        // set up the initial cluster configuration
        nodeOptions.setInitialConf(initConf);
        return nodeOptions;
    }

    /**
     * Start the shared RPC server
     */
    protected static void startSharedRpcServer() {
        SharedRpcServerManager.initializeSharedRpcServer();
        SharedRpcServerManager.startSharedRpcServer();
    }

    /**
     * Shutdown the shared RPC server
     */
    protected static void destroySharedRpcServer() {
        SharedRpcServerManager.shutdownSharedRpcServer();
    }

    public static CliService getCliServiceInstance() {
        return CLI_SERVICE;
    }

    public static CliClientService getCliClientServiceInstance() {
        return CLI_CLIENT_SERVICE;
    }
}
