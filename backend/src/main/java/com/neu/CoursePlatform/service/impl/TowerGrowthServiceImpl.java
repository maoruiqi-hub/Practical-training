package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.entity.StudentTowerNode;
import com.neu.CoursePlatform.entity.StudentTowerRun;
import com.neu.CoursePlatform.entity.TowerActionLog;
import com.neu.CoursePlatform.entity.TowerNodeOption;
import com.neu.CoursePlatform.entity.TowerRunInventory;
import com.neu.CoursePlatform.mapper.StudentTowerNodeMapper;
import com.neu.CoursePlatform.mapper.StudentTowerRunMapper;
import com.neu.CoursePlatform.mapper.TowerActionLogMapper;
import com.neu.CoursePlatform.mapper.TowerNodeOptionMapper;
import com.neu.CoursePlatform.mapper.TowerRunInventoryMapper;
import com.neu.CoursePlatform.profile.entity.StudentProfile;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.service.TowerGrowthService;
import com.neu.CoursePlatform.service.TowerRunService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TowerGrowthServiceImpl implements TowerGrowthService {
    private static final String RULE_VERSION = "tower_growth_v1";
    private static final String HEALING_SUPPLY = "healing_supply";
    private static final Set<String> COMBAT = Set.of("battle", "elite", "boss");
    private static final Set<String> NON_COMBAT = Set.of("rest", "shop", "treasure", "event");

    private final StudentTowerRunMapper runMapper;
    private final StudentTowerNodeMapper nodeMapper;
    private final TowerNodeOptionMapper optionMapper;
    private final TowerRunInventoryMapper inventoryMapper;
    private final TowerActionLogMapper actionMapper;
    private final TowerRunService towerRunService;
    private final ProfileService profileService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public TowerGrowthServiceImpl(StudentTowerRunMapper runMapper,
                                  StudentTowerNodeMapper nodeMapper,
                                  TowerNodeOptionMapper optionMapper,
                                  TowerRunInventoryMapper inventoryMapper,
                                  TowerActionLogMapper actionMapper,
                                  TowerRunService towerRunService,
                                  ProfileService profileService,
                                  ObjectMapper objectMapper,
                                  JdbcTemplate jdbcTemplate) {
        this.runMapper = runMapper;
        this.nodeMapper = nodeMapper;
        this.optionMapper = optionMapper;
        this.inventoryMapper = inventoryMapper;
        this.actionMapper = actionMapper;
        this.towerRunService = towerRunService;
        this.profileService = profileService;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public Map<String, Object> getNodeOptions(String studentNo, String runId, String nodeId) {
        StudentTowerRun run = lockOwnedRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        validateOptionAccess(run, node);
        List<TowerNodeOption> options = nodeOptions(runId, nodeId);
        if (options.isEmpty()) {
            for (OptionRule rule : rulesFor(node)) optionMapper.insert(toOption(run, node, rule));
            options = nodeOptions(runId, nodeId);
        }
        return optionEnvelope(run, node, options);
    }

    @Override
    @Transactional
    public Map<String, Object> chooseNodeOption(String studentNo, String runId, String nodeId,
                                                String optionId, String actionId) {
        requireActionId(actionId);
        StudentTowerRun run = lockOwnedRun(studentNo, runId);
        Map<String, Object> replay = replay(actionId, studentNo, runId, nodeId, "choose_option", optionId);
        if (replay != null) return replay;

        StudentTowerNode node = requireNode(runId, nodeId);
        validateOptionAccess(run, node);
        TowerNodeOption option = optionMapper.selectById(optionId);
        if (option == null || !runId.equals(option.getRunId()) || !nodeId.equals(option.getNodeId())) {
            throw new IllegalArgumentException("选项不属于当前节点");
        }
        boolean alreadySelected = nodeOptions(runId, nodeId).stream().anyMatch(item -> Boolean.TRUE.equals(item.getSelected()));
        if (alreadySelected) throw new IllegalStateException("当前节点已完成选择");

        OptionRule rule = rulesFor(node).stream()
                .filter(item -> item.code().equals(option.getOptionCode()))
                .findFirst().orElseThrow(() -> new IllegalStateException("选项规则已失效，请刷新节点"));
        applyRule(run, rule, actionId);
        option.setSelected(true);
        option.setSelectedAt(LocalDateTime.now());
        optionMapper.updateById(option);
        if (NON_COMBAT.contains(node.getRoomType())) {
            towerRunService.completeNonCombatNode(studentNo, runId, nodeId, rule.code());
        }

        Map<String, Object> result = actionResult(run, node, actionId, optionId, rule.title(), rule.description());
        saveAction(actionId, runId, nodeId, studentNo, "choose_option", optionId, result);
        return result;
    }

    @Override
    public List<Map<String, Object>> getInventory(String studentNo, String runId) {
        requireOwnedRun(studentNo, runId);
        return inventory(runId).stream().map(this::inventoryDto).toList();
    }

    @Override
    @Transactional
    public Map<String, Object> useInventoryItem(String studentNo, String runId, String nodeId,
                                                String itemCode, String actionId) {
        requireActionId(actionId);
        StudentTowerRun run = lockOwnedRun(studentNo, runId);
        Map<String, Object> replay = replay(actionId, studentNo, runId, nodeId, "use_inventory", itemCode);
        if (replay != null) return replay;
        if (!"active".equals(run.getStatus())) throw new IllegalStateException("当前路线已结束");
        StudentTowerNode node = requireNode(runId, nodeId);
        if ("locked".equals(node.getStatus()) || "disabled".equals(node.getStatus())) {
            throw new IllegalStateException("当前节点不可使用补给");
        }
        if (!HEALING_SUPPLY.equals(itemCode)) throw new IllegalArgumentException("不支持的库存物品");
        TowerRunInventory item = inventoryItem(runId, itemCode);
        if (item == null || item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new IllegalStateException("恢复补给库存不足");
        }
        item.setQuantity(item.getQuantity() - 1);
        item.setVersion(item.getVersion() + 1);
        item.setUpdatedAt(LocalDateTime.now());
        inventoryMapper.updateById(item);
        lockProfile(run);
        applyProfile(run, new OptionRule(itemCode, "使用恢复补给", "恢复 30 HP", 30, 0, 0, 0, 0, 0, 0, null), actionId);

        Map<String, Object> result = actionResult(run, node, actionId, itemCode, "恢复补给已使用", "库存 -1，HP 由服务端结算");
        saveAction(actionId, runId, nodeId, studentNo, "use_inventory", itemCode, result);
        return result;
    }

    private void validateOptionAccess(StudentTowerRun run, StudentTowerNode node) {
        if (COMBAT.contains(node.getRoomType())) {
            if ("archived".equals(run.getStatus())) throw new IllegalStateException("已归档路线不能领取奖励");
            if (!"cleared".equals(node.getStatus())) throw new IllegalStateException("战斗通关后才能领取奖励");
            return;
        }
        if (!NON_COMBAT.contains(node.getRoomType())) throw new IllegalArgumentException("当前节点没有可选成长动作");
        if (!"active".equals(run.getStatus())) throw new IllegalStateException("当前路线已结束");
        if ("locked".equals(node.getStatus()) || "disabled".equals(node.getStatus())) {
            throw new IllegalStateException("节点尚未解锁");
        }
    }

    private List<OptionRule> rulesFor(StudentTowerNode node) {
        return switch (node.getRoomType()) {
            case "battle" -> List.of(
                    rule("reward_coins", "金币袋", "获得 20 金币", 0, 0, 0, 0, 20, 0, 0, null),
                    rule("reward_supply", "恢复补给", "获得 1 份可使用的恢复补给", 0, 0, 0, 0, 0, 0, 1, HEALING_SUPPLY),
                    rule("reward_focus", "战斗复盘", "防御 +1，精力 +1", 0, 0, 1, 0, 0, 1, 0, null));
            case "elite", "boss" -> List.of(
                    rule("reward_coins", "稀有金币袋", "获得 35 金币", 0, 0, 0, 0, 35, 0, 0, null),
                    rule("reward_supply", "恢复补给", "获得 1 份可使用的恢复补给", 0, 0, 0, 0, 0, 0, 1, HEALING_SUPPLY),
                    rule("reward_focus", "专注徽记", "攻击 +1，精力 +1", 0, 1, 0, 0, 0, 1, 0, null));
            case "treasure" -> List.of(
                    rule("treasure_coins", "古旧钱匣", "获得 25 金币", 0, 0, 0, 0, 25, 0, 0, null),
                    rule("treasure_supply", "补给箱", "获得 1 份恢复补给", 0, 0, 0, 0, 0, 0, 1, HEALING_SUPPLY));
            case "rest" -> List.of(
                    rule("rest_restore", "休整恢复", "HP +20", 20, 0, 0, 0, 0, 0, 0, null),
                    rule("rest_focus", "复盘训练", "防御 +1，精力 +1", 0, 0, 1, 0, 0, 1, 0, null));
            case "shop" -> List.of(
                    rule("shop_supply", "购买恢复补给", "消耗 20 金币，获得 1 份补给", 0, 0, 0, 0, -20, 0, 1, HEALING_SUPPLY),
                    rule("shop_focus", "购买专注笔记", "消耗 10 金币，精力 +1", 0, 0, 0, 0, -10, 1, 0, null),
                    rule("shop_leave", "离开商店", "不购买任何物品", 0, 0, 0, 0, 0, 0, 0, null));
            case "event" -> List.of(
                    rule("event_study", "深入研究", "经验 +20，金币 +5，精力 -1", 0, 0, 0, 20, 5, -1, 0, null),
                    rule("event_safe", "稳妥处理", "金币 +5", 0, 0, 0, 0, 5, 0, 0, null));
            default -> List.of();
        };
    }

    private void applyRule(StudentTowerRun run, OptionRule rule, String actionId) {
        StudentProfile profile = lockProfile(run);
        if (rule.coins() < 0 && (profile.getCoins() == null ? 0 : profile.getCoins()) < -rule.coins()) {
            throw new IllegalStateException("金币不足");
        }
        applyProfile(run, rule, actionId);
        if (rule.itemQuantity() != 0 && rule.itemCode() != null) {
            changeInventory(run, rule.itemCode(), rule.itemQuantity());
        }
    }

    private StudentProfile lockProfile(StudentTowerRun run) {
        int studentNo = parseNumber(run.getStudentNo(), "studentNo");
        int courseCode = parseNumber(run.getCourseCode(), "courseCode");
        profileService.getOrCreateProfile(studentNo, courseCode);
        jdbcTemplate.queryForObject("""
                SELECT id FROM student_profile
                WHERE student_no = ? AND course_code = ?
                FOR UPDATE
                """, String.class, studentNo, courseCode);
        return profileService.getOrCreateProfile(studentNo, courseCode);
    }

    private void applyProfile(StudentTowerRun run, OptionRule rule, String actionId) {
        profileService.applyGameDelta(parseNumber(run.getStudentNo(), "studentNo"),
                parseNumber(run.getCourseCode(), "courseCode"), rule.hp(), rule.atk(), rule.def(),
                rule.exp(), rule.coins(), rule.energy(), "tower_growth_option", actionId);
    }

    private void changeInventory(StudentTowerRun run, String itemCode, int delta) {
        TowerRunInventory item = inventoryItem(run.getRunId(), itemCode);
        if (item == null) {
            item = new TowerRunInventory();
            item.setId(SharedIds.newId());
            item.setRunId(run.getRunId());
            item.setStudentNo(run.getStudentNo());
            item.setItemCode(itemCode);
            item.setQuantity(delta);
            item.setVersion(1);
            item.setUpdatedAt(LocalDateTime.now());
            inventoryMapper.insert(item);
        } else {
            int next = item.getQuantity() + delta;
            if (next < 0) throw new IllegalStateException("库存不足");
            item.setQuantity(next);
            item.setVersion(item.getVersion() + 1);
            item.setUpdatedAt(LocalDateTime.now());
            inventoryMapper.updateById(item);
        }
    }

    private TowerNodeOption toOption(StudentTowerRun run, StudentTowerNode node, OptionRule rule) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ruleVersion", RULE_VERSION);
        snapshot.put("title", rule.title());
        snapshot.put("description", rule.description());
        TowerNodeOption option = new TowerNodeOption();
        option.setOptionId(SharedIds.newId());
        option.setRunId(run.getRunId());
        option.setNodeId(node.getNodeId());
        option.setOptionKind(COMBAT.contains(node.getRoomType()) ? "reward" : "room");
        option.setOptionCode(rule.code());
        option.setOptionSnapshotJson(write(snapshot));
        option.setSelected(false);
        option.setCreatedAt(LocalDateTime.now());
        return option;
    }

    private Map<String, Object> optionEnvelope(StudentTowerRun run, StudentTowerNode node, List<TowerNodeOption> options) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("runId", run.getRunId());
        result.put("nodeId", node.getNodeId());
        result.put("roomType", node.getRoomType());
        result.put("ruleVersion", RULE_VERSION);
        result.put("resolved", options.stream().anyMatch(item -> Boolean.TRUE.equals(item.getSelected())));
        result.put("options", options.stream().map(this::optionDto).toList());
        result.put("inventory", inventory(run.getRunId()).stream().map(this::inventoryDto).toList());
        return result;
    }

    private Map<String, Object> optionDto(TowerNodeOption option) {
        Map<String, Object> result = read(option.getOptionSnapshotJson());
        result.put("optionId", option.getOptionId());
        result.put("optionCode", option.getOptionCode());
        result.put("optionKind", option.getOptionKind());
        result.put("selected", Boolean.TRUE.equals(option.getSelected()));
        return result;
    }

    private Map<String, Object> actionResult(StudentTowerRun run, StudentTowerNode node, String actionId,
                                             String targetId, String title, String description) {
        StudentProfile profile = profileService.getOrCreateProfile(parseNumber(run.getStudentNo(), "studentNo"),
                parseNumber(run.getCourseCode(), "courseCode"));
        Map<String, Object> profileDto = new LinkedHashMap<>();
        profileDto.put("hp", profile.getHp());
        profileDto.put("atk", profile.getAtk());
        profileDto.put("def", profile.getDef());
        profileDto.put("exp", profile.getExp());
        profileDto.put("level", profile.getLevel());
        profileDto.put("coins", profile.getCoins());
        profileDto.put("energy", profile.getEnergy());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("actionId", actionId);
        result.put("targetId", targetId);
        result.put("title", title);
        result.put("description", description);
        result.put("applied", true);
        result.put("nodeId", node.getNodeId());
        result.put("profile", profileDto);
        result.put("inventory", inventory(run.getRunId()).stream().map(this::inventoryDto).toList());
        return result;
    }

    private void saveAction(String actionId, String runId, String nodeId, String studentNo,
                            String actionType, String targetId, Map<String, Object> result) {
        TowerActionLog log = new TowerActionLog();
        log.setActionId(actionId);
        log.setRunId(runId);
        log.setNodeId(nodeId);
        log.setStudentNo(studentNo);
        log.setActionType(actionType);
        log.setTargetId(targetId);
        log.setResultJson(write(result));
        log.setCreatedAt(LocalDateTime.now());
        actionMapper.insert(log);
    }

    private Map<String, Object> replay(String actionId, String studentNo, String runId, String nodeId,
                                       String actionType, String targetId) {
        TowerActionLog previous = actionMapper.selectById(actionId);
        if (previous == null) return null;
        if (!studentNo.equals(previous.getStudentNo()) || !runId.equals(previous.getRunId())
                || !safeEquals(nodeId, previous.getNodeId()) || !actionType.equals(previous.getActionType())
                || !safeEquals(targetId, previous.getTargetId())) {
            throw new IllegalStateException("actionId 已被其他动作使用");
        }
        Map<String, Object> result = read(previous.getResultJson());
        result.put("replayed", true);
        return result;
    }

    private StudentTowerRun lockOwnedRun(String studentNo, String runId) {
        StudentTowerRun run = runMapper.selectForUpdate(runId);
        if (run == null || !studentNo.equals(run.getStudentNo())) throw new IllegalArgumentException("爬塔路线不存在");
        return run;
    }

    private StudentTowerRun requireOwnedRun(String studentNo, String runId) {
        StudentTowerRun run = runMapper.selectById(runId);
        if (run == null || !studentNo.equals(run.getStudentNo())) throw new IllegalArgumentException("爬塔路线不存在");
        return run;
    }

    private StudentTowerNode requireNode(String runId, String nodeId) {
        StudentTowerNode node = nodeMapper.selectById(nodeId);
        if (node == null || !runId.equals(node.getRunId())) throw new IllegalArgumentException("爬塔节点不存在");
        return node;
    }

    private List<TowerNodeOption> nodeOptions(String runId, String nodeId) {
        return optionMapper.selectList(new LambdaQueryWrapper<TowerNodeOption>()
                .eq(TowerNodeOption::getRunId, runId)
                .eq(TowerNodeOption::getNodeId, nodeId)
                .orderByAsc(TowerNodeOption::getCreatedAt));
    }

    private List<TowerRunInventory> inventory(String runId) {
        return inventoryMapper.selectList(new LambdaQueryWrapper<TowerRunInventory>()
                .eq(TowerRunInventory::getRunId, runId)
                .gt(TowerRunInventory::getQuantity, 0)
                .orderByAsc(TowerRunInventory::getItemCode));
    }

    private TowerRunInventory inventoryItem(String runId, String itemCode) {
        return inventoryMapper.selectOne(new LambdaQueryWrapper<TowerRunInventory>()
                .eq(TowerRunInventory::getRunId, runId)
                .eq(TowerRunInventory::getItemCode, itemCode));
    }

    private Map<String, Object> inventoryDto(TowerRunInventory item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemCode", item.getItemCode());
        result.put("name", HEALING_SUPPLY.equals(item.getItemCode()) ? "恢复补给" : item.getItemCode());
        result.put("quantity", item.getQuantity());
        return result;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("无法保存节点结算快照", e);
        }
    }

    private Map<String, Object> read(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            throw new IllegalStateException("无法读取节点结算快照", e);
        }
    }

    private static OptionRule rule(String code, String title, String description,
                                   int hp, int atk, int def, int exp, int coins, int energy,
                                   int itemQuantity, String itemCode) {
        return new OptionRule(code, title, description, hp, atk, def, exp, coins, energy, itemQuantity, itemCode);
    }

    private static void requireActionId(String actionId) {
        if (actionId == null || actionId.isBlank() || actionId.length() > 64) {
            throw new IllegalArgumentException("actionId 不可为空且长度不能超过 64");
        }
    }

    private static int parseNumber(String value, String field) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(field + " 不能投影到现有画像子域");
        }
    }

    private static boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private record OptionRule(String code, String title, String description,
                              int hp, int atk, int def, int exp, int coins, int energy,
                              int itemQuantity, String itemCode) { }
}
