package com.hexix.mail.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mailbox_account")
@NamedQueries({
    @NamedQuery(name = MailboxAccount.QUERY_FIND_ALL, query = "SELECT ma FROM MailboxAccount ma"),
    @NamedQuery(name = MailboxAccount.QUERY_FIND_BY_EMAIL, query = "SELECT ma FROM MailboxAccount ma WHERE ma.email = :email") // Neue Named Query
})
public class MailboxAccount {

    public static final String QUERY_FIND_ALL = "MailboxAccount.findAll";
    public static final String QUERY_FIND_BY_EMAIL = "MailboxAccount.findByEmail";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; // Die E-Mail-Adresse des Postfachs

    @Column(nullable = false)
    private String host; // IMAP/POP3 Host

    @Column(nullable = false)
    private int port; // IMAP/POP3 Port

    @Column(nullable = false)
    private String username; // Benutzername für das Postfach

    @Column(nullable = false)
    private String password; // Passwort für das Postfach

    @Column(nullable = false)
    private String protocol; // z.B. "imaps", "pop3s"

    // Default constructor for JPA
    public MailboxAccount() {
    }

    public MailboxAccount(String email, String host, int port, String username, String password, String protocol) {
        this.email = email;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.protocol = protocol;
    }

    // --- Getter und Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
