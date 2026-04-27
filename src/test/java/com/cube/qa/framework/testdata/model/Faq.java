package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.Map;

/**
 * A single FAQ inside a {@link LearnTopicDetail}. The CDN wraps each entry in
 * {@code {type: "faq", data: {...}}} — we only care about the {@code data}
 * payload here. Both fields are localized per-locale maps; helpers default to
 * en-US.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Faq {

    public String type;

    public FaqData data = new FaqData();

    public String questionEn() {
        return data == null || data.title == null ? null : data.title.get("en-US");
    }

    public String answerEn() {
        return data == null || data.description == null ? null : data.description.get("en-US");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FaqData {
        public Map<String, String> title = Collections.emptyMap();
        public Map<String, String> description = Collections.emptyMap();
    }
}
