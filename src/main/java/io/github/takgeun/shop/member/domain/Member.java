package io.github.takgeun.shop.member.domain;

import io.github.takgeun.shop.global.error.ConflictException;
import lombok.Getter;

@Getter
public class Member {
    private Long id;
    private String email;
    private String password;
    private String name;
    private String phone;
    private MemberRole role;
    private MemberStatus status;

    protected Member() {
    }

    public Member(String email, String password, String name, String phone) {
        // 생성자 생성 시점에서 검증 로직 넣기
        changeEmail(email);
        changePassword(password);
        changeName(name);
        changePhone(phone);
        this.role = MemberRole.USER;
        this.status = MemberStatus.ACTIVE;
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

    public static Member create(String email, String password, String name, String phone) {
        return new Member(email, password, name, phone);
    }

    public void changeEmail(String email) {
        // email 필수 검증 (null 체크 + trim() 기준 비어있는지 체크) --> IllegalArgumentException 400 Bad Request
        if(email == null) {
            throw new IllegalArgumentException("email은 필수입니다.");
        }
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
        if(password == null) {
            throw new IllegalArgumentException("password는 필수입니다.");
        }
        if(password.isEmpty()) {
            throw new IllegalArgumentException("password는 필수입니다.");
        }

        // password 길이 제한 --> IllegalArgumentException 400 Bad Request
        if(password.length() < 8) {
            throw new IllegalArgumentException("password 길이는 8자 이상이어야 합니다.");
        }
        if(password.length() > 20) {
            throw new IllegalArgumentException("password 길이는 20자 이하이어야 합니다.");
        }

        // 특수문자 및 대소문자 포함 등등 규칙은 나중에

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
            throw new IllegalArgumentException("phone은 필수입니다.");
        }
        String normalized = phone.trim();
        if(normalized.isEmpty()) {
            throw new IllegalArgumentException("phone은 필수입니다.");
        }

        // phone 길이 제한 --> IllegalArgumentException 400 Bad Request
        if(normalized.length() > 20) {
            throw new IllegalArgumentException("전화번호의 길이가 너무 깁니다.");
        }

        // 숫자, +, -, 공백만 허용 --> IllegalArgumentException 400 Bad Request
        if(!normalized.matches("[0-9+\\- ]+")) {
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

    public void deactivate() {
        if(this.status == MemberStatus.INACTIVE) {
            return;     // 멱등
        }
        this.status = MemberStatus.INACTIVE;
    }

    public void activate() {
        if(this.status == MemberStatus.ACTIVE) {
            return;     // 멱등
        }
        this.status = MemberStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return this.role == MemberRole.ADMIN;
    }
}
