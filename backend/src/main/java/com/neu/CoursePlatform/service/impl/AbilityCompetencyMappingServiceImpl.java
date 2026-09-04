package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.dto.AbilityCompetencyMapDTO;
import com.neu.CoursePlatform.dto.AbilityCompetencyRelationRequest;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.AbilityPointCompetencyRelation;
import com.neu.CoursePlatform.entity.CompetencyPoint;
import com.neu.CoursePlatform.entity.CompetencyTaskObservation;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.mapper.AbilityPointCompetencyRelationMapper;
import com.neu.CoursePlatform.mapper.CompetencyPointMapper;
import com.neu.CoursePlatform.mapper.CompetencyTaskObservationMapper;
import com.neu.CoursePlatform.service.AbilityCompetencyMappingService;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.AbilitySnapshotService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AbilityCompetencyMappingServiceImpl implements AbilityCompetencyMappingService {
    private static final Logger log = LoggerFactory.getLogger(AbilityCompetencyMappingServiceImpl.class);
    private static final String DEFAULT_VERSION = "v1";
    private static final String ALGORITHM_VERSION = "pearson-v1";
    private static final int MIN_TRAINING_SAMPLES = 10;
    private static final int MIN_VALIDATION_SAMPLES = 3;
    private static final double MIN_CORRELATION = 0.20D;

    private final AbilityPointService abilityPointService;
    private final CompetencyPointMapper competencyMapper;
    private final AbilityPointCompetencyRelationMapper relationMapper;
    private final CompetencyTaskObservationMapper observationMapper;
    private final LearningTaskService taskService;
    private final TaskSubmissionService submissionService;
    private final AbilitySnapshotService abilitySnapshotService;
    private final JdbcTemplate jdbcTemplate;

    public AbilityCompetencyMappingServiceImpl(AbilityPointService abilityPointService,
                                               CompetencyPointMapper competencyMapper,
                                               AbilityPointCompetencyRelationMapper relationMapper,
                                               CompetencyTaskObservationMapper observationMapper,
                                               LearningTaskService taskService,
                                               TaskSubmissionService submissionService,
                                               AbilitySnapshotService abilitySnapshotService,
                                               JdbcTemplate jdbcTemplate) {
        this.abilityPointService = abilityPointService;
        this.competencyMapper = competencyMapper;
        this.relationMapper = relationMapper;
        this.observationMapper = observationMapper;
        this.taskService = taskService;
        this.submissionService = submissionService;
        this.abilitySnapshotService = abilitySnapshotService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AbilityCompetencyMapDTO getByCourseCode(String courseCode) {
        List<AbilityPoint> abilityPoints = abilityPointService.listByCourseCode(courseCode);
        List<CompetencyPoint> competencies = competencyMapper.selectList(new LambdaQueryWrapper<CompetencyPoint>()
                .eq(CompetencyPoint::getCourseCode, courseCode)
                .orderByAsc(CompetencyPoint::getSortOrder)
                .orderByAsc(CompetencyPoint::getCompetencyId));
        String version = publishedVersion(courseCode);
        List<AbilityPointCompetencyRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<AbilityPointCompetencyRelation>()
                .eq(AbilityPointCompetencyRelation::getCourseCode, courseCode)
                .eq(AbilityPointCompetencyRelation::getMatrixVersion, version));
        List<CompetencyTaskObservation> observations = observationMapper.selectList(new LambdaQueryWrapper<CompetencyTaskObservation>()
                .eq(CompetencyTaskObservation::getCourseCode, courseCode)
                .eq(CompetencyTaskObservation::getStatus, "active"));
        return new AbilityCompetencyMapDTO(abilityPoints, competencies, relations, observations, version);
    }

    @Override
    @Transactional
    public CompetencyPoint createCompetency(CompetencyPoint competency) {
        if (competency == null || competency.getCourseCode() == null || competency.getCourseCode().isBlank()) {
            throw new IllegalArgumentException("真能力必须绑定课程");
        }
        competency.setCompetencyId(UUID.randomUUID().toString());
        competency.setStatus("active");
        if (competency.getSortOrder() == null) competency.setSortOrder(0);
        competencyMapper.insert(competency);
        return competency;
    }

    @Override
    public CompetencyPoint getCompetencyById(String competencyId) {
        return competencyId == null || competencyId.isBlank() ? null : competencyMapper.selectById(competencyId);
    }

    @Override
    public boolean hasAbilityPoint(String abilityPointId, String courseCode) {
        AbilityPoint point = abilityPointService.getById(abilityPointId);
        return point != null && courseCode != null && courseCode.equals(point.getCourseCode());
    }

    @Override
    public boolean hasTask(String taskNo, String courseCode) {
        LearningTask task = taskService.getById(taskNo);
        return task != null && courseCode != null && courseCode.equals(task.getCourseCode());
    }

    @Override
    @Transactional
    public boolean updateCompetency(String competencyId, CompetencyPoint request) {
        CompetencyPoint existing = competencyMapper.selectById(competencyId);
        if (existing == null) return false;
        if (request == null || (request.getCourseCode() != null && !request.getCourseCode().isBlank()
                && !existing.getCourseCode().equals(request.getCourseCode()))) {
            throw new IllegalArgumentException("不能跨课程修改真能力");
        }
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());
        if (request.getSortOrder() != null) existing.setSortOrder(request.getSortOrder());
        return competencyMapper.updateById(existing) > 0;
    }

    @Override
    @Transactional
    public boolean deleteCompetency(String competencyId) {
        relationMapper.delete(new LambdaQueryWrapper<AbilityPointCompetencyRelation>()
                .eq(AbilityPointCompetencyRelation::getCompetencyId, competencyId));
        observationMapper.delete(new LambdaQueryWrapper<CompetencyTaskObservation>()
                .eq(CompetencyTaskObservation::getCompetencyId, competencyId));
        return competencyMapper.deleteById(competencyId) > 0;
    }

    @Override
    @Transactional
    public void saveRelation(AbilityCompetencyRelationRequest request) {
        if (!hasAbilityPoint(request.getAbilityPointId(), request.getCourseCode())) {
            throw new IllegalArgumentException("能力点不属于当前课程");
        }
        CompetencyPoint competency = getCompetencyById(request.getCompetencyId());
        if (competency == null || !request.getCourseCode().equals(competency.getCourseCode())) {
            throw new IllegalArgumentException("真能力不属于当前课程");
        }
        AbilityPointCompetencyRelation relation = relationMapper.selectOne(new LambdaQueryWrapper<AbilityPointCompetencyRelation>()
                .eq(AbilityPointCompetencyRelation::getCourseCode, request.getCourseCode())
                .eq(AbilityPointCompetencyRelation::getAbilityPointId, request.getAbilityPointId())
                .eq(AbilityPointCompetencyRelation::getCompetencyId, request.getCompetencyId())
                .eq(AbilityPointCompetencyRelation::getMatrixVersion, publishedVersion(request.getCourseCode())));
        if (relation == null) {
            relation = new AbilityPointCompetencyRelation();
            relation.setId(UUID.randomUUID().toString());
            relation.setCourseCode(request.getCourseCode());
            relation.setAbilityPointId(request.getAbilityPointId());
            relation.setCompetencyId(request.getCompetencyId());
            relation.setMatrixVersion(publishedVersion(request.getCourseCode()));
            relation.setStrengthSource("uniform_prior");
            relation.setConfidence(BigDecimal.ZERO);
            relation.setEvidenceCount(0);
        }
        relation.setRelationStatus(request.getRelationStatus());
        relation.setReviewNote(request.getReviewNote());
        relation.setStrength(resolveInitialStrength(request.getCourseCode(), request.getAbilityPointId(), request.getCompetencyId(), request.getRelationStatus()));
        relation.setStrengthSource("uniform_prior");
        relation.setConfidence(BigDecimal.ZERO);
        relation.setEvidenceCount(0);
        relation.setUpdatedAt(LocalDateTime.now());
        relationMapper.insertOrUpdate(relation);
        normalizePublishedRow(request.getCourseCode(), request.getAbilityPointId());
    }

    @Override
    @Transactional
    public void saveObservation(CompetencyTaskObservation observation) {
        CompetencyPoint competency = getCompetencyById(observation.getCompetencyId());
        if (competency == null || !observation.getCourseCode().equals(competency.getCourseCode())) {
            throw new IllegalArgumentException("真能力不属于当前课程");
        }
        if (!hasTask(observation.getTaskNo(), observation.getCourseCode())) {
            throw new IllegalArgumentException("观测任务不属于当前课程");
        }
        CompetencyTaskObservation existing = observationMapper.selectOne(new LambdaQueryWrapper<CompetencyTaskObservation>()
                .eq(CompetencyTaskObservation::getCourseCode, observation.getCourseCode())
                .eq(CompetencyTaskObservation::getTaskNo, observation.getTaskNo())
                .eq(CompetencyTaskObservation::getCompetencyId, observation.getCompetencyId()));
        if (existing == null) {
            observation.setId(UUID.randomUUID().toString());
            existing = observation;
        }
        existing.setCourseCode(observation.getCourseCode());
        existing.setTaskNo(observation.getTaskNo());
        existing.setCompetencyId(observation.getCompetencyId());
        existing.setStatus("active".equalsIgnoreCase(observation.getStatus()) ? "active" : "inactive");
        existing.setUpdatedAt(LocalDateTime.now());
        observationMapper.insertOrUpdate(existing);
    }

    @Override
    @Transactional
    public void saveObservations(List<CompetencyTaskObservation> observations) {
        if (observations == null || observations.isEmpty()) throw new IllegalArgumentException("观测任务不能为空");
        String courseCode = observations.get(0).getCourseCode();
        if (courseCode == null || courseCode.isBlank()) throw new IllegalArgumentException("课程编号不能为空");
        for (CompetencyTaskObservation observation : observations) {
            if (observation == null || !courseCode.equals(observation.getCourseCode())) {
                throw new IllegalArgumentException("批量观测任务必须属于同一课程");
            }
        }
        for (CompetencyTaskObservation observation : observations) saveObservation(observation);
    }

    @Override
    @Transactional
    public Map<String, Object> calibrateStrengths(String courseCode) {
        List<AbilityPoint> abilities = abilityPointService.listByCourseCode(courseCode);
        String baseVersion = publishedVersion(courseCode);
        List<AbilityPointCompetencyRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<AbilityPointCompetencyRelation>()
                .eq(AbilityPointCompetencyRelation::getCourseCode, courseCode)
                .eq(AbilityPointCompetencyRelation::getMatrixVersion, baseVersion));
        List<CompetencyTaskObservation> observations = observationMapper.selectList(new LambdaQueryWrapper<CompetencyTaskObservation>()
                .eq(CompetencyTaskObservation::getCourseCode, courseCode)
                .eq(CompetencyTaskObservation::getStatus, "active"));
        Map<String, LearningTask> taskIndex = new HashMap<>();
        taskService.listByCourseCode(courseCode).forEach(task -> taskIndex.put(task.getTaskNo(), task));
        Map<String, Map<String, ScoreAggregate>> targetByCompetency = new HashMap<>();
        for (CompetencyTaskObservation observation : observations) {
            LearningTask task = taskIndex.get(observation.getTaskNo());
            if (task == null) continue;
            int maxScore = task.getScore() == null || task.getScore() <= 0 ? 100 : task.getScore();
            for (TaskSubmission submission : submissionService.listByTaskNo(observation.getTaskNo())) {
                if (submission.getStudentNo() == null || submission.getScore() == null
                        || "superseded".equalsIgnoreCase(submission.getStatus())) continue;
                double score = Math.max(0D, Math.min(100D, submission.getScore() * 100D / maxScore));
                targetByCompetency.computeIfAbsent(observation.getCompetencyId(), ignored -> new HashMap<>())
                        .computeIfAbsent(submission.getStudentNo(), ignored -> new ScoreAggregate())
                        .add(score);
            }
        }
        Map<String, Map<String, Double>> sourceByAbility = new HashMap<>();
        List<String> evidencedStudents = jdbcTemplate.queryForList(
                "SELECT DISTINCT student_no FROM learning_answer_evidence WHERE course_code = ?",
                String.class, courseCode);
        for (String studentNo : evidencedStudents) {
            for (AbilitySnapshotService.AbilityScore score : abilitySnapshotService.currentScores(studentNo, courseCode)) {
                if (score.abilityPointId() == null || score.score() == null) continue;
                sourceByAbility.computeIfAbsent(score.abilityPointId(), ignored -> new HashMap<>())
                        .put(studentNo, score.score().doubleValue());
            }
        }
        Map<String, Map<String, Object>> report = new LinkedHashMap<>();
        int totalTrainSamples = 0;
        int totalValidationSamples = 0;
        boolean publishable = true;
        List<String> blockingReasons = new ArrayList<>();
        for (AbilityPointCompetencyRelation relation : relations) {
            if (!"related".equalsIgnoreCase(relation.getRelationStatus())) continue;
            Map<String, ScoreAggregate> target = targetByCompetency.getOrDefault(relation.getCompetencyId(), Map.of());
            Map<String, Double> source = sourceByAbility.getOrDefault(relation.getAbilityPointId(), Map.of());
            List<Double> trainX = new ArrayList<>();
            List<Double> trainY = new ArrayList<>();
            List<Double> validationX = new ArrayList<>();
            List<Double> validationY = new ArrayList<>();
            target.forEach((studentNo, value) -> {
                if (!source.containsKey(studentNo)) return;
                // 按学生稳定划分，避免同一学生同时出现在训练和验证数据中。
                if (Math.floorMod(studentNo.hashCode(), 10) < 8) {
                    trainX.add(source.get(studentNo));
                    trainY.add(value.average());
                } else {
                    validationX.add(source.get(studentNo));
                    validationY.add(value.average());
                }
            });
            double correlation = pearson(trainX, trainY);
            double validationCorrelation = pearson(validationX, validationY);
            totalTrainSamples += trainX.size();
            totalValidationSamples += validationX.size();
            boolean enoughSamples = trainX.size() >= MIN_TRAINING_SAMPLES
                    && validationX.size() >= MIN_VALIDATION_SAMPLES;
            boolean directionConsistent = validationX.size() < MIN_VALIDATION_SAMPLES
                    || (correlation >= 0 && validationCorrelation >= 0);
            boolean meaningfulCorrelation = correlation >= MIN_CORRELATION;
            if (!enoughSamples) {
                publishable = false;
                blockingReasons.add(relation.getAbilityPointId() + "->" + relation.getCompetencyId() + ":样本不足");
            } else if (!directionConsistent) {
                publishable = false;
                blockingReasons.add(relation.getAbilityPointId() + "->" + relation.getCompetencyId() + ":训练与验证方向冲突");
            } else if (!meaningfulCorrelation) {
                publishable = false;
                blockingReasons.add(relation.getAbilityPointId() + "->" + relation.getCompetencyId() + ":相关程度未达到门槛");
            }
            double raw = Math.max(0D, correlation);
            relation.setStrength(BigDecimal.valueOf(raw));
            relation.setEvidenceCount(trainX.size());
            relation.setConfidence(BigDecimal.valueOf(Math.min(1D, trainX.size() / 20D) * Math.max(0D, correlation))
                    .setScale(4, java.math.RoundingMode.HALF_UP));
            relation.setStrengthSource("behavior_calibration");
            Map<String, Object> relationReport = new LinkedHashMap<>();
            relationReport.put("abilityPointId", relation.getAbilityPointId());
            relationReport.put("competencyId", relation.getCompetencyId());
            relationReport.put("sampleCount", trainX.size());
            relationReport.put("correlation", round(correlation));
            relationReport.put("rawStrength", round(raw));
            relationReport.put("validationSampleCount", validationX.size());
            relationReport.put("validationCorrelation", round(validationCorrelation));
            relationReport.put("validationDirectionConsistent", directionConsistent);
            relationReport.put("enoughSamples", enoughSamples);
            relationReport.put("meaningfulCorrelation", meaningfulCorrelation);
            relationReport.put("publishable", enoughSamples && directionConsistent && meaningfulCorrelation);
            relationReport.put("confidence", relation.getConfidence().doubleValue());
            report.put(relation.getAbilityPointId() + "->" + relation.getCompetencyId(), relationReport);
        }
        for (AbilityPoint ability : abilities) {
            List<AbilityPointCompetencyRelation> row = relations.stream()
                    .filter(item -> ability.getAbilityPointId().equals(item.getAbilityPointId())
                            && "related".equalsIgnoreCase(item.getRelationStatus())).toList();
            double total = row.stream().mapToDouble(item -> item.getStrength() == null ? 0D : item.getStrength().doubleValue()).sum();
            if (total <= 0D) continue;
            for (AbilityPointCompetencyRelation relation : row) {
                relation.setStrength(relation.getStrength().divide(BigDecimal.valueOf(total), 6, java.math.RoundingMode.HALF_UP));
                // 归一化后的关系稍后以候选版本写入，正式版本保持不变。
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matrixVersion", baseVersion);
        result.put("candidateVersion", null);
        result.put("relationCount", report.size());
        result.put("sampleCount", totalTrainSamples);
        result.put("validationSampleCount", totalValidationSamples);
        result.put("algorithmVersion", ALGORITHM_VERSION);
        result.put("publishable", publishable);
        result.put("blockingReasons", blockingReasons);
        result.put("relations", report.values());
        if (!publishable || report.isEmpty()) return result;

        String candidateVersion = "v" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update("INSERT INTO ability_competency_matrix_version "
                        + "(id, course_code, version, status, based_on_version, sample_count, validation_sample_count, algorithm_version, created_at) VALUES (?, ?, ?, 'draft', ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID().toString(), courseCode, candidateVersion, baseVersion,
                totalTrainSamples, totalValidationSamples, ALGORITHM_VERSION);
        for (AbilityPointCompetencyRelation relation : relations) {
            relation.setId(UUID.randomUUID().toString());
            relation.setMatrixVersion(candidateVersion);
            relation.setUpdatedAt(LocalDateTime.now());
            relationMapper.insert(relation);
        }
        result.put("candidateVersion", candidateVersion);
        return result;
    }

    @Override
    @Transactional
    public void publishVersion(String courseCode, String version) {
        publishVersion(courseCode, version, null);
    }

    @Override
    @Transactional
    public void publishVersion(String courseCode, String version, String publisherId) {
        List<String> lockedCourses = jdbcTemplate.queryForList(
                "SELECT course_code FROM course WHERE course_code = ? FOR UPDATE", String.class, courseCode);
        if (lockedCourses.isEmpty()) throw new IllegalArgumentException("课程不存在");
        List<Map<String, Object>> candidates = jdbcTemplate.queryForList(
                "SELECT based_on_version, sample_count, validation_sample_count FROM ability_competency_matrix_version WHERE course_code = ? AND version = ? AND status = 'draft'",
                courseCode, version);
        if (candidates.isEmpty()) throw new IllegalArgumentException("候选矩阵不存在或已发布");
        Map<String, Object> candidate = candidates.get(0);
        String currentVersion = publishedVersion(courseCode);
        if (!currentVersion.equals(candidate.get("based_on_version"))) {
            throw new IllegalArgumentException("候选矩阵已过期，请重新生成");
        }
        if (Integer.parseInt(String.valueOf(candidate.get("sample_count"))) < MIN_TRAINING_SAMPLES
                || Integer.parseInt(String.valueOf(candidate.get("validation_sample_count"))) < MIN_VALIDATION_SAMPLES) {
            throw new IllegalArgumentException("候选矩阵证据不足，不能发布");
        }
        jdbcTemplate.update("UPDATE ability_competency_matrix_version SET status = 'archived' WHERE course_code = ? AND status = 'published'", courseCode);
        int updated = jdbcTemplate.update("UPDATE ability_competency_matrix_version SET status = 'published', published_at = CURRENT_TIMESTAMP, published_by = ? WHERE course_code = ? AND version = ? AND status = 'draft' AND based_on_version = ?", publisherId, courseCode, version, currentVersion);
        if (updated != 1) throw new IllegalArgumentException("候选矩阵不存在或已发布");
    }

    private String publishedVersion(String courseCode) {
        try {
            String version = jdbcTemplate.queryForObject(
                    "SELECT version FROM ability_competency_matrix_version WHERE course_code = ? AND status = 'published' ORDER BY published_at DESC LIMIT 1",
                    String.class, courseCode);
            return version == null ? DEFAULT_VERSION : version;
        } catch (EmptyResultDataAccessException ignored) {
            return DEFAULT_VERSION;
        } catch (DataAccessException ex) {
            log.error("读取课程正式能力矩阵版本失败，courseCode={}", courseCode, ex);
            throw ex;
        }
    }

    private double pearson(List<Double> x, List<Double> y) {
        if (x.size() < 3) return 0D;
        double xMean = x.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
        double yMean = y.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
        double numerator = 0D, xSum = 0D, ySum = 0D;
        for (int i = 0; i < x.size(); i++) {
            double dx = x.get(i) - xMean, dy = y.get(i) - yMean;
            numerator += dx * dy; xSum += dx * dx; ySum += dy * dy;
        }
        return xSum == 0D || ySum == 0D ? 0D : numerator / Math.sqrt(xSum * ySum);
    }

    private double round(double value) { return Math.round(value * 10000D) / 10000D; }

    private BigDecimal resolveInitialStrength(String courseCode, String abilityPointId, String competencyId, String status) {
        if (!"related".equalsIgnoreCase(status)) return BigDecimal.ZERO;
        long count = relationMapper.selectCount(new LambdaQueryWrapper<AbilityPointCompetencyRelation>()
                .eq(AbilityPointCompetencyRelation::getCourseCode, courseCode)
                .eq(AbilityPointCompetencyRelation::getAbilityPointId, abilityPointId)
                .eq(AbilityPointCompetencyRelation::getRelationStatus, "related")
                .eq(AbilityPointCompetencyRelation::getMatrixVersion, publishedVersion(courseCode))
                .ne(AbilityPointCompetencyRelation::getCompetencyId, competencyId));
        return BigDecimal.ONE.divide(BigDecimal.valueOf(count + 1), 4, java.math.RoundingMode.HALF_UP);
    }

    private void normalizePublishedRow(String courseCode, String abilityPointId) {
        String version = publishedVersion(courseCode);
        List<AbilityPointCompetencyRelation> row = relationMapper.selectList(
                new LambdaQueryWrapper<AbilityPointCompetencyRelation>()
                        .eq(AbilityPointCompetencyRelation::getCourseCode, courseCode)
                        .eq(AbilityPointCompetencyRelation::getAbilityPointId, abilityPointId)
                        .eq(AbilityPointCompetencyRelation::getMatrixVersion, version));
        List<AbilityPointCompetencyRelation> related = row.stream()
                .filter(item -> "related".equalsIgnoreCase(item.getRelationStatus())).toList();
        BigDecimal uniform = related.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.ONE.divide(BigDecimal.valueOf(related.size()), 6, java.math.RoundingMode.HALF_UP);
        for (AbilityPointCompetencyRelation item : row) {
            item.setStrength("related".equalsIgnoreCase(item.getRelationStatus()) ? uniform : BigDecimal.ZERO);
            item.setStrengthSource("uniform_prior");
            item.setConfidence(BigDecimal.ZERO);
            item.setEvidenceCount(0);
            item.setUpdatedAt(LocalDateTime.now());
            relationMapper.updateById(item);
        }
    }

    private static final class ScoreAggregate {
        private double sum;
        private int count;

        private void add(double value) {
            sum += value;
            count++;
        }

        private double average() {
            return count == 0 ? 0D : sum / count;
        }
    }
}
