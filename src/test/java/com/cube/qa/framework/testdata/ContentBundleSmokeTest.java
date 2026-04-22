package com.cube.qa.framework.testdata;

import com.cube.qa.framework.testdata.loader.ContentBundleLoader;
import com.cube.qa.framework.testdata.model.Article;
import com.cube.qa.framework.testdata.model.LearnTopic;
import com.cube.qa.framework.testdata.model.PersonalizationTag;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collection;

/**
 * Canary for the CDN content bundle. Runs without a driver and without
 * extending BaseTest — this is a pure HTTP/JSON sanity check so a broken
 * bundle shape or env URL surfaces before real Appium tests spin up.
 *
 * <p>Env resolution: reads the {@code env} system property
 * (e.g. {@code -Denv=prod}), defaulting to {@code staging}. This mirrors how
 * ConfigLoader picks up env overrides, without the full TestNG parameter
 * machinery.
 *
 * <p>Run standalone:
 * <pre>
 *   mvn test -Dtest=ContentBundleSmokeTest -Denv=staging
 * </pre>
 */
public class ContentBundleSmokeTest {

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        String env = System.getProperty("env", "staging");
        ContentBundleLoader.setEnvironment(env);
    }

    @Test(description = "Personalization tags resolve from the CDN bundle", groups = {"smoke"})
    public void personalizationTagsResolve() {
        Collection<PersonalizationTag> tags = ContentBundleLoader.allTags();
        Assert.assertNotNull(tags, "allTags() should not return null");
        Assert.assertFalse(tags.isEmpty(), "Expected at least one personalization tag");

        // Content IDs differ across envs — don't anchor on a specific ID. Instead
        // pick any tag from what we fetched and verify the loader populated it
        // end-to-end (id + localized title).
        PersonalizationTag any = tags.iterator().next();
        Assert.assertNotNull(any.id, "Tag id should be populated");
        Assert.assertNotNull(any.titleEn(), "Tag should have an en-US title");

        // tag(id) round-trip must resolve the same object.
        Assert.assertSame(ContentBundleLoader.tag(any.id), any,
                "tag(id) should return the same instance as the one surfaced by allTags()");
    }

    @Test(description = "Learn topics resolve and expose lessonIds", groups = {"smoke"})
    public void learnTopicsResolve() {
        Collection<LearnTopic> topics = ContentBundleLoader.allTopics();
        Assert.assertNotNull(topics, "allTopics() should not return null");
        Assert.assertFalse(topics.isEmpty(), "Expected at least one learn topic");

        boolean anyWithLessons = topics.stream()
                .anyMatch(t -> t.lessonIds != null && !t.lessonIds.isEmpty());
        Assert.assertTrue(anyWithLessons,
                "Expected at least one learn topic with a non-empty lessonIds list");

        LearnTopic first = topics.iterator().next();
        Assert.assertNotNull(first.id, "Loader should populate topic.id from the map key");
        Assert.assertNotNull(first.titleEn(), "Topic should have an en-US title");
    }

    @Test(description = "Article manifest resolves", groups = {"smoke"})
    public void articlesResolve() {
        Collection<Article> articles = ContentBundleLoader.articlesForTab("learn");
        // We don't assert a specific count — just that the lookup path works
        // end-to-end and the underlying manifest deserializes cleanly.
        Assert.assertNotNull(articles, "articlesForTab() should not return null");

        Article any = ContentBundleLoader.allTopics().isEmpty()
                ? null
                : ContentBundleLoader.article(pickAnyArticleId());
        if (any != null) {
            Assert.assertNotNull(any.id, "Loader should populate article.id from the map key");
        }
    }

    /** Pick any article ID from the bundle so we exercise the article() lookup path. */
    private String pickAnyArticleId() {
        // Lean on articlesForTab with a wildcard-ish tab; if empty, fall back to
        // grabbing the first article via a fresh lookup against a common tab.
        Collection<Article> learn = ContentBundleLoader.articlesForTab("learn");
        if (!learn.isEmpty()) return learn.iterator().next().id;
        Collection<Article> giveCare = ContentBundleLoader.articlesForTab("giveCare");
        if (!giveCare.isEmpty()) return giveCare.iterator().next().id;
        return null;
    }
}
