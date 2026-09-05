package io.github.takgeun.shop.member.domain;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 20;

    /**
     * PasswordPolicy는 상태를 가진 객체가 아니라 비밀번호 정책 상수만 제공하는 유틸리티 클래스임
     *
     * 다른 데서 인스턴스를 새롭게 생성할 이유가 없으므로
     * private 생성자를 명시해 외부 생성을 차단한다.
     */
    private PasswordPolicy() {

    }
}
