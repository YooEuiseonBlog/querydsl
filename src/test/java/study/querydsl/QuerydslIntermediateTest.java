package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslIntermediateTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = Team.builder().name("teamA").build();
        Team teamB = Team.builder().name("teamB").build();

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = Member.builder()
                .username("member1")
                .age(10)
                .team(teamA)
                .build();

        Member member2 = Member.builder()
                .username("member2")
                .age(20)
                .team(teamA)
                .build();

        Member member3 = Member.builder()
                .username("member3")
                .age(30)
                .team(teamB)
                .build();

        Member member4 = Member.builder()
                .username("member4")
                .age(40)
                .team(teamB)
                .build();

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void simpleProjections() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

//        List<Member> resultMember = queryFactory
//                .select(member)
//                .from(member)
//                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        /**
         *  select
         *         new study.querydsl.dto.MemberDto(m.username,
         *         m.age)
         *     from
         *         Member m
         */

        /**
         *  select
         *             member0_.username as col_0_0_,
         *             member0_.age as col_1_0_
         *         from
         *             member member0_
         */

        /**
         * ?????? JPA?????? DTO??? ????????? ?????? new ???????????? ???????????????
         * DTO??? package????????? ??? ?????????????????? ????????????
         * ????????? ????????? ?????????
         */

        String qlString = "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m";
        List<MemberDto> result = em.createQuery(qlString, MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl ??? ??????(Bean population)
     *  ????????? DTO ????????? ??? ??????
     *
     *  ?????? 3?????? ?????? ??????
     *      ???   ???????????? ??????
     *      ???   ?????? ?????? ??????
     *      ???   ????????? ??????
     */

    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, //getter & setter ???????????? ???????????? ?????????????????? ?????? ??????(??????)
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, //????????? ??????
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByConstructor() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class, //????????? ??????
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * ???????????????, ?????? ?????? ?????? ???????????? ????????? ?????? ??? ?????? ??????
     * ExpressionUtils.as(source,alias) : ?????????, ?????? ????????? ?????? ??????
     * username.as("memberName") : ????????? ?????? ??????
     */

    @Test
    public void findUserDto() {
        /**
         * select
         *         member1.username as name,
         *         (select
         *             max(memberSub.age)
         *         from
         *             Member memberSub) as age
         *     from
         *         Member member1
         */

        /**
         * select
         *             member0_.username as col_0_0_,
         *             (select
         *                 max(member1_.age)
         *             from
         *                 member member1_) as col_1_0_
         *         from
         *             member member0_
         */

        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
//                        ExpressionUtils.as(member.username, "name"),
                        member.username.as("name"),
                        //??????????????? ExpressionUtils??? ????????? ??????????????????. (alias ??? ???)
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

//        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond)); //?????????, ????????? ??????
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * ?????? ?????? - Where ?????? ???????????? ??????
         * where ????????? null ?????? ????????????.
         * ???????????? ?????? ??????????????? ????????? ??? ??? ??????.
         * ?????? ????????? ???????????? ????????????.
     */
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
//                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
//        if (usernameCond == null) {
//            return null;
//        } else {
//            return member.username.eq(usernameCond);
//        }
//        return usernameCond == null ? null : member.username.eq(usernameCond);
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    //?????? ?????? isValid, ?????? IN: isServiceable

    /**
     * ?????? ??????
        * null ????????? ???????????? ???????????????
     */
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    public void bulkUpdate() {

        //member1 = 10 -> DB member1
        //member2 = 20 -> DB member2
        //member3 = 30 -> DB member3
        //member4 = 40 -> DB member4

        long count = queryFactory
                .update(member)
                .set(member.username, "?????????")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        // ?????? ????????? ????????? ??????????????? ???????????? ?????? db??? ????????? ?????????, ????????? ???????????? ????????? ????????????.
        // ????????? ???????????? > DB --> repeatable read
        //1 member1 = 10 -> 1 DB ?????????
        //2 member2 = 20 -> 2 DB ?????????
        //3 member3 = 30 -> 3 DB member3
        //4 member4 = 40 -> 4 DB member4

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    @Commit
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
//                .set(member.age, member.age.add(1))
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    @Commit
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    @Commit
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)
                        member.username.lower()
                ))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
