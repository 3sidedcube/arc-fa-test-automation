package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.Map;

/**
 * One content block inside a {@link LessonCard}. Observed types:
 * {@code paragraph}, {@code sectionHeading}, {@code unorderedListItemComponent}.
 * The shape is consistent enough that title + description (both localized
 * maps) cover all variants.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LessonContentComponent {

    public String type;
    public ComponentData data = new ComponentData();

    /**
     * sectionHeading uses {@code data.name}; paragraphs use {@code data.title};
     * bullets use {@code data.title}. Returns the en-US value of whichever is
     * populated.
     */
    public String titleEn() {
        if (data == null) return null;
        if (data.name != null && data.name.get("en-US") != null) return data.name.get("en-US");
        if (data.title != null) return data.title.get("en-US");
        return null;
    }

    public String descriptionEn() {
        return data == null || data.description == null ? null : data.description.get("en-US");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComponentData {
        public Map<String, String> name = Collections.emptyMap();        // sectionHeading
        public Map<String, String> title = Collections.emptyMap();       // paragraph / bullets
        public Map<String, String> description = Collections.emptyMap(); // paragraph
    }
}
