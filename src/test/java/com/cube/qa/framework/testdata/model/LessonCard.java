package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A single card inside a {@link Lesson}. The CDN wraps every card in
 * {@code {type: "lessonCard", data: {...}}} — only the {@code data} payload
 * carries anything we test against.
 *
 * <p>Cards may carry optional {@link #media} (image or video) and a list of
 * {@link LessonContentComponent content components} (paragraphs, section
 * headings, unordered list items).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LessonCard {

    public String type;
    public CardData data = new CardData();

    public String titleEn() {
        return data == null || data.title == null ? null : data.title.get("en-US");
    }

    /** Convenience: true if this card has an {@code imageComponent} media block. */
    public boolean hasImage() {
        return data != null && data.media != null && "imageComponent".equals(data.media.type);
    }

    /** Convenience: true if this card has a {@code videoComponent} media block. */
    public boolean hasVideo() {
        return data != null && data.media != null && "videoComponent".equals(data.media.type);
    }

    /** Convenience: true if this card carries any unordered-list-item components. */
    public boolean hasBullets() {
        if (data == null || data.content == null) return false;
        for (LessonContentComponent c : data.content) {
            if (c != null && "unorderedListItemComponent".equals(c.type)) return true;
        }
        return false;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CardData {
        public Map<String, String> title = Collections.emptyMap();
        public LessonMedia media;
        public List<LessonContentComponent> content = Collections.emptyList();
    }
}
