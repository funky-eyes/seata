package org.apache.seata.server.cluster.raft;

import com.alipay.remoting.serialization.SerializerManager;
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
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.server.cluster.raft.processor.PutNodeInfoRequestProcessor;
import org.apache.seata.server.cluster.raft.serializer.JacksonBoltSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
/**
 * Shared RPC Server Manager - Singleton pattern for managing shared RPC server
 */
class SharedRpcServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedRpcServerManager.class);
    private static final AtomicReference<RpcServer> SHARED_RPC_SERVER = new AtomicReference<>();
    private static final AtomicBoolean RPC_SERVER_INITIALIZED = new AtomicBoolean(false);
    private static final AtomicBoolean RPC_SERVER_STARTED = new AtomicBoolean(false);

    /**
     * Get or create the shared RPC server instance
     */
    public static RpcServer getOrCreateSharedRpcServer(PeerId serverId) throws IOException {
        if (SHARED_RPC_SERVER.get() == null) {
            synchronized (SharedRpcServerManager.class) {
                if (SHARED_RPC_SERVER.get() == null) {
                    RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
                    SHARED_RPC_SERVER.set(rpcServer);
                    LOGGER.info("Created shared RPC server for endpoint: {}", serverId.getEndpoint());
                }
            }
        }
        return SHARED_RPC_SERVER.get();
    }

    /**
     * Initialize the shared RPC server with common processors and serializers
     */
    public static void initializeSharedRpcServer() {
        if (RPC_SERVER_INITIALIZED.compareAndSet(false, true)) {
            RpcServer rpcServer = SHARED_RPC_SERVER.get();
            if (rpcServer != null) {
                rpcServer.registerProcessor(new PutNodeInfoRequestProcessor());
                SerializerManager.addSerializer(SerializerType.JACKSON.getCode(), new JacksonBoltSerializer());
                LOGGER.info("Initialized shared RPC server with processors and serializers");
            }
        }
    }

    /**
     * Start the shared RPC server
     */
    public static void startSharedRpcServer() {
        if (RPC_SERVER_STARTED.compareAndSet(false, true)) {
            RpcServer rpcServer = SHARED_RPC_SERVER.get();
            if (rpcServer != null) {
                if (!rpcServer.init(null)) {
                    RPC_SERVER_STARTED.set(false);
                    throw new RuntimeException("Failed to start shared RPC server!");
                }
                LOGGER.info("Started shared RPC server");
            }
        }
    }

    /**
     * Shutdown the shared RPC server
     */
    public static void shutdownSharedRpcServer() {
        RpcServer rpcServer = SHARED_RPC_SERVER.getAndSet(null);
        if (rpcServer != null) {
            rpcServer.shutdown();
            RPC_SERVER_INITIALIZED.set(false);
            RPC_SERVER_STARTED.set(false);
            LOGGER.info("Shutdown shared RPC server");
        }
    }

    /**
     * Get the current shared RPC server instance (may be null)
     */
    public static RpcServer getSharedRpcServer() {
        return SHARED_RPC_SERVER.get();
    }
}
