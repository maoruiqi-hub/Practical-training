package com.neu.CoursePlatform.profile;

import com.neu.CoursePlatform.service.TowerGrowthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tower_growth_chain;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:profile-projection-chain-schema.sql",
        "app.performance-indexes.enabled=false"
})
class TowerGrowthChainIntegrationTest {

    @Autowired
    private TowerGrowthService growthService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seedRun() {
        jdbc.update("INSERT INTO student(student_no, name) VALUES ('7', '闭环测试学生')");
        jdbc.update("INSERT INTO knowledge_point(knowledge_point_id, course_code, name, importance) VALUES ('kp-growth', '101', '对象成长', 1)");
        jdbc.update("""
                INSERT INTO student_profile(id, student_no, course_code, hp, atk, def, exp, level, coins, energy,
                                            consecutive_correct, recent_answers, recent_scores, updated_at)
                VALUES ('profile-growth', '7', '101', 40, 50, 50, 0, 1, 40, 5, 0, '', '', CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO student_tower_run(run_id, student_no, course_code, version, status, route_source,
                                              current_node_id, created_at, updated_at)
                VALUES ('run-growth', '7', '101', 1, 'active', 'rule', 'node-rest', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        insertNode("node-rest", 1, "rest", "available", null);
        insertNode("node-shop", 2, "shop", "locked", "node-rest");
        insertNode("node-next", 3, "battle", "locked", "node-shop");
        insertNode("node-reward", 4, "battle", "cleared", null);
        insertNode("node-poor-shop", 5, "shop", "available", null);
    }

    @Test
    void roomRewardInventoryAndProfileFormOneIdempotentChain() {
        assertThatThrownBy(() -> growthService.useInventoryItem(
                "7", "run-growth", "node-rest", "healing_supply", "use-empty"))
                .hasMessageContaining("库存不足");

        Map<String, Object> restEnvelope = growthService.getNodeOptions("7", "run-growth", "node-rest");
        Map<String, Object> restOption = option(restEnvelope, "rest_restore");
        Map<String, Object> restResult = growthService.chooseNodeOption(
                "7", "run-growth", "node-rest", String.valueOf(restOption.get("optionId")), "action-rest");
        assertThat(profileValue("hp")).isEqualTo(60);
        assertThat(nodeStatus("node-rest")).isEqualTo("cleared");
        assertThat(nodeStatus("node-shop")).isEqualTo("available");

        Map<String, Object> restReplay = growthService.chooseNodeOption(
                "7", "run-growth", "node-rest", String.valueOf(restOption.get("optionId")), "action-rest");
        assertThat(restReplay.get("replayed")).isEqualTo(true);
        assertThat(profileValue("hp")).isEqualTo(60);
        assertThat(count("tower_action_log")).isEqualTo(1);

        Map<String, Object> shopEnvelope = growthService.getNodeOptions("7", "run-growth", "node-shop");
        Map<String, Object> supplyOption = option(shopEnvelope, "shop_supply");
        growthService.chooseNodeOption(
                "7", "run-growth", "node-shop", String.valueOf(supplyOption.get("optionId")), "action-shop");
        assertThat(profileValue("coins")).isEqualTo(20);
        assertThat(inventoryQuantity()).isEqualTo(1);
        assertThat(nodeStatus("node-next")).isEqualTo("available");

        Map<String, Object> useResult = growthService.useInventoryItem(
                "7", "run-growth", "node-next", "healing_supply", "action-use");
        assertThat(((Map<?, ?>) useResult.get("profile")).get("hp")).isEqualTo(90);
        assertThat(inventoryQuantity()).isZero();
        growthService.useInventoryItem("7", "run-growth", "node-next", "healing_supply", "action-use");
        assertThat(profileValue("hp")).isEqualTo(90);
        assertThat(inventoryQuantity()).isZero();

        Map<String, Object> rewardEnvelope = growthService.getNodeOptions("7", "run-growth", "node-reward");
        Map<String, Object> reward = option(rewardEnvelope, "reward_coins");
        growthService.chooseNodeOption(
                "7", "run-growth", "node-reward", String.valueOf(reward.get("optionId")), "action-reward");
        assertThat(profileValue("coins")).isEqualTo(40);
        assertThat(count("tower_action_log")).isEqualTo(4);
        assertThat(count("growth_history")).isEqualTo(4);

        assertThatThrownBy(() -> growthService.chooseNodeOption(
                "7", "run-growth", "node-reward", "forged-option", "action-forged"))
                .hasMessageContaining("不属于当前节点");
        assertThat(profileValue("coins")).isEqualTo(40);

        jdbc.update("UPDATE student_profile SET coins = 0 WHERE id = 'profile-growth'");
        Map<String, Object> poorShop = growthService.getNodeOptions("7", "run-growth", "node-poor-shop");
        Map<String, Object> unaffordable = option(poorShop, "shop_supply");
        assertThatThrownBy(() -> growthService.chooseNodeOption(
                "7", "run-growth", "node-poor-shop", String.valueOf(unaffordable.get("optionId")), "action-poor"))
                .hasMessageContaining("金币不足");
        assertThat(count("tower_action_log")).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tower_node_option WHERE node_id = 'node-poor-shop' AND selected = TRUE", Integer.class))
                .isZero();
        assertThat(nodeStatus("node-poor-shop")).isEqualTo("available");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> option(Map<String, Object> envelope, String code) {
        return ((List<Map<String, Object>>) envelope.get("options")).stream()
                .filter(item -> code.equals(item.get("optionCode")))
                .findFirst().orElseThrow();
    }

    private void insertNode(String nodeId, int order, String roomType, String status, String unlockAfter) {
        jdbc.update("""
                INSERT INTO student_tower_node(node_id, run_id, node_order, row_no, col_no, room_type, status,
                                               knowledge_point_id, unlock_after_node_id, difficulty, created_at, updated_at)
                VALUES (?, 'run-growth', ?, ?, 1, ?, ?, 'kp-growth', ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, nodeId, order, order, roomType, status, unlockAfter);
    }

    private int profileValue(String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM student_profile WHERE id = 'profile-growth'", Integer.class);
    }

    private String nodeStatus(String nodeId) {
        return jdbc.queryForObject("SELECT status FROM student_tower_node WHERE node_id = ?", String.class, nodeId);
    }

    private int inventoryQuantity() {
        return jdbc.queryForObject("SELECT quantity FROM tower_run_inventory WHERE run_id = 'run-growth'", Integer.class);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
