export type NamespaceNode = {
  namespace: string;
  cluster: string;
  type?: string;
  vgroups: string[];
  units: string[];
};

export function buildNamespaceNodes(data: unknown): NamespaceNode[] {
  const res: NamespaceNode[] = [];
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    for (const [namespace, nsDataValue] of Object.entries(data as any)) {
      const nsData = nsDataValue as any;
      const clusters = nsData?.clusters;
      if (clusters && typeof clusters === 'object') {
        for (const [clusterName, clusterDataValue] of Object.entries(clusters as any)) {
          const clusterData = clusterDataValue as any;
          res.push({
            namespace,
            cluster: clusterName,
            type: clusterData?.type,
            vgroups: Array.isArray(clusterData?.vgroups) ? clusterData.vgroups : [],
            units: Array.isArray(clusterData?.units) ? clusterData.units : []
          });
        }
      }
    }
  }
  return res;
}
