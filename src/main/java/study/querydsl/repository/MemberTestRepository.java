package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCond;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import java.util.List;
import java.util.function.Supplier;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCond condition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                        allSearchCondition(condition)
                );

        List<Member> content = getQuerydsl().applyPagination(pageable, query)
                .fetch();
        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    public Page<Member> applyPagination(MemberSearchCond condition, Pageable pageable) {
        return applyPagination(pageable, query -> query
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(allSearchCondition(condition))
        );
    }

    public Page<Member> applyPagination2(MemberSearchCond condition, Pageable pageable) {
        return applyPagination(pageable, contentQuery -> contentQuery
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(allSearchCondition(condition)
                ), countQuery -> countQuery
                .select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(allSearchCondition(condition))
        );
    }


    private BooleanBuilder allSearchCondition(MemberSearchCond condition) {
        return usernameEq(condition.getUsername())
                .and(teamNameEq(condition.getTeamName()))
                .and(ageGoe(condition.getAgeGoe()))
                .and(ageLoe(condition.getAgeLoe()));
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
