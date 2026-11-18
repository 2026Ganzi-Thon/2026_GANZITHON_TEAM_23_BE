package com.ganzithon.pulse.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(nullable = false)
    private String passwordHash;

    /** 심박 급상승 알림 기준 (평소 대비 bpm 차이). "괜찮아요"를 누를수록 올라감 */
    @Column(nullable = false)
    private int alertThreshold;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {}

    public User(String name, String nickname, String email, String loginId,
                String passwordHash, int alertThreshold) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.alertThreshold = alertThreshold;
    }

    public void raiseThreshold(int step, int max) {
        this.alertThreshold = Math.min(this.alertThreshold + step, max);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getLoginId() { return loginId; }
    public String getPasswordHash() { return passwordHash; }
    public int getAlertThreshold() { return alertThreshold; }
}
