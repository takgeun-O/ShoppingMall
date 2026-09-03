package io.github.takgeun.shop.member.domain;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Member {
    private Long id;
    private String email;
    private String password;    // 저장용으로 인코딩된 비밀번호
    private String name;
    private String phone;
    private MemberRole role;
    private MemberStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    protected Member() {
    }

    private Member(String email, String password, String name, String phone) {
        // 생성자 생성 시점에서 검증 로직 넣기
        changeEmail(email);
        changePassword(password);
        changeName(name);
        changePhone(phone);
        this.role = MemberRole.USER;
        this.status = MemberStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = null;    // 생성 시점엔 null
    }

    public static Member create(String email, String password, String name, String phone) {
        return new Member(email, password, name, phone);
    }

    public void assignId(Long id) {
        if(id == null || id <= 0) {
            throw new IllegalArgumentException("id는 양수여야 합니다.");
        }
        if(this.id != null) {
            throw new ConflictException("id는 이미 할당되었습니다.");
        }
        this.id = id;
    }

    public void changeEmail(String email) {
        // email 필수 검증 (null 체크 + trim() 기준 비어있는지 체크) --> IllegalArgumentException 400 Bad Request
        if(email == null) throw new IllegalArgumentException("email은 필수입니다.");

        String normalized = email.trim().toLowerCase();
        if(normalized.isEmpty()) {
            throw new IllegalArgumentException("email은 필수입니다.");
        }

        // email 길이 제한 --> IllegalArgumentException 400 Bad Request
        if(normalized.length() > 320) {
            throw new IllegalArgumentException("email 길이는 320자 이하여야 합니다.");
        }

        // @가 정확히 1개인지, 로컬/도메인 파트가 비어있지 않은지 --> IllegalArgumentException 400 Bad Request
        int at = normalized.indexOf('@');
        // @가 없거나 @가 문자열 맨 앞에 있는 경우 || 처음 등장하는 @의 위치와 마지막에 등장하는 @의 위치가 다를 경우(즉 @ 개수가 2개 이상일 때 || @의 위치가 마지막일 때
        if(at <= 0 || at != normalized.lastIndexOf('@') || at == normalized.length() - 1) {
            throw new IllegalArgumentException("email 형식이 올바르지 않습니다.");
        }

        this.email = normalized;
    }

    public void changePassword(String password) {
        // password 필수 검증 (null 체크 + trim() 기준 비어있는지 체크) --> IllegalArgumentException 400 Bad Request
        if(password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("password는 필수입니다.");
        }

        // 인코딩 방식과 무관하게 DB 컬럼 크기 이내의 저장 비밀번호만 허용한다.
        if(password.length() > 255) {
            throw new IllegalArgumentException("password 길이는 255자 이하이어야 합니다.");
        }

        this.password = password;
    }

    public void changeName(String name) {
        // name 필수 검증 (null 체크 + trim() 기준 비어있는지 체크) --> IllegalArgumentException 400 Bad Request
        if(name == null) {
            throw new IllegalArgumentException("name은 필수입니다.");
        }
        String normalized = name.trim();
        if(normalized.isEmpty()) {
            throw new IllegalArgumentException("name은 필수입니다.");
        }

        // name 길이 제한 --> IllegalArgumentException 400 Bad Request
        if(normalized.length() > 50) {
            throw new IllegalArgumentException("name 길이는 50자 이하여야 합니다.");
        }

        this.name = normalized;
    }

    public void changePhone(String phone) {
        // phone 필수 검증 (null 체크 + trim() 기준 비어있는지 체크) --> IllegalArgumentException 400 Bad Request
        if(phone == null) {
            throw new IllegalArgumentException("전화번호는 필수입니다.");
        }
        String normalized = phone.trim();
        if(normalized.isEmpty()) {
            throw new IllegalArgumentException("전화번호는 필수입니다.");
        }

        // phone 길이 제한 --> IllegalArgumentException 400 Bad Request
        if(normalized.length() > 20) {
            throw new IllegalArgumentException("전화번호의 길이가 너무 깁니다.");
        }

        // 한국 휴대폰 형식 --> IllegalArgumentException 400 Bad Request
        if(!normalized.matches("^010-\\d{4}-\\d{4}$")) {
            throw  new IllegalArgumentException("전화번호 형식이 올바르지 않습니다.");
        }

        this.phone = normalized;
    }

    public void changeRole(MemberRole role) {
        if(role == null) {
            throw new IllegalArgumentException("role은 필수입니다.");
        }
        this.role = role;
    }

    // 상태 변경 (멱등)
    public void changeStatus(MemberStatus status) {
        if(status == null) throw new IllegalArgumentException("status는 필수입니다.");
        if(this.status == status) return;   // 멱등
        this.status = status;
    }

    public void activate() {
        changeStatus(MemberStatus.ACTIVE);
    }

    public void deactivate() {
        changeStatus(MemberStatus.INACTIVE);
    }

    public void withdraw() { changeStatus(MemberStatus.WITHDRAWN); }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        if(lastLoginAt == null) {
            throw new IllegalArgumentException("lastLoginAt은 필수입니다.");
        }
        this.lastLoginAt = lastLoginAt;
    }
}
