package org.apache.seata.server.cluster.raft;

import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RaftOptions;
import com.alipay.sofa.jraft.rpc.CliClientService;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.XID;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.registry.FileRegistryServiceImpl;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.seata.discovery.registry.namingserver.NamingserverRegistryServiceImpl;
import org.apache.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_APPLY_BATCH;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_DISRUPTOR_BUFFER_SIZE;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_ELECTION_TIMEOUT_MS;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_MAX_APPEND_BUFFER_SIZE;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_MAX_REPLICATOR_INFLIGHT_MSGS;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_PORT_CAMEL;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SNAPSHOT_INTERVAL;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SYNC;
import static org.apache.seata.common.DefaultValues.DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS;

/**
 * this is base abstract class for all raft server managers
 */
public abstract class AbstractRaftServerManager implements RaftServerManager<RaftServer> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<String /*group*/, RaftServer /*raft-group-cluster*/> RAFT_SERVER_MAP =
            new ConcurrentHashMap<>();
    protected static final AtomicBoolean init = new AtomicBoolean(false);
    protected static final AtomicBoolean rpcStarted = new AtomicBoolean(false);

    protected static final org.apache.seata.config.Configuration CONFIG = ConfigurationFactory.getInstance();
    protected static volatile boolean RAFT_MODE;
    protected static volatile RpcServer rpcServer;
    protected static volatile PeerId serverId;

    public void init(String initConfStr) {
        if (init.compareAndSet(false, true)) {
            RAFT_MODE = StoreConfig.getSessionMode().equals(SessionMode.RAFT);
            if (StringUtils.isBlank(initConfStr)) {
                if (RAFT_MODE) {
                    throw new IllegalArgumentException(
                            "Raft store mode must config: " + ConfigurationKeys.SERVER_RAFT_SERVER_ADDR);
                }
                return;
            } else {
                if (RAFT_MODE) {
                    for (RegistryService<?> instance : MultiRegistryFactory.getInstances()) {
                        if (!(instance instanceof FileRegistryServiceImpl)
                                && !(instance instanceof NamingserverRegistryServiceImpl)) {
                            throw new IllegalArgumentException("Raft store mode not support other Registration Center");
                        }
                    }
                }
                logger.warn("raft mode and raft cluster is an experimental feature");
            }
            final Configuration initConf = new Configuration();
            if (!initConf.parse(initConfStr)) {
                throw new IllegalArgumentException("fail to parse initConf:" + initConfStr);
            }
            if (serverId == null) {
                int port = Integer.parseInt(System.getProperty(SERVER_RAFT_PORT_CAMEL, "0"));
                String host = XID.getIpAddress();
                if (port <= 0) {
                    // Highly available deployments require different nodes
                    for (PeerId peer : initConf.getPeers()) {
                        List<String> peerIps = NetUtil.getHostByName(peer.getIp());
                        for (String peerIp : peerIps) {
                            if (StringUtils.equals(peerIp, host)) {
                                if (serverId != null) {
                                    throw new IllegalArgumentException(
                                            "server.raft.cluster has duplicate ip, For local debugging, use -Dserver.raftPort to specify the raft port");
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
            }
            // Here you have raft RPC and business RPC using the same RPC server, and you can usually do this
            // separately
            if (rpcServer == null) {
                rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
            }
        }
    }

    public boolean isLeader(String group) {
        AtomicReference<RaftStateMachine> stateMachine = new AtomicReference<>();
        Optional.ofNullable(RAFT_SERVER_MAP.get(group)).ifPresent(raftServer -> {
            stateMachine.set(raftServer.getRaftStateMachine());
        });
        RaftStateMachine raftStateMachine = stateMachine.get();
        return !isRaftMode() && RAFT_SERVER_MAP.isEmpty() || (raftStateMachine != null && raftStateMachine.isLeader());
    }

    public static boolean isRaftMode() {
        return RAFT_MODE;
    }

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

    protected NodeOptions initNodeOptions(Configuration initConf) {
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

    public void destroy() {
        RAFT_SERVER_MAP.forEach((group, raftServer) -> {
            try {
                raftServer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info("closed seata server raft cluster, group: {} ", group);
        });
        Optional.ofNullable(rpcServer).ifPresent(RpcServer::shutdown);
        RAFT_SERVER_MAP.clear();
        rpcServer = null;
        RAFT_MODE = false;
        init.set(false);
    }

    public CliService getCliServiceInstance() {
        return SingletonHandler.CLI_SERVICE;
    }

    public CliClientService getCliClientServiceInstance() {
        return SingletonHandler.CLI_CLIENT_SERVICE;
    }

    public AtomicBoolean getInit() {
        return init;
    }

    private static class SingletonHandler {
        private static final CliService CLI_SERVICE = RaftServiceFactory.createAndInitCliService(new CliOptions());
        private static final CliClientService CLI_CLIENT_SERVICE = new CliClientServiceImpl();

        static {
            CLI_CLIENT_SERVICE.init(new CliOptions());
        }
    }
}
