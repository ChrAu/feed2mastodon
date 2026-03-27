package com.hexix.mail;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.logging.Logger;

@ApplicationScoped
public class MailReceiverScheduler {

    private static final Logger LOG = Logger.getLogger(MailReceiverScheduler.class.getName());

    @Inject
    MailReceiverService mailReceiverService;

    /**
     * Überprüft alle konfigurierten Mailboxen auf empfangene E-Mails.
     * Der Cron-Ausdruck "0 5 * * * ?" bedeutet:
     * Jede Stunde zur Minute 5.
     */
    @Scheduled(cron = "0 5 * * * ?")
    void checkReceivedEmails() {
        LOG.info("Starting scheduled fallback check for received emails...");
        mailReceiverService.checkAllMailboxesForReceivedEmails();
        LOG.info("Finished scheduled fallback check for received emails.");
    }
}
