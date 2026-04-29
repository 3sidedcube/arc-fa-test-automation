package com.cube.qa.framework.testdata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.Map;

/**
 * One entry from {@link ArticleDetail#components}. Heterogeneous payload —
 * the {@code type} discriminator says how to interpret {@code data}. Kept
 * loosely-typed (Map) here so adding a new component type doesn't require
 * a model change before tests can read it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleComponent {

    public String type;
    public Map<String, Object> data = Collections.emptyMap();
}
