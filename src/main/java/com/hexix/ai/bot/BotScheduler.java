package com.hexix.ai.bot;


import com.hexix.mastodon.PublicMastodonPostEntity;
import io.quarkus.panache.common.Sort;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BotScheduler {

    private static final Logger LOG = Logger.getLogger(BotScheduler.class);

    @Inject
    VikiAiService vikiAiService;

    /**
     * This method is executed automatically by the scheduler.
     * It finds the most relevant, uncommented post and generates a response from Viki.
     * The cron expression "0 0/15 * * * ?" means "every 15 minutes".
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void triggerBot() {
        LOG.info("🤖 Bot Scheduler triggered. Looking for a new post to comment on...");

        // 1. Find the best post that Viki hasn't commented on yet.
        PublicMastodonPostEntity postToComment = PublicMastodonPostEntity.find(
                "vikiCommented = false and cosDistance is not null",
                Sort.by("cosDistance").descending()
        ).firstResult();

        if (postToComment == null) {
            LOG.info("No new posts found for Viki to comment on. Will try again later.");
            return;
        }

        LOG.infof("Found a post to comment on with ID: %s and cosDistance: %f",
                postToComment.getMastodonId(), postToComment.getCosDistance());

        // 2. Create the topic for the AI from the post's content.
        // We combine the post text and any text extracted from URLs.
        String topic = (postToComment.getPostText() != null ? postToComment.getPostText() : "") +
                "\n" +
                (postToComment.getUrlText() != null ? postToComment.getUrlText() : "");

        if (topic.isBlank()) {
            LOG.warnf("Post with ID %s has no text content. Marking as commented to avoid re-processing.", postToComment.getMastodonId());
            postToComment.setVikiCommented(true);
            postToComment.persist();
            return;
        }

        // 3. Call our AI service to generate Viki's response.
        VikiResponse vikiResponse = vikiAiService.generatePostContent(topic.trim());

        if (vikiResponse == null) {
            LOG.error("Failed to generate content from VikiAiService. The original post will not be marked as commented.");
            // We don't mark the post as commented, so the bot can try again later.
            return;
        }

        // 4. For now, just log the generated content.
        // In the next step, we will actually post this to Mastodon.
        LOG.infof("🎉 Successfully generated a comment from Viki!");
        LOG.infof("   Content: %s", vikiResponse.content());
        LOG.infof("   Hashtags: %s", vikiResponse.hashTags());

        // 5. Mark the post as commented to prevent it from being processed again.
        postToComment.setVikiCommented(true);
        postToComment.persist();
        LOG.infof("Post with ID %s has been marked as 'commented'.", postToComment.getMastodonId());
    }
}
