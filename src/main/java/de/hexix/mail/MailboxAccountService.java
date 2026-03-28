package de.hexix.mail;

import de.hexix.mail.model.MailboxAccount;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class MailboxAccountService {

    private static final Logger LOG = Logger.getLogger(MailboxAccountService.class.getName());

    @Inject
    EntityManager entityManager;

    @Transactional
    public List<MailboxAccount> getAllMailboxAccounts() {
        // Verwendet die Named Query "MailboxAccount.findAll"
        return entityManager.createNamedQuery(MailboxAccount.QUERY_FIND_ALL, MailboxAccount.class).getResultList();
    }

    // Neue Methode: Gibt ein MailboxAccount anhand der E-Mail-Adresse zurück
    @Transactional
    public MailboxAccount getMailboxAccountByEmail(String email) {
        try {
            TypedQuery<MailboxAccount> query = entityManager.createNamedQuery(MailboxAccount.QUERY_FIND_BY_EMAIL, MailboxAccount.class);
            query.setParameter("email", email);
            return query.getSingleResult();
        } catch (NoResultException e) {
            LOG.warning("No MailboxAccount found for email: " + email);
            return null;
        } catch (Exception e) {
            LOG.severe("Error retrieving MailboxAccount for email " + email + ": " + e.getMessage());
            throw new RuntimeException("Error retrieving MailboxAccount", e);
        }
    }

    @Transactional
    public void addMailboxAccount(MailboxAccount account) {
        entityManager.persist(account);
        LOG.info("Mailbox account added for: " + account.getEmail());
    }

    @Transactional
    public MailboxAccount updateMailboxAccount(MailboxAccount account) {
        return entityManager.merge(account);
    }

    @Transactional
    public void deleteMailboxAccount(Long id) {
        MailboxAccount account = entityManager.find(MailboxAccount.class, id);
        if (account != null) {
            entityManager.remove(account);
            LOG.info("Mailbox account deleted for ID: " + id);
        }
    }
}
