package com.neu.CoursePlatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.entity.Question;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuestionAnswerEvaluator {
    private static final List<String> LETTERS = Arrays.stream("ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("")).toList();
    private static final Pattern MARKED = Pattern.compile("^\\s*([A-Za-z])\\s*(?:[.)]|[:：]|、|\\uFF0E)\\s*(.*)$");
    private static final Pattern SPACED = Pattern.compile("^\\s*([A-Za-z])\\s+(.+)$");
    private final ObjectMapper objectMapper;

    public QuestionAnswerEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isAutoGradable(Question question) {
        return question != null && Set.of("single", "multi", "fill").contains(question.getType());
    }

    public boolean isCorrect(Question question, Object response) {
        if (!isAutoGradable(question) || question.getAnswer() == null) return false;
        if ("single".equals(question.getType())) {
            String expected = canonicalChoice(question.getAnswer(), question);
            return !expected.isBlank() && expected.equals(canonicalChoice(response, question));
        }
        if ("multi".equals(question.getType())) {
            Set<String> expected = choiceSet(question.getAnswer(), question);
            return !expected.isEmpty() && expected.equals(choiceSet(response, question));
        }
        return normalize(response).equals(normalize(question.getAnswer()));
    }

    private Set<String> choiceSet(Object value, Question question) {
        Set<String> result = new TreeSet<>();
        for (String token : split(value)) {
            String canonical = canonicalChoice(token, question);
            if (!canonical.isBlank()) result.add(canonical);
        }
        return result;
    }

    private List<String> split(Object value) {
        if (value == null) return List.of();
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).map(String::trim).filter(item -> !item.isBlank()).toList();
        }
        return Arrays.stream(String.valueOf(value).split("[,，;；]"))
                .map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private String canonicalChoice(Object value, Question question) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isBlank()) return "";
        if (text.matches("[A-Za-z]")) return "option:" + text.toUpperCase(Locale.ROOT);
        MarkedOption marked = markedOption(text);
        if (marked != null) return "option:" + marked.letter();
        String normalized = normalize(text);
        for (OptionEntry option : optionEntries(question)) {
            if (option.aliases().contains(normalized)) return "option:" + option.letter();
        }
        return "text:" + normalized;
    }

    private List<OptionEntry> optionEntries(Question question) {
        String options = question.getOptions();
        if (options == null || options.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(options);
            List<OptionEntry> result = new ArrayList<>();
            if (root.isArray()) {
                for (int i = 0; i < root.size(); i++) result.add(optionEntry(letter(i), root.get(i).asText(""), ""));
                return result;
            }
            if (root.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                int index = 0;
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    result.add(optionEntry(letter(index++), field.getValue().asText(""), field.getKey()));
                }
                return result;
            }
        } catch (Exception ignored) {
            // Legacy newline options are handled below.
        }
        List<OptionEntry> result = new ArrayList<>();
        for (String line : options.split("\\R")) {
            if (!line.isBlank()) result.add(optionEntry(letter(result.size()), line, ""));
        }
        return result;
    }

    private OptionEntry optionEntry(String fallback, String raw, String key) {
        MarkedOption marked = markedOption(raw);
        String keyLetter = key != null && key.matches("[A-Za-z]") ? key.toUpperCase(Locale.ROOT) : "";
        String optionLetter = marked != null ? marked.letter() : !keyLetter.isBlank() ? keyLetter : fallback;
        String display = marked != null && !marked.rest().isBlank() ? marked.rest() : raw;
        Set<String> aliases = new TreeSet<>();
        addAlias(aliases, raw);
        addAlias(aliases, display);
        addAlias(aliases, key);
        addAlias(aliases, optionLetter + ". " + display);
        addAlias(aliases, optionLetter + "、" + display);
        return new OptionEntry(optionLetter, aliases);
    }

    private MarkedOption markedOption(String value) {
        Matcher matcher = MARKED.matcher(value);
        if (!matcher.matches()) matcher = SPACED.matcher(value);
        return matcher.matches()
                ? new MarkedOption(matcher.group(1).toUpperCase(Locale.ROOT), matcher.group(2).trim())
                : null;
    }

    private void addAlias(Set<String> aliases, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) aliases.add(normalized);
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).replace('\u3000', ' ').trim()
                .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String letter(int index) {
        return index >= 0 && index < LETTERS.size() ? LETTERS.get(index) : String.valueOf(index + 1);
    }

    private record OptionEntry(String letter, Set<String> aliases) {}
    private record MarkedOption(String letter, String rest) {}
}
