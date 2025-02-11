package org.apache.seata.namingserver.entity.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jianbin@apache.org
 */
public class NamespaceVO {

	List<String> clusters = new ArrayList<>();

	List<String> vgroups = new ArrayList<>();

	public List<String> getClusters() {
		return clusters;
	}

	public void setClusters(List<String> clusters) {
		this.clusters = clusters;
	}

	public List<String> getVgroups() {
		return vgroups;
	}

	public void setVgroups(List<String> vgroups) {
		this.vgroups = vgroups;
	}
}
