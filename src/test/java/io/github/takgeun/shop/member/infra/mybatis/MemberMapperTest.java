package io.github.takgeun.shop.member.infra.mybatis;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static io.github.takgeun.shop.TestPasswordFixtures.BCRYPT_PASSWORD;
import static io.github.takgeun.shop.TestPasswordFixtures.matchesTestPassword;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@MybatisTest
@ActiveProfiles({"test", "mybatis"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberMapperTest {

    @Autowired
    private MemberMapper memberMapper;

    @BeforeEach
    void clear() {
        memberMapper.deleteAll();
    }

    @Test
    void insert() {

        // given
        Member member = Member.create(
                "test1@test.com",
                BCRYPT_PASSWORD,
                "테스트회원",
                "010-1234-5555"
        );

        // when
        int affectedRows = memberMapper.insert(member);

        // then
        assertThat(affectedRows).isEqualTo(1);
        assertThat(member.getId()).isNotNull();

        Member found = memberMapper.findById(member.getId());
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("test1@test.com");
        assertThat(found.getPassword()).isEqualTo(BCRYPT_PASSWORD);
        assertThat(matchesTestPassword(found.getPassword())).isTrue();
        assertThat(found.getName()).isEqualTo("테스트회원");
        assertThat(found.getPhone()).isEqualTo("010-1234-5555");
        assertThat(found.getRole()).isEqualTo(MemberRole.USER);
        assertThat(found.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getLastLoginAt()).isNull();
    }

    @Test
    void findById() {

        // given
        Member member = Member.create(
                "test1@test.com",
                BCRYPT_PASSWORD,
                "테스트회원",
                "010-1234-5555"
        );

        memberMapper.insert(member);

        // when
        Member found = memberMapper.findById(member.getId());

        // then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(member.getId());
        assertThat(found.getEmail()).isEqualTo("test1@test.com");
    }

    @Test
    void findByEmail() {

        // given
        Member member = Member.create(
                "test1@test.com",
                BCRYPT_PASSWORD,
                "테스트회원",
                "010-1234-5555"
        );

        memberMapper.insert(member);

        // when
        Member found = memberMapper.findByEmail("test1@test.com");

        // then
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("test1@test.com");
        assertThat(found.getName()).isEqualTo("테스트회원");
    }

    @Test
    void existsByEmail_true() {

        // given
        Member member = Member.create(
                "test1@test.com",
                BCRYPT_PASSWORD,
                "테스트회원",
                "010-1234-5555"
        );

        memberMapper.insert(member);

        // when
        boolean result = memberMapper.existsByEmail("test1@test.com");

        // then
        assertThat(result).isTrue();
    }

    @Test
    void existsByEmail_false() {

        // when
        boolean result = memberMapper.existsByEmail("test1@test.com");

        // then
        assertThat(result).isFalse();
    }

    @Test
    void findAll() {
        // given
        Member member1 = Member.create(
                "test1@test.com",
                BCRYPT_PASSWORD,
                "테스트회원1",
                "010-1234-5555"
        );
        Member member2 = Member.create(
                "test2@test.com",
                BCRYPT_PASSWORD,
                "테스트회원2",
                "010-1234-2225"
        );

        memberMapper.insert(member1);
        memberMapper.insert(member2);

        // when
        List<Member> result = memberMapper.findAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Member::getEmail)
                .contains("test1@test.com", "test2@test.com");
    }

    @Test
    void update() {

        // given
        Member member = Member.create(
                "test1@test.com",
                BCRYPT_PASSWORD,
                "테스트회원",
                "010-1234-5555"
        );
        memberMapper.insert(member);

        member.changeName("수정테스트");
        member.changePhone("010-1111-2222");
        member.changeRole(MemberRole.ADMIN);
        member.changeStatus(MemberStatus.INACTIVE);
        member.updateLastLoginAt(LocalDateTime.of(2026, 3, 28, 12, 30));

        // when
        int affectedRows = memberMapper.update(member);

        // then
        Member found = memberMapper.findById(member.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("수정테스트");
        assertThat(found.getPhone()).isEqualTo("010-1111-2222");
        assertThat(found.getRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(found.getStatus()).isEqualTo(MemberStatus.INACTIVE);
        assertThat(found.getLastLoginAt()).isEqualTo(LocalDateTime.of(2026, 3, 28, 12, 30));

        // password는 추후 변경 페이지 만들 예정 (지금은 변경 X)
        assertThat(found.getPassword()).isEqualTo(BCRYPT_PASSWORD);

    }
}
