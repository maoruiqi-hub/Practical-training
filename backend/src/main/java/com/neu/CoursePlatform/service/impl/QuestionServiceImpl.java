package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.dto.PaperGenerateRequest;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.QuestionMapper;
import com.neu.CoursePlatform.service.QuestionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Override
    public List<Question> listByCourseCode(String courseCode) {
        return baseMapper.selectByCourseCode(courseCode);
    }

    @Override
    public List<Question> listByLessonNo(String lessonNo) {
        return baseMapper.selectByLessonNo(lessonNo);
    }

    @Override
    public List<Question> searchByKeyword(String keyword) {
        return baseMapper.selectByKeyword(keyword);
    }

    @Override
    public List<Question> filterQuestions(String courseCode, String lessonNo, String knowledgePointId,
                                          String type, Integer difficulty, String keyword) {
        return baseMapper.selectByFilter(courseCode, lessonNo, knowledgePointId, type, difficulty, keyword);
    }

    @Override
    public List<Question> generatePaper(String courseCode, PaperGenerateRequest request) {
        if (request == null) throw new IllegalArgumentException("组卷参数不能为空");
        int totalCount = request.getCount() == null ? 10 : request.getCount();
        if (totalCount <= 0) throw new IllegalArgumentException("题目数量必须大于 0");

        List<Question> candidates = filterCandidates(listByCourseCode(courseCode), request);
        if (candidates.isEmpty()) throw new IllegalArgumentException("没有符合条件的题目");

        List<Question> selected = selectQuestions(candidates, request, totalCount);
        if (selected.size() < totalCount) {
            throw new IllegalArgumentException("符合条件的题目不足，目标 " + totalCount + " 道，当前 " + selected.size() + " 道");
        }
        validateTargetScore(selected, request.getTargetScore());
        return selected;
    }

    private List<Question> selectQuestions(List<Question> candidates, PaperGenerateRequest request, int totalCount) {
        Map<String, Integer> knowledgeCounts = request.getKnowledgePointIdCounts();
        if (knowledgeCounts != null && !knowledgeCounts.isEmpty()) {
            return generateByKnowledgePointCounts(candidates, request, knowledgeCounts);
        }
        if (request.getTypeCounts() != null && !request.getTypeCounts().isEmpty()) {
            return generateByTypeCounts(candidates, request);
        }
        if (request.getDifficultyRatios() != null && !request.getDifficultyRatios().isEmpty()) {
            return generateByDifficultyRatios(candidates, request, totalCount);
        }
        return selectByStrategy(candidates, request.getStrategy(), totalCount, request.getKnowledgePointIds());
    }

    private List<Question> filterCandidates(List<Question> all, PaperGenerateRequest request) {
        Set<String> types = toSet(request.getTypes());
        Set<String> knowledgePointIds = toSet(request.getKnowledgePointIds());
        int min = request.getDifficultyMin() == null ? 1 : request.getDifficultyMin();
        int max = request.getDifficultyMax() == null ? 5 : request.getDifficultyMax();
        return all.stream()
                .filter(q -> types.isEmpty() || types.contains(q.getType()))
                .filter(q -> knowledgePointIds.isEmpty() || knowledgePointIds.contains(q.getKnowledgePointId()))
                .filter(q -> q.getDifficulty() == null || (q.getDifficulty() >= min && q.getDifficulty() <= max))
                .collect(Collectors.toList());
    }

    private List<Question> generateByTypeCounts(List<Question> candidates, PaperGenerateRequest request) {
        LinkedHashSet<Question> selected = new LinkedHashSet<>();
        int targetCount = request.getTypeCounts().values().stream()
                .filter(Objects::nonNull)
                .filter(count -> count > 0)
                .mapToInt(Integer::intValue)
                .sum();
        request.getTypeCounts().forEach((type, count) -> {
            if (type == null || count == null || count <= 0) return;
            List<Question> typed = candidates.stream()
                    .filter(q -> type.equals(q.getType()))
                    .collect(Collectors.toList());
            selected.addAll(selectByStrategy(typed, request.getStrategy(), count, request.getKnowledgePointIds()));
        });
        if (selected.isEmpty()) throw new IllegalArgumentException("没有符合题型数量要求的题目");
        if (selected.size() < targetCount) {
            throw new IllegalArgumentException("符合题型数量要求的题目不足，目标 " + targetCount + " 道，当前 " + selected.size() + " 道");
        }
        return new ArrayList<>(selected);
    }

    private List<Question> generateByKnowledgePointCounts(List<Question> candidates, PaperGenerateRequest request, Map<String, Integer> knowledgeCounts) {
        LinkedHashSet<Question> selected = new LinkedHashSet<>();
        int targetCount = knowledgeCounts.values().stream()
                .filter(Objects::nonNull)
                .filter(count -> count > 0)
                .mapToInt(Integer::intValue)
                .sum();
        knowledgeCounts.forEach((knowledgePointId, count) -> {
            if (knowledgePointId == null || count == null || count <= 0) return;
            String normalized = normalize(knowledgePointId);
            List<Question> matched = candidates.stream()
                    .filter(q -> normalized.equals(normalize(q.getKnowledgePointId())))
                    .collect(Collectors.toList());
            selected.addAll(selectByStrategy(matched, request.getStrategy(), count, List.of(normalized)));
        });
        if (selected.isEmpty()) throw new IllegalArgumentException("没有符合知识点题量要求的题目");
        if (selected.size() < targetCount) {
            throw new IllegalArgumentException("符合知识点题量要求的题目不足，目标 " + targetCount + " 道，当前 " + selected.size() + " 道");
        }
        return new ArrayList<>(selected);
    }

    private List<Question> generateByDifficultyRatios(List<Question> candidates, PaperGenerateRequest request, int totalCount) {
        LinkedHashSet<Question> selected = new LinkedHashSet<>();
        Map<Integer, Integer> counts = toCounts(request.getDifficultyRatios(), totalCount);
        counts.forEach((difficulty, count) -> {
            if (difficulty == null || count == null || count <= 0) return;
            List<Question> matched = candidates.stream()
                    .filter(q -> difficulty.equals(q.getDifficulty() == null ? 3 : q.getDifficulty()))
                    .collect(Collectors.toList());
            selected.addAll(selectRandom(matched, count));
        });
        if (selected.isEmpty()) throw new IllegalArgumentException("没有符合难度比例要求的题目");
        if (selected.size() < totalCount) {
            throw new IllegalArgumentException("符合难度比例要求的题目不足，目标 " + totalCount + " 道，当前 " + selected.size() + " 道");
        }
        return new ArrayList<>(selected);
    }

    private List<Question> selectByStrategy(List<Question> candidates, String strategy, int count, List<String> knowledgePoints) {
        if (candidates.isEmpty()) return List.of();
        String mode = strategy == null || strategy.isBlank() ? "random" : strategy;
        if ("knowledge".equals(mode)) return selectRoundRobin(candidates, count, q -> normalize(q.getKnowledgePointId()), knowledgePoints);
        if ("difficulty".equals(mode)) return selectDifficultyBalanced(candidates, count);
        return selectRandom(candidates, count);
    }

    private List<Question> selectRandom(List<Question> candidates, int count) {
        List<Question> copy = new ArrayList<>(candidates);
        Collections.shuffle(copy);
        return copy.stream().limit(count).collect(Collectors.toList());
    }

    private List<Question> selectDifficultyBalanced(List<Question> candidates, int count) {
        List<String> levels = candidates.stream()
                .map(q -> String.valueOf(q.getDifficulty() == null ? 3 : q.getDifficulty()))
                .distinct()
                .sorted(Comparator.comparingInt(Integer::parseInt))
                .collect(Collectors.toList());
        return selectRoundRobin(candidates, count, q -> String.valueOf(q.getDifficulty() == null ? 3 : q.getDifficulty()), levels);
    }

    private List<Question> selectRoundRobin(List<Question> candidates, int count,
                                            java.util.function.Function<Question, String> classifier,
                                            Collection<String> preferredOrder) {
        Map<String, List<Question>> groups = new LinkedHashMap<>();
        if (preferredOrder != null) {
            preferredOrder.stream().map(this::normalize).filter(s -> !s.isEmpty()).forEach(k -> groups.put(k, new ArrayList<>()));
        }
        for (Question q : candidates) {
            groups.computeIfAbsent(classifier.apply(q), k -> new ArrayList<>()).add(q);
        }
        groups.values().forEach(Collections::shuffle);

        List<Question> selected = new ArrayList<>();
        while (selected.size() < count) {
            boolean picked = false;
            for (List<Question> group : groups.values()) {
                if (!group.isEmpty() && selected.size() < count) {
                    selected.add(group.remove(0));
                    picked = true;
                }
            }
            if (!picked) break;
        }
        return selected;
    }

    private Set<String> toSet(List<String> values) {
        if (values == null) return Set.of();
        return values.stream().filter(Objects::nonNull).map(this::normalize).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    private Map<Integer, Integer> toCounts(Map<Integer, Integer> ratios, int totalCount) {
        int ratioSum = ratios.values().stream()
                .filter(Objects::nonNull)
                .filter(value -> value > 0)
                .mapToInt(Integer::intValue)
                .sum();
        if (ratioSum <= 0) throw new IllegalArgumentException("难度比例必须大于 0");

        Map<Integer, Integer> counts = new LinkedHashMap<>();
        int allocated = 0;
        List<Integer> levels = ratios.keySet().stream().filter(Objects::nonNull).sorted().collect(Collectors.toList());
        for (Integer level : levels) {
            Integer ratio = ratios.get(level);
            if (ratio == null || ratio <= 0) continue;
            int count = (int) Math.floor(totalCount * ratio * 1.0 / ratioSum);
            counts.put(level, count);
            allocated += count;
        }
        int remaining = totalCount - allocated;
        for (Integer level : levels) {
            if (remaining <= 0) break;
            if (ratios.get(level) == null || ratios.get(level) <= 0) continue;
            counts.put(level, counts.getOrDefault(level, 0) + 1);
            remaining--;
        }
        return counts;
    }

    private void validateTargetScore(List<Question> selected, Integer targetScore) {
        if (targetScore == null) return;
        if (targetScore <= 0) throw new IllegalArgumentException("目标总分必须大于 0");
        int actual = selected.stream().mapToInt(q -> q.getScore() == null ? 0 : q.getScore()).sum();
        if (actual != targetScore) {
            throw new IllegalArgumentException("当前组卷总分为 " + actual + " 分，不等于目标总分 " + targetScore + " 分，请调整题量或题目分值");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
