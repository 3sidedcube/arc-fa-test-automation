package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.Map;

/**
 * Optional media block on a {@link LessonCard}. {@code type} is either
 * {@code imageComponent} or {@code videoComponent}; the {@code data} fields
 * relevant to each are union-merged here so a single POJO covers both.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LessonMedia {

    public String type;
    public MediaData data = new MediaData();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MediaData {
        // imageComponent
        public String imageId;
        public boolean displayInLandscape;

        // videoComponent
        public Map<String, String> title = Collections.emptyMap();
        public Map<String, String> url = Collections.emptyMap();
        public Map<String, String> thumbnailId = Collections.emptyMap();
    }
}
