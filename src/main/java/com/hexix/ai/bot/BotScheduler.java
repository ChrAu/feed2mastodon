package com.hexix.ai.bot;

import com.hexix.ai.ThemenEntity;
import com.hexix.ai.bot.telegram.TelegramNotificationService;
import com.hexix.mastodon.PublicMastodonPostEntity;
import io.quarkus.panache.common.Sort;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class BotScheduler {

    private static final Logger LOG = Logger.getLogger(BotScheduler.class);

    @Inject
    VikiAiService vikiAiService;

    @Inject
    TelegramNotificationService telegramNotificationService;


    @Scheduled(cron = "0 0 6-20/2 * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void wissenBot() {

        try {
            // Berechne eine zufällige Verzögerung in Sekunden (0 bis 7199 Sekunden).
            // 2 Stunden = 120 Minuten = 7200 Sekunden.
            long randomDelayInSeconds = ThreadLocalRandom.current().nextLong(7200);

            System.out.println(
                    "Scheduler-Prozess gestartet um " + LocalTime.now() +
                            ". Warte für " + randomDelayInSeconds + " Sekunden."
            );

            // Lege den Thread für die zufällige Zeit schlafen.
            TimeUnit.SECONDS.sleep(randomDelayInSeconds);

            // Nach der Wartezeit wird die eigentliche Logik ausgeführt.
            System.out.println("Führe die Bot-Logik aus um " + LocalTime.now());



            LOG.info("🤖 Bot Scheduler triggered. Looking for a new post to comment on...");

            final List<ThemenEntity> list = ThemenEntity.findAll().<ThemenEntity>stream().filter(themenEntity -> themenEntity.getLastPost() == null || themenEntity.getLastPost().isBefore(LocalDate.now().minusDays(15))).toList();

            if(list.isEmpty()) {
                LOG.info("No new posts found for Viki to comment on. Will try again later.");
                return;
            }
            final int nextIndex = new Random().nextInt(list.size());
            final ThemenEntity themenEntity = list.get(nextIndex);



            LOG.infof("Found a post to comment on with ID: %s", themenEntity.getThema());

            themenEntity.setLastPost(LocalDate.now());


            // 3. Call our AI service to generate Viki's response.
            VikiResponse vikiResponse = vikiAiService.generatePostContent(themenEntity.getThema().trim());

            if (vikiResponse == null) {
                LOG.error("Failed to generate content from VikiAiService. The original post will not be marked as commented.");
                return;
            }

            // 4. Log the generated content.
            LOG.infof("🎉 Successfully generated a comment from Viki!");
            LOG.infof("   Content: %s", vikiResponse.content());
            LOG.infof("   Hashtags: %s", vikiResponse.hashTags());


            // 6. Broadcast the new post to all subscribers using the notification service.
            String hashtags = String.join(" ", vikiResponse.hashTags()); // Format hashtags for user-facing message
            String broadcastMessage = String.format("%s\n\n%s", vikiResponse.content(), hashtags);

            LOG.infof("Ein neuer Beitrag wurde von Viki erstellt: %s", broadcastMessage);
            telegramNotificationService.broadcastMessage(broadcastMessage);
        } catch (InterruptedException e) {
            // Gute Praxis: Den Interrupt-Status wiederherstellen.
            Thread.currentThread().interrupt();
            LOG.error("Der Scheduler-Thread wurde unterbrochen.", e);
        }
    }


    /**
     * This method is executed automatically by the scheduler.
     * It finds the most relevant, uncommented post and generates a response from Viki.
     * The cron expression "0 0/15 * * * ?" means "every 15 minutes".
     */
    @Scheduled(cron = "0 0 7-19/2 * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void triggerBot() {
        try {
            // Berechne eine zufällige Verzögerung in Sekunden (0 bis 7199 Sekunden).
            // 2 Stunden = 120 Minuten = 7200 Sekunden.
            long randomDelayInSeconds = ThreadLocalRandom.current().nextLong(7200);

            System.out.println(
                    "Scheduler-Prozess gestartet um " + LocalTime.now() +
                            ". Warte für " + randomDelayInSeconds + " Sekunden."
            );

            // Lege den Thread für die zufällige Zeit schlafen.
            TimeUnit.SECONDS.sleep(randomDelayInSeconds);

            // Nach der Wartezeit wird die eigentliche Logik ausgeführt.
            System.out.println("Führe die Bot-Logik aus um " + LocalTime.now());



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
                return;
            }

            // 4. Log the generated content.
            LOG.infof("🎉 Successfully generated a comment from Viki!");
            LOG.infof("   Content: %s", vikiResponse.content());
            LOG.infof("   Hashtags: %s", vikiResponse.hashTags());

            // 5. Mark the post as commented to prevent it from being processed again.
            postToComment.setVikiCommented(true);
            postToComment.persist();
            LOG.infof("Post with ID %s has been marked as 'commented'.", postToComment.getMastodonId());

            // 6. Broadcast the new post to all subscribers using the notification service.
            String hashtags = String.join(" ", vikiResponse.hashTags()); // Format hashtags for user-facing message
            String broadcastMessage = String.format("%s\n\n%s", vikiResponse.content(), hashtags);

            LOG.infof("Ein neuer Beitrag wurde von Viki erstellt: %s", broadcastMessage);
            telegramNotificationService.broadcastMessage(broadcastMessage);
        } catch (InterruptedException e) {
            // Gute Praxis: Den Interrupt-Status wiederherstellen.
            Thread.currentThread().interrupt();
           LOG.error("Der Scheduler-Thread wurde unterbrochen.", e);
        }
    }
}
