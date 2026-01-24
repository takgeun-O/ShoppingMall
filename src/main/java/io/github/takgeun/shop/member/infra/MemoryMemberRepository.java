package io.github.takgeun.shop.member.infra;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MemoryMemberRepository implements MemberRepository {

    // 저장 순서는 필요하지 않을 것 같음. HashMap<> 사용
    private final Map<Long, Member> store = new HashMap<>();
    private long sequence = 0L;

    @Override
    public Member save(Member member) {
        if(member.getId() == null) {
            long id = ++sequence;
            member.assignId(id);
            store.put(id, member);
            return member;
        }

        // 수정할 때는 기존 id로 덮어쓰기
        // 우선 메모리 저장이니까 Map에 덮어쓰자.
        store.put(member.getId(), member);
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(store.get(id));  // 값이 있으면 Optional<Category> 없으면 Optional.empty()
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        if(email == null) {
            return Optional.empty();
        }
        String key = email.trim().toLowerCase();
        return store.values().stream()
                .filter(m -> key.equals(m.getEmail()))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public List<Member> findAll() {
        return new ArrayList<>(store.values());
    }
}
