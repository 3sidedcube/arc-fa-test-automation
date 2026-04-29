package com.cube.qa.framework.testdata.loader;

import com.cube.qa.framework.testdata.model.Article;
import com.cube.qa.framework.testdata.model.ArticleDetail;
import com.cube.qa.framework.testdata.model.LearnTopic;
import com.cube.qa.framework.testdata.model.LearnTopicDetail;
import com.cube.qa.framework.testdata.model.PersonalizationTag;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Suite-scoped loader that fetches the Cube CDN content bundle once per JVM
 * and exposes typed lookups to tests. Mirrors {@link UserDataLoader}: tests
 * don't instantiate it — {@link #setEnvironment(String)} is called from
 * BaseTest and the rest happens lazily on first lookup.
 *
 * <p>Content varies per environment (dev/staging/prod), so tests should read
 * their expectations from the bundle rather than hardcode strings that will
 * drift as content is renamed or promoted between envs.
 *
 * <p>One fetch per JVM is deliberate — a TestNG suite run is short-lived and
 * cache-invalidation mid-suite would just introduce flake.
 */
public class ContentBundleLoader {

    private static final Map<String, String> ENV_BASE_URLS = Map.of(
            "dev", "https://dev.arc.firstaid.cube-cdn.com/v3/",
            "staging", "https://staging.arc.firstaid.cube-cdn.com/v3/",
            "prod", "https://prod.arc.firstaid.cube-cdn.com/v3/"
    );

    // Shared across all fetches — unlike UserDataLoader's per-call mapper.
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static String environment;
    private static String baseUrl;

    // Lazily populated on first lookup. Null = not yet fetched.
    private static Manifest manifest;
    private static Map<String, PersonalizationTag> tagsById;
    private static Map<String, LearnTopic> topicsById;
    private static Map<String, Article> articlesById;
    // app-strings.json: {"_KEY": {"en-US": "value", "es-US": "..."}, ...}
    private static Map<String, Map<String, String>> appStringsByKey;
    // Per-topic detail JSON is fetched on demand and cached per id. The
    // top-level manifest doesn't list these; the URL is `<base>learn-topics/{id}.json`.
    private static final Map<String, LearnTopicDetail> topicDetailsById = new LinkedHashMap<>();
    // Per-article body, fetched on demand from `<base>articles/{id}.json`.
    private static final Map<String, ArticleDetail> articleDetailsById = new LinkedHashMap<>();

    private ContentBundleLoader() {
        // Static-only.
    }

    /**
     * Called from BaseTest after the env is resolved. Does not fetch —
     * the first actual lookup triggers the HTTP call.
     */
    public static void setEnvironment(String env) {
        if (env == null) {
            throw new IllegalArgumentException("env must not be null");
        }
        String normalized = env.toLowerCase();
        String url = ENV_BASE_URLS.get(normalized);
        if (url == null) {
            throw new IllegalArgumentException(
                    "Unknown env '" + env + "'. Expected one of " + ENV_BASE_URLS.keySet());
        }
        // Reset cache if env changes mid-JVM (shouldn't happen in practice,
        // but keeps behavior sane if a suite ever re-wires config).
        if (!normalized.equals(environment)) {
            manifest = null;
            tagsById = null;
            topicsById = null;
            articlesById = null;
            appStringsByKey = null;
            topicDetailsById.clear();
            articleDetailsById.clear();
        }
        environment = normalized;
        baseUrl = url;
    }

    // ---- Public API ---------------------------------------------------------

    public static PersonalizationTag tag(String id) {
        return ensureTagsLoaded().get(id);
    }

    public static Collection<PersonalizationTag> allTags() {
        return ensureTagsLoaded().values();
    }

    public static LearnTopic topic(String id) {
        return ensureTopicsLoaded().get(id);
    }

    public static Collection<LearnTopic> allTopics() {
        return ensureTopicsLoaded().values();
    }

    /** First topic whose {@code personalizationTagIds} contains {@code tagId}. */
    public static LearnTopic topicByTag(String tagId) {
        for (LearnTopic t : ensureTopicsLoaded().values()) {
            if (t.personalizationTagIds != null && t.personalizationTagIds.contains(tagId)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Per-topic detail (description, lessons, related-article ids, faqs).
     * Fetched lazily on first call for that id and cached for the JVM.
     */
    public static synchronized LearnTopicDetail topicDetail(String id) {
        requireEnvSet();
        LearnTopicDetail cached = topicDetailsById.get(id);
        if (cached != null) return cached;
        String url = baseUrl + "learn-topics/" + id + ".json";
        LearnTopicDetail detail = fetchJson(url, new TypeReference<LearnTopicDetail>() {});
        if (detail.id == null || detail.id.isEmpty()) detail.id = id;
        topicDetailsById.put(id, detail);
        return detail;
    }

    public static Article article(String id) {
        return ensureArticlesLoaded().get(id);
    }

    /**
     * Per-article body (components / emergency steps / images). Fetched
     * lazily on first call for that id and cached for the JVM. Mirrors
     * {@link #topicDetail(String)}.
     */
    public static synchronized ArticleDetail articleDetail(String id) {
        requireEnvSet();
        ArticleDetail cached = articleDetailsById.get(id);
        if (cached != null) return cached;
        String url = baseUrl + "articles/" + id + ".json";
        ArticleDetail detail = fetchJson(url, new TypeReference<ArticleDetail>() {});
        if (detail.id == null || detail.id.isEmpty()) detail.id = id;
        articleDetailsById.put(id, detail);
        return detail;
    }

    /**
     * First emergency article (by tabLocation = EMERGENCY) whose en-US title
     * matches {@code titleEn} case-insensitively, or {@code null}. Convenience
     * for tests that target a specific emergency topic by name.
     */
    public static Article emergencyArticleByTitle(String titleEn) {
        if (titleEn == null) return null;
        for (Article a : articlesForTab("EMERGENCY")) {
            if (!"emergencyArticle".equals(a.type)) continue;
            if (titleEn.equalsIgnoreCase(a.titleEn())) return a;
        }
        return null;
    }

    /**
     * Looks up an app-string by key (e.g. {@code _LESSON_COMPLETE_DESCRIPTION_1}),
     * returning the en-US value or {@code null} if the key is missing.
     */
    public static String appString(String key) {
        Map<String, String> localized = ensureAppStringsLoaded().get(key);
        return localized == null ? null : localized.get("en-US");
    }

    /** Raw localized map for an app-string key, or {@code null} if absent. */
    public static Map<String, String> appStringLocalized(String key) {
        return ensureAppStringsLoaded().get(key);
    }

    public static Collection<Article> articlesForTab(String tabLocation) {
        List<Article> out = new ArrayList<>();
        for (Article a : ensureArticlesLoaded().values()) {
            if (a.tabLocation != null && a.tabLocation.contains(tabLocation)) {
                out.add(a);
            }
        }
        return out;
    }

    // ---- Lazy loaders -------------------------------------------------------

    private static synchronized Manifest ensureManifestLoaded() {
        if (manifest == null) {
            requireEnvSet();
            ManifestEnvelope envelope = fetchJson(
                    baseUrl + "manifest.json", new TypeReference<ManifestEnvelope>() {});
            manifest = envelope == null ? new Manifest() : envelope.data;
            if (manifest == null) {
                throw new RuntimeException("Manifest 'data' block missing (env=" + environment + ")");
            }
        }
        return manifest;
    }

    private static synchronized Map<String, PersonalizationTag> ensureTagsLoaded() {
        if (tagsById == null) {
            String url = resolveSubManifestUrl("personalization_tags");
            List<PersonalizationTag> list =
                    fetchJson(url, new TypeReference<List<PersonalizationTag>>() {});
            Map<String, PersonalizationTag> out = new LinkedHashMap<>();
            for (PersonalizationTag t : list) {
                out.put(t.id, t);
            }
            tagsById = Collections.unmodifiableMap(out);
        }
        return tagsById;
    }

    private static synchronized Map<String, LearnTopic> ensureTopicsLoaded() {
        if (topicsById == null) {
            String url = resolveSubManifestUrl("learn_topic_manifest");
            Map<String, LearnTopic> raw =
                    fetchJson(url, new TypeReference<Map<String, LearnTopic>>() {});
            // Populate id from map key (CDN doesn't repeat it in the value).
            raw.forEach((id, topic) -> topic.id = id);
            topicsById = Collections.unmodifiableMap(raw);
        }
        return topicsById;
    }

    private static synchronized Map<String, Map<String, String>> ensureAppStringsLoaded() {
        if (appStringsByKey == null) {
            String url = resolveSubManifestUrl("app_strings");
            Map<String, Map<String, String>> raw =
                    fetchJson(url, new TypeReference<Map<String, Map<String, String>>>() {});
            appStringsByKey = Collections.unmodifiableMap(raw);
        }
        return appStringsByKey;
    }

    private static synchronized Map<String, Article> ensureArticlesLoaded() {
        if (articlesById == null) {
            String url = resolveSubManifestUrl("article_manifest");
            Map<String, Article> raw =
                    fetchJson(url, new TypeReference<Map<String, Article>>() {});
            raw.forEach((id, article) -> article.id = id);
            articlesById = Collections.unmodifiableMap(raw);
        }
        return articlesById;
    }

    // ---- Helpers ------------------------------------------------------------

    private static String resolveSubManifestUrl(String key) {
        Manifest m = ensureManifestLoaded();
        ManifestEntry entry = m == null ? null : m.get(key);
        if (entry == null || entry.url == null) {
            throw new RuntimeException(
                    "Manifest missing entry for '" + key + "' (env=" + environment + ")");
        }
        // The manifest may list absolute or relative URLs — handle both.
        return URI.create(baseUrl).resolve(entry.url).toString();
    }

    private static <T> T fetchJson(String url, TypeReference<T> type) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new RuntimeException(
                        "CDN fetch " + url + " returned HTTP " + res.statusCode());
            }
            return MAPPER.readValue(res.body(), type);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to fetch/parse " + url, e);
        }
    }

    private static void requireEnvSet() {
        if (environment == null || baseUrl == null) {
            throw new IllegalStateException(
                    "ContentBundleLoader.setEnvironment(...) must be called before use");
        }
    }

    // ---- Internal manifest shape -------------------------------------------
    // manifest.json wraps the keyed sub-manifest entries inside a "data" block
    // alongside "meta". We bind the envelope, then treat `data` as a map of
    // {name -> {timestamp,url}}.

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ManifestEnvelope {
        public Manifest data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Manifest extends LinkedHashMap<String, ManifestEntry> {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ManifestEntry {
        public long timestamp;
        public String url;
    }
}
