package com.neu.CoursePlatform.service;

import java.util.List;
import java.util.Map;

public interface TowerGrowthService {
    Map<String, Object> getNodeOptions(String studentNo, String runId, String nodeId);
    Map<String, Object> chooseNodeOption(String studentNo, String runId, String nodeId,
                                         String optionId, String actionId);
    List<Map<String, Object>> getInventory(String studentNo, String runId);
    Map<String, Object> useInventoryItem(String studentNo, String runId, String nodeId,
                                         String itemCode, String actionId);
}
