package io.github.takgeun.shop.member.infra.mybatis;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mybatis")
@RequiredArgsConstructor
public class MyBatisMemberRepository implements MemberRepository {

    private final MemberMapper memberMapper;

    @Override
    public Member save(Member member) {
        int affectedRows;

        if(member.getId() == null) {
            affectedRows = memberMapper.insert(member);
            if(affectedRows != 1) {
                throw new IllegalStateException("회원 저장에 실패했습니다.");
            }
        } else {
            affectedRows = memberMapper.update(member);
            if(affectedRows != 1) {
                throw new IllegalStateException("회원 수정에 실패했습니다. id=" + member.getId());
            }
        }
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(memberMapper.findById(id));
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return Optional.ofNullable(memberMapper.findByEmail(email));
    }

    @Override
    public boolean existsByEmail(String email) {
        return memberMapper.existsByEmail(email);
    }

    @Override
    public List<Member> findAll() {
        return memberMapper.findAll();
    }
}
