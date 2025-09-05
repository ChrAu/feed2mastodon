package com.hexix.ai.bot;

import com.hexix.ai.ExecutionJob;
import com.hexix.ai.ThemenEntity;
import com.hexix.ai.bot.telegram.TelegramNotificationService;
import com.hexix.mastodon.PublicMastodonPostEntity;
import com.hexix.mastodon.PublicMastodonPostRepository;
import io.quarkus.panache.common.Sort;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class BotScheduler {

    private static final Logger LOG = Logger.getLogger(BotScheduler.class);

    @Inject
    VikiAiService vikiAiService;

    @Inject
    TelegramNotificationService telegramNotificationService;

    @Inject
    PublicMastodonPostRepository publicMastodonPostRepository;

    @Scheduled(every = "2h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void scheduleWissenBot() {
        createExecutionJob("wissenBot");
        createExecutionJob("triggerBot");
    }


    @Transactional
    void createExecutionJob(String schedulerName) {
        // Check if an uncompleted job for this scheduler already exists.
        long existingJobs = ExecutionJob.count("schedulerName = ?1 and completed = false", schedulerName);
        if (existingJobs > 0) {
            LOG.infof("Job for scheduler '%s' already exists. Skipping creation.", schedulerName);
            return;
        }

        long randomDelayInSeconds = ThreadLocalRandom.current().nextLong(7200);
        LocalDateTime executionTime = LocalDateTime.now().plusSeconds(randomDelayInSeconds);

        ExecutionJob job = new ExecutionJob();
        job.schedulerName = schedulerName;
        job.executionTime = executionTime;
        job.completed = false;
        job.persist();

        LOG.infof("Scheduled new job '%s' to run at %s", schedulerName, executionTime);
    }

    @Scheduled(cron = "0/10 * 6-20 * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void processExecutionJobs() {
        LOG.debug("Checking for pending execution jobs...");
        List<ExecutionJob> pendingJobs = ExecutionJob.find(
                "completed = false AND executionTime <= ?1",
                LocalDateTime.now()
        ).list();

        if (pendingJobs.isEmpty()) {
            LOG.debug("No pending jobs found.");
            return;
        }

        LOG.infof("Found %d pending jobs to execute.", pendingJobs.size());

        for (ExecutionJob job : pendingJobs) {
            try {
                LOG.infof("Executing job: %s (ID: %d)", job.schedulerName, job.id);
                if ("wissenBot".equals(job.schedulerName)) {
                    executeWissenBotLogic();
                } else if ("triggerBot".equals(job.schedulerName)) {
                    executeTriggerBotLogic();
                }
                job.completed = true;
                job.persist();
                LOG.infof("Successfully executed and marked job as completed: %s (ID: %d)", job.schedulerName, job.id);
            } catch (Exception e) {
                LOG.errorf(e, "Error executing job: %s (ID: %d)", job.schedulerName, job.id);
                // Optionally, handle failed jobs (e.g., retry logic, marking as failed)
            }
        }
    }


    private void executeWissenBotLogic() {
        LOG.info("🤖 Executing wissenBot logic...");

        final List<ThemenEntity> list = ThemenEntity.findAll().<ThemenEntity>stream().filter(themenEntity -> themenEntity.getLastPost() == null || themenEntity.getLastPost().isBefore(LocalDate.now().minusDays(15))).toList();

        if(list.isEmpty()) {
            LOG.info("No new posts found for Viki to comment on. Will try again later.");
            return;
        }
        final int nextIndex = new Random().nextInt(list.size());
        final ThemenEntity themenEntity = list.get(nextIndex);

        LOG.infof("Found a post to comment on with ID: %s", themenEntity.getThema());

        themenEntity.setLastPost(LocalDate.now());

        VikiResponse vikiResponse = vikiAiService.generatePostContent(String.format("Fakten über %s", themenEntity.getThema().trim()));

        if (vikiResponse == null) {
            LOG.error("Failed to generate content from VikiAiService. The original post will not be marked as commented.");
            return;
        }

        LOG.infof("🎉 Successfully generated a comment from Viki!");
        LOG.infof("   Content: %s", vikiResponse.content());
        LOG.infof("   Hashtags: %s", vikiResponse.hashTags());

        String hashtags = String.join(" ", vikiResponse.hashTags());
        String broadcastMessage = String.format("%s\n\n%s", vikiResponse.content(), hashtags);

        LOG.infof("Ein neuer Beitrag wurde von Viki erstellt: %s", broadcastMessage);
        telegramNotificationService.broadcastMessage(broadcastMessage);
    }

    private void executeTriggerBotLogic() {
        LOG.info("🤖 Executing triggerBot logic...");

        final Optional<PublicMastodonPostEntity> postToCommentOption = publicMastodonPostRepository.findNextVikiComment();

        if (postToCommentOption.isEmpty()) {
            LOG.info("No new posts found for Viki to comment on. Will try again later.");
            return;
        }
        PublicMastodonPostEntity postToComment = postToCommentOption.get();

        LOG.infof("Found a post to comment on with ID: %s and cosDistance: %f",
                postToComment.getMastodonId(), postToComment.getCosDistance());

        String topic = (postToComment.getPostText() != null ? postToComment.getPostText() : "") +
                "\n" +
                (postToComment.getUrlText() != null ? postToComment.getUrlText() : "");

        if (topic.isBlank()) {
            LOG.warnf("Post with ID %s has no text content. Marking as commented to avoid re-processing.", postToComment.getMastodonId());
            postToComment.setVikiCommented(true);
            publicMastodonPostRepository.persist(postToComment);
            return;
        }

        VikiResponse vikiResponse = vikiAiService.generatePostContent(String.format("Gibt einen Kommentar zur '%s'", topic.trim()));

        if (vikiResponse == null) {
            LOG.error("Failed to generate content from VikiAiService. The original post will not be marked as commented.");
            return;
        }

        LOG.infof("🎉 Successfully generated a comment from Viki!");
        LOG.infof("   Content: %s", vikiResponse.content());
        LOG.infof("   Hashtags: %s", vikiResponse.hashTags());

        postToComment.setVikiCommented(true);
        publicMastodonPostRepository.persist(postToComment);

        LOG.infof("Post with ID %s has been marked as 'commented'.", postToComment.getMastodonId());

        String hashtags = String.join(" ", vikiResponse.hashTags());
        String broadcastMessage = String.format("%s\n\n%s", vikiResponse.content(), hashtags);

        LOG.infof("Ein neuer Beitrag wurde von Viki erstellt: %s", broadcastMessage);
        telegramNotificationService.broadcastMessage(broadcastMessage);
    }
}
