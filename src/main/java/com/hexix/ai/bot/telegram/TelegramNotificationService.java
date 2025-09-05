package com.hexix.ai.bot.telegram;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TelegramNotificationService {

    private static final Logger LOG = Logger.getLogger(TelegramNotificationService.class);

    @Inject
    SubscriptionService subscriptionService;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    Instance<TelegramNotificationService> self;

    /**
     * Sends a message to all active subscribers and logs each message.
     * Includes a delay between messages to avoid hitting API rate limits.
     * @param message The message to send.
     */
    public void broadcastMessage(String message) {
        LOG.info("📢 Broadcasting message to all active subscribers...");
        List<Subscriber> activeSubscribers = subscriptionService.getActiveSubscribers();

        if (activeSubscribers.isEmpty()) {
            LOG.info("No active subscribers to send a message to.");
            return;
        }

        for (Subscriber subscriber : activeSubscribers) {
            // Step 1: Create and persist the log entry before sending.
            MessageLog logEntry = self.get().createLogEntry(subscriber, message);

            try {
                // Step 2: Try to send the message.
                producerTemplate.sendBodyAndHeader("direct:sendDirectMessage", message, "chatId", subscriber.getChatId());
                LOG.infof("Message sent to subscriber with ID: %d (ChatID: %s)", subscriber.getId(), subscriber.getChatId());

                // Step 3: If successful, update the log entry.
                self.get().markAsSent(logEntry.getId());

                // Add a delay to avoid hitting API rate limits
                Thread.sleep(100); // 100ms delay

            } catch (InterruptedException e) {
                LOG.warn("Message sending thread was interrupted.");
                Thread.currentThread().interrupt();
                break; // Exit the loop if the thread is interrupted
            } catch (Exception e) {
                LOG.errorf(e, "Failed to send message to subscriber with ID: %d (ChatID: %s). The log entry ID is %d.",
                        subscriber.getId(), subscriber.getChatId(), logEntry.getId());
                // The log entry remains marked as not successfully sent.
            }
        }
        LOG.info("✅ Finished broadcasting message.");
    }

    /**
     * Creates a new MessageLog entry in a new transaction.
     * @param subscriber The recipient subscriber.
     * @param message The message content.
     * @return The persisted MessageLog entity.
     */
    @Transactional
    public MessageLog createLogEntry(Subscriber subscriber, String message) {
        MessageLog log = new MessageLog(subscriber, message);
        log.persist();
        return log;
    }

    /**
     * Marks a message log as successfully sent in a new transaction.
     * @param logId The ID of the MessageLog to update.
     */
    @Transactional
    public void markAsSent(Long logId) {
        MessageLog.findByIdOptional(logId).ifPresent(log -> {
            MessageLog messageLog = (MessageLog) log;
            messageLog.setSuccessfullySent(true);
            messageLog.setDeliveryTimestamp(LocalDateTime.now());
        });
    }
}
