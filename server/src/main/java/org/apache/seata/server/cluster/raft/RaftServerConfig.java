package org.apache.seata.server.cluster.raft;

/**
 * Configuration holder for Raft server initialization
 */
public class RaftServerConfig {
    private final String configKey;
    private final String portProperty;
    private final String groupConfigKey;
    private final String defaultGroup;
    private final String dataPathSuffix;
    private final String duplicateIpErrorMessage;

    public RaftServerConfig(String configKey, String portProperty, String groupConfigKey,
                            String defaultGroup, String dataPathSuffix, String duplicateIpErrorMessage) {
        this.configKey = configKey;
        this.portProperty = portProperty;
        this.groupConfigKey = groupConfigKey;
        this.defaultGroup = defaultGroup;
        this.dataPathSuffix = dataPathSuffix;
        this.duplicateIpErrorMessage = duplicateIpErrorMessage;
    }

    // Getters
    public String getConfigKey() { return configKey; }
    public String getPortProperty() { return portProperty; }
    public String getGroupConfigKey() { return groupConfigKey; }
    public String getDefaultGroup() { return defaultGroup; }
    public String getDataPathSuffix() { return dataPathSuffix; }
    public String getDuplicateIpErrorMessage() { return duplicateIpErrorMessage; }
}

