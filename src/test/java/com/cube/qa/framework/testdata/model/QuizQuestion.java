package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One question inside a {@link QuizDetail}.
 *
 * <p>The CDN wraps the actual question payload in a {@code data} block so
 * different question types can share a common envelope. We keep {@code data}
 * loosely-typed (Map) and project typed views via the helper methods —
 * mirrors the {@link ArticleComponent}/{@link EmergencyStep} split.
 *
 * <p>For text-selection questions, {@code data} carries:
 * <ul>
 *   <li>{@code title} — localized question prompt</li>
 *   <li>{@code failMessage} — localized feedback shown on incorrect</li>
 *   <li>{@code answers} — list of {@code {title, isCorrect}} answer rows</li>
 *   <li>{@code uiType} — {@code DEFAULT} or {@code FILL_IN_THE_BLANK}</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizQuestion {

    public String type;
    public Map<String, Object> data = Collections.emptyMap();

    /** {@code DEFAULT}, {@code FILL_IN_THE_BLANK}, … or {@code null}. */
    public String uiType() {
        Object v = data == null ? null : data.get("uiType");
        return v == null ? null : v.toString();
    }

    public String titleEn() {
        return pluckEn(data == null ? null : data.get("title"));
    }

    /** Bottom-sheet copy shown when the user selects the wrong answer. */
    public String failMessageEn() {
        return pluckEn(data == null ? null : data.get("failMessage"));
    }

    /** Decoded answers in declaration order. Empty list if the data is missing. */
    @SuppressWarnings("unchecked")
    public List<QuizAnswer> answers() {
        if (data == null) return Collections.emptyList();
        Object raw = data.get("answers");
        if (!(raw instanceof List)) return Collections.emptyList();
        List<QuizAnswer> out = new ArrayList<>();
        for (Object row : (List<Object>) raw) {
            if (!(row instanceof Map)) continue;
            Map<String, Object> map = (Map<String, Object>) row;
            QuizAnswer a = new QuizAnswer();
            a.titleEn = pluckEn(map.get("title"));
            Object correct = map.get("isCorrect");
            a.isCorrect = (correct instanceof Boolean) && (Boolean) correct;
            out.add(a);
        }
        return out;
    }

    public int correctAnswerCount() {
        int n = 0;
        for (QuizAnswer a : answers()) if (a.isCorrect) n++;
        return n;
    }

    public List<QuizAnswer> correctAnswers() {
        List<QuizAnswer> out = new ArrayList<>();
        for (QuizAnswer a : answers()) if (a.isCorrect) out.add(a);
        return out;
    }

    public List<QuizAnswer> incorrectAnswers() {
        List<QuizAnswer> out = new ArrayList<>();
        for (QuizAnswer a : answers()) if (!a.isCorrect) out.add(a);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static String pluckEn(Object localized) {
        if (localized instanceof Map) {
            Object v = ((Map<String, Object>) localized).get("en-US");
            return v == null ? null : v.toString();
        }
        return null;
    }
}
