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
    public List<Question> generatePaper(String courseCode, PaperGenerateRequest request) {
        if (request == null) throw new IllegalArgumentException("组卷参数不能为空");
        int totalCount = request.getCount() == null ? 10 : request.getCount();
        if (totalCount <= 0) throw new IllegalArgumentException("题目数量必须大于 0");

        List<Question> candidates = filterCandidates(listByCourseCode(courseCode), request);
        if (candidates.isEmpty()) throw new IllegalArgumentException("没有符合条件的题目");

        if (request.getTypeCounts() != null && !request.getTypeCounts().isEmpty()) {
            return generateByTypeCounts(candidates, request);
        }
        return selectByStrategy(candidates, request.getStrategy(), totalCount, request.getKnowledgePoints());
    }

    private List<Question> filterCandidates(List<Question> all, PaperGenerateRequest request) {
        Set<String> types = toSet(request.getTypes());
        Set<String> knowledgePoints = toSet(request.getKnowledgePoints());
        int min = request.getDifficultyMin() == null ? 1 : request.getDifficultyMin();
        int max = request.getDifficultyMax() == null ? 5 : request.getDifficultyMax();
        return all.stream()
                .filter(q -> types.isEmpty() || types.contains(q.getType()))
                .filter(q -> knowledgePoints.isEmpty() || knowledgePoints.contains(q.getKnowledgePoint()))
                .filter(q -> q.getDifficulty() == null || (q.getDifficulty() >= min && q.getDifficulty() <= max))
                .collect(Collectors.toList());
    }

    private List<Question> generateByTypeCounts(List<Question> candidates, PaperGenerateRequest request) {
        LinkedHashSet<Question> selected = new LinkedHashSet<>();
        request.getTypeCounts().forEach((type, count) -> {
            if (type == null || count == null || count <= 0) return;
            List<Question> typed = candidates.stream()
                    .filter(q -> type.equals(q.getType()))
                    .collect(Collectors.toList());
            selected.addAll(selectByStrategy(typed, request.getStrategy(), count, request.getKnowledgePoints()));
        });
        if (selected.isEmpty()) throw new IllegalArgumentException("没有符合题型数量要求的题目");
        return new ArrayList<>(selected);
    }

    private List<Question> selectByStrategy(List<Question> candidates, String strategy, int count, List<String> knowledgePoints) {
        if (candidates.isEmpty()) return List.of();
        String mode = strategy == null || strategy.isBlank() ? "random" : strategy;
        if ("knowledge".equals(mode)) return selectRoundRobin(candidates, count, q -> normalize(q.getKnowledgePoint()), knowledgePoints);
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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
