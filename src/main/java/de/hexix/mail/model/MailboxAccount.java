package de.hexix.mail.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mailbox_account")
@NamedQueries({
    @NamedQuery(name = MailboxAccount.QUERY_FIND_ALL, query = "SELECT ma FROM MailboxAccount ma"),
    @NamedQuery(name = MailboxAccount.QUERY_FIND_BY_EMAIL, query = "SELECT ma FROM MailboxAccount ma WHERE ma.email = :email")
})
public class MailboxAccount {

    public static final String QUERY_FIND_ALL = "MailboxAccount.findAll";
    public static final String QUERY_FIND_BY_EMAIL = "MailboxAccount.findByEmail";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private String username;

    @Column(nullable = true) // Can be null for OAuth
    private String password;

    @Column(nullable = false)
    private String protocol;

    @Column(nullable = false)
    private String authenticationType = "PASSWORD"; // Default to PASSWORD

    @Lob
    @Column(length = 1024)
    private String accessToken;

    @Lob
    @Column(length = 1024)
    private String refreshToken;

    private LocalDateTime accessTokenExpiry;

    @Column(name = "last_successful_login")
    private LocalDateTime lastSuccessfulLogin;

    // Default constructor for JPA
    public MailboxAccount() {
    }

    // Constructor for password-based authentication
    public MailboxAccount(String email, String host, int port, String username, String password, String protocol) {
        this.email = email;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.protocol = protocol;
        this.authenticationType = "PASSWORD";
    }

    // Constructor for OAuth-based authentication
    public MailboxAccount(String email, String host, int port, String username, String protocol) {
        this.email = email;
        this.host = host;
        this.port = port;
        this.username = username;
        this.protocol = protocol;
        this.authenticationType = "OAUTH";
    }

    // --- Getters and Setters ---
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

    public String getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    public void setAccessTokenExpiry(LocalDateTime accessTokenExpiry) {
        this.accessTokenExpiry = accessTokenExpiry;
    }

    public LocalDateTime getLastSuccessfulLogin() {
        return lastSuccessfulLogin;
    }

    public void setLastSuccessfulLogin(LocalDateTime lastSuccessfulLogin) {
        this.lastSuccessfulLogin = lastSuccessfulLogin;
    }
}