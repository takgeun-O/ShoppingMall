package io.github.takgeun.shop.member.infra.mybatis;

import io.github.takgeun.shop.member.domain.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MemberMapper {

    int insert(Member member);

    int update(Member member);

    Member findById(@Param("id") Long id);

    Member findByEmail(@Param("email") String email);

    boolean existsByEmail(@Param("email") String email);

    List<Member> findAll();

    int deleteAll();    // 테스트용
}
