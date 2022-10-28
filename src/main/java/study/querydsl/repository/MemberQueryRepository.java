package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCond;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.function.Supplier;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberQueryRepository {
    private final JPAQueryFactory queryFactory;

    public MemberQueryRepository(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    public List<MemberTeamDto> search(MemberSearchCond condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age, team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
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
}
