package com.hexix.ai.bot.telegram;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class SubscriptionService {

    private static final Logger LOG = Logger.getLogger(SubscriptionService.class);

    /**
     * Adds a subscriber or reactivates an existing inactive one.
     * @param chatId The chat ID of the Telegram chat.
     */
    @Transactional
    public void addSubscriber(String chatId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            LOG.warn("Chat ID is null or empty");
            return;
        }

        Subscriber subscriber = Subscriber.findByChatId(chatId);
        if (subscriber == null) {
            LOG.infof("Adding new subscriber with ChatID %s", chatId);
            Subscriber newSubscriber = new Subscriber(chatId);
            newSubscriber.persist();
        } else if (!subscriber.isActive()) {
            LOG.infof("Reactivating subscriber with ChatID %s", chatId);
            subscriber.setActive(true);
        } else {
            LOG.debugf("Chat ID %s is already active", chatId);
        }
    }

    /**
     * Deactivates a subscriber (soft delete).
     * @param chatId The chat ID to deactivate.
     * @return true if the subscriber was deactivated, false otherwise.
     */
    @Transactional
    public boolean removeSubscriber(String chatId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            LOG.warn("Chat ID is null or empty");
            return false;
        }

        Subscriber subscriber = Subscriber.findByChatId(chatId);
        if (subscriber != null && subscriber.isActive()) {
            subscriber.setActive(false);
            LOG.infof("Deactivated subscriber with ChatID: %s", chatId);
            return true;
        } else {
            LOG.warnf("Subscriber to deactivate not found or already inactive: %s", chatId);
            return false;
        }
    }

    /**
     * Returns all active subscribers.
     * @return List of all active subscribers.
     */
    public List<Subscriber> getActiveSubscribers() {
        List<Subscriber> activeSubscribers = Subscriber.findActive();
        LOG.debugf("Found %d active subscribers", activeSubscribers.size());
        return activeSubscribers;
    }

    /**
     * Checks if a chat ID is actively subscribed.
     * @param chatId The chat ID to check.
     * @return true if subscribed and active, false otherwise.
     */
    public boolean isSubscribed(String chatId) {
        Subscriber subscriber = Subscriber.findByChatId(chatId);
        return subscriber != null && subscriber.isActive();
    }

    /**
     * Returns the count of active subscribers.
     * @return The number of active subscribers.
     */
    public long getSubscriberCount() {
        return Subscriber.count("active", true);
    }

    /**
     * Deactivates all subscribers (for administrative purposes).
     */
    @Transactional
    public void clearAllSubscribers() {
        long count = Subscriber.update("active = false");
        LOG.infof("Deactivated all %d subscribers", count);
    }
}
