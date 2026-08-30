package io.github.takgeun.shop.member.infra.mybatis;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static io.github.takgeun.shop.TestPasswordFixtures.BCRYPT_PASSWORD;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@MybatisTest
@Import(MyBatisMemberRepository.class)      // @MybatisTest는 @Mapper만 스캔함. @Repository는 스캔 안하니까 직접 넣음
@ActiveProfiles({"test", "mybatis"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MyBatisMemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void save() {

        // given
        Member member = Member.create(
                "test@test.com",
                BCRYPT_PASSWORD,
                "테스트유저",
                "010-1234-5555"
        );

        // when
        Member savedMember = memberRepository.save(member);

        // then
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void findById() {

        // given
        Member member = Member.create(
                "test@test.com",
                BCRYPT_PASSWORD,
                "테스트유저",
                "010-1234-5555"
        );
        Member savedMember = memberRepository.save(member);

        // when
        Optional<Member> result = memberRepository.findById(savedMember.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedMember.getId());
    }

    @Test
    void findByEmail() {

        // given
        Member member = Member.create(
                "test@test.com",
                BCRYPT_PASSWORD,
                "테스트유저",
                "010-1234-5555"
        );
        Member savedMember = memberRepository.save(member);

        // when
        Optional<Member> result = memberRepository.findByEmail(savedMember.getEmail());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedMember.getId());
    }

    @Test
    void existsByEmail() {

        // given
        Member member = Member.create(
                "test@test.com",
                BCRYPT_PASSWORD,
                "테스트유저",
                "010-1234-5555"
        );
        Member savedMember = memberRepository.save(member);

        // when
        boolean exists = memberRepository.existsByEmail("test@test.com");
        boolean notExists = memberRepository.existsByEmail("ddddd@test.com");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void findAll() {

        // given
        Member member1 = Member.create(
                "test1@test.com",
                BCRYPT_PASSWORD,
                "테스트유저1",
                "010-1234-2222"
        );
        Member member2 = Member.create(
                "test2@test.com",
                BCRYPT_PASSWORD,
                "테스트유저2",
                "010-1234-3333"
        );
        Member savedMember1 = memberRepository.save(member1);
        Member savedMember2 = memberRepository.save(member2);

        // when
        List<Member> result = memberRepository.findAll();

        // then
        assertThat(result).hasSize(2);
    }
}
