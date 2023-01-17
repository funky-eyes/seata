package io.seata.discovery.loadbalance;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.discovery.registry.RegistryFactory;
import io.seata.discovery.registry.RegistryService;


import static io.seata.discovery.loadbalance.LoadBalanceFactory.GROUP_LOAD_BALANCE;

/**
 * @author jianbin.chen
 */
@LoadLevel(name = GROUP_LOAD_BALANCE)
public class GroupLoadBalance implements LoadBalance {

	RegistryService<?> registryService = RegistryFactory.getInstance();

    @Override
    public <T> T select(List<T> invokers, String group) throws Exception {
        Map<String, List<InetSocketAddress>> inetSocketAddressMap = registryService.lookup();
        List<String> groupList = new ArrayList<>(inetSocketAddressMap.keySet());
        if (!inetSocketAddressMap.containsKey(group)) {
            inetSocketAddressMap.put(group, registryService.lookupByGroup(group));
        }
        if (StringUtils.isBlank(group)) {
            group = groupList.get(ThreadLocalRandom.current().nextInt(groupList.size()));
        }
        return (T)inetSocketAddressMap.get(group).get(0);
    }

}
