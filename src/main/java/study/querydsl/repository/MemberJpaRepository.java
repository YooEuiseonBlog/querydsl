package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCond;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@Repository
//@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }


//    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
//
//        /*
//        bean 등록 후 사용 가능
//        @Bean
//        JPAQueryFactory jpaQueryFactory(EntityManager em) {
//            return new JPAQueryFactory(em);
//        }
//        */
//
//        this.em = em;
//        this.queryFactory = queryFactory;
//    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCond condition) {

        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }

        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCond condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    // 재사용 가능
    public List<Member> searchV2(MemberSearchCond condition) {
        return queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
                        ageBetween(condition.getAgeLoe(), condition.getAgeGoe())
                )
                .fetch();
    }

    private BooleanBuilder usernameEq(String username) {
        if (username == null) {
            return new BooleanBuilder();
        } else {
            return new BooleanBuilder(member.username.eq(username));
        }
    }

    private BooleanBuilder teamNameEq(String teamName) {
        return hasText(teamName) ? new BooleanBuilder(team.name.eq(teamName)) : new BooleanBuilder();
    }

    private BooleanBuilder ageGoe(Integer ageGoe) {
//        BooleanExpression booleanExpression = ageGoe != null ? member.age.goe(ageGoe) : null;
//        return new BooleanBuilder(booleanExpression);
        return nullSafeBuilder(() -> member.age.goe(ageGoe));
    }

    private BooleanBuilder ageLoe(Integer ageLoe) {
//        return new BooleanBuilder(ageLoe != null ? member.age.loe(ageLoe) : null);
        return nullSafeBuilder(() -> member.age.loe(ageLoe));
    }

    private BooleanBuilder ageBetween(Integer ageLoe, Integer ageGoe) {
        return ageLoe(ageLoe).and(ageGoe(ageGoe));
    }

    private static BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> f) {
        try {
            return new BooleanBuilder(f.get());
        } catch (Exception e) {
            return new BooleanBuilder();
        }
    }

//    private BooleanExpression ageBetween(int ageLoe, int ageGoe) {
//        return ageLoe(ageLoe).and(ageGoe(ageGoe));
//    }
//
//    private BooleanExpression usernameEq(String username) {
//        return hasText(username) ? member.username.eq(username) : null;
//    }
//
//    private BooleanExpression teamNameEq(String teamName) {
//        return hasText(teamName) ? team.name.eq(teamName) : null;
//    }
//
//    private BooleanExpression ageGoe(Integer ageGoe) {
//        return ageGoe != null ? member.age.goe(ageGoe) : null;
//    }
//
//    private BooleanExpression ageLoe(Integer ageLoe) {
//        return ageLoe != null ? member.age.loe(ageLoe) : null;
//    }

}
