package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

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
    }//before()

    @Test
    public void startJPQL() {
        //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl() {
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
//        QMember m = new QMember("m");
//        QMember m = QMember.member;

//        Member findMember = queryFactory
//                .select(m)
//                .from(m)
//                .where(m.username.eq("member1")) //파라미터 바인딩 처리
//                .fetchOne();

        //권장!
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
//                        .and(member.age.eq(10)))
                        .and(member.age.between(10, 30)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10) //','를 써도 and연산자 같은 효과를 발휘한다.
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetchTest() {

        /**
         * fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
         * fetchOne() : 단 건 조회
         * 결과가 없으면 : null
         * 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
         * fetchFirst() : limit(1).fetchOne()
         * fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
         * fetchCount() : count 쿼리로 변경해서 count 수 조회
         */

        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //단 건
        Member fetchOne = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //처음 한 건 조회
        Member fetchFirst = queryFactory
                .selectFrom(member)
//                .limit(1).fetchOne();
                .fetchFirst();

        //페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();

        long offset = results.getOffset();
        long limit = results.getLimit();
        System.out.println("limit = " + limit);
        System.out.println("offset = " + offset);

        //count 쿼리로 변경
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();


    }

    /**
     * Querydsl fetchResults() , fetchCount() Deprecated(향후 미지원)
     * Querydsl의 fetchCount() , fetchResult() 는 개발자가 작성한 select 쿼리를 기반으로 count용
     * 쿼리를 내부에서 만들어서 실행합니다.
     * 그런데 이 기능은 강의에서 설명드린 것 처럼 select 구문을 단순히 count 처리하는 용도로 바꾸는
     * 정도입니다. 따라서 단순한 쿼리에서는 잘 동작하지만, 복잡한 쿼리에서는 제대로 동작하지 않습니다.
     * Querydsl은 향후 fetchCount() , fetchResult() 를 지원하지 않기로 결정했습니다.
     * 참고로 Querydsl의 변화가 빠르지는 않기 때문에 당장 해당 기능을 제거하지는 않을 것입니다.
     * 따라서 count 쿼리가 필요하면 다음과 같이 별도로 작성해야 합니다.
     */

    @Test
    public void count() {
        Long totalCount = queryFactory
//                .select(Wildcard.count) //select count(*)
                .select(member.count()) //select count(member.id)
                .from(member)
                .fetchOne();

        System.out.println("totalCount = " + totalCount);

        /**
         * count(*) 을 사용하고 싶으면 예제의 주석처럼 Wildcard.count 를 사용하시면 됩니다.
         * member.count() 를 사용하면 count(member.id) 로 처리됩니다.
         * 응답 결과는 숫자 하나이므로 fetchOne() 을 사용합니다.
         */
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름을 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {

        Member memberNull = Member.builder()
                .age(100)
                .build();

        Member member5 = Member.builder()
                .username("member5")
                .age(100)
                .build();

        Member member6 = Member.builder()
                .username("member6")
                .age(100)
                .build();

        em.persist(memberNull);
        em.persist(member5);
        em.persist(member6);

        /**
         *  select
         *         member1
         *     from
         *         Member member1
         *     where
         *         member1.age = ?1
         *     order by
         *         member1.age desc,
         *         member1.username asc nulls last
         */
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member findMember5 = result.get(0);
        Member findMember6 = result.get(1);
        Member findMemberNull = result.get(2);

        assertThat(findMember5.getUsername()).isEqualTo("member5");
        assertThat(findMember6.getUsername()).isEqualTo("member6");
        assertThat(findMemberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {

        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2 == 15

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2 == 35
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
//                .leftJoin(member.team, team)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인 : 외부 조인 불가능
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */

    @Test
    public void theta_join() {
        em.persist(Member.builder().username("teamA").age(10).build());
        em.persist(Member.builder().username("teamB").age(10).build());
        em.persist(Member.builder().username("teamC").age(10).build());

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조회, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
//                .join(member.team, team)
//                .on(team.name.eq("teamA"))
//                .where(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계가 없는 엔티티 '외부 조인'
     * 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(Member.builder().username("teamA").age(10).build());
        em.persist(Member.builder().username("teamB").age(10).build());
        em.persist(Member.builder().username("teamC").age(10).build());

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
//                .join(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */

    @Test
    public void subQuery() {

        /** -JPQL-
         * select
         *         member1
         *     from
         *         Member member1
         *     where
         *         member1.age = (
         *             select
         *                 max(memberSub.age)
         *             from
         *                 Member memberSub
         *         )
         */

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */

    @Test
    public void subQueryGoe() {

        /**
         * select
         *         member1
         *     from
         *         Member member1
         *     where
         *         member1.age >= (
         *             select
         *                 avg(memberSub.age)
         *             from
         *                 Member memberSub
         *         )
         */
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    @Test
    public void subQueryIn() {

        /**
         * select
         *         member1
         *     from
         *         Member member1
         *     where
         *         member1.age in (
         *             select
         *                 memberSub.age
         *             from
         *                 Member memberSub
         *             where
         *                 memberSub.age > ?1
         *         )
         */

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }


    /**
     * from 절의 서브쿼리 한계
     * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다. 당연히 Querydsl
     * 도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다. Querydsl도
     * 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.
     *
     * from 절의 서브쿼리 해결방안
     * 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     * 3. nativeSQL을 사용한다.
     */
    @Test
    public void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() {

        /** jpql
         * select
         *         case
         *             when member1.age = ?1 then ?2
         *             when member1.age = ?3 then ?4
         *             else '기타'
         *         end
         *     from
         *         Member member1
         */

        /** spl
         * select
         *             case
         *                 when member0_.age=? then ?
         *                 when member0_.age=? then ?
         *                 else '기타'
         *             end as col_0_0_
         *         from
         *             member member0_
         */

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {

        /**
         * select
         *         case
         *             when (member1.age between ?1 and ?2) then ?3
         *             when (member1.age between ?4 and ?5) then ?6
         *             else '기타'
         *         end
         *     from
         *         Member member1
         */

        /**
         *  select
         *             case
         *                 when member0_.age between ? and ? then ?
         *                 when member0_.age between ? and ? then ?
         *                 else '기타'
         *             end as col_0_0_
         *         from
         *             member member0_
         */
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * orderBy에서 Case 문 함께 사용하기 예제
     * <p>
     * 예를 들어서 다음과 같은 임의의 순서로 회원을 출력하고 싶다면?
     * 1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
     * 2. 0 ~ 20살 회원 출력
     * 3. 21 ~ 30살 회원 출력
     */

    @Test
    public void orderByCase() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);

            System.out.println("username = " + username + " age =" + age + " rank = " + rank);
        }
    }

    /**
     * 위와 같이 최적화가 가능하면 SQL에 constant 값을 넘기지 않는다. 상수를 더하는 것 처럼 최적화가
     * 어려우면 SQL에 constant 값을 넘긴다.
     */

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    /**
     *  member.age.stringValue() 부분이 중요한데, 문자가 아닌 다른 타입들은 stringValue() 로
     *  문자로 변환할 수 있다. 이 방법은 ENUM을 처리할 때도 자주 사용한다.
     */
    @Test
    public void concat() {

        /**
         * select
         *         concat(concat(member1.username,
         *         ?1),
         *         str(member1.age))
         *     from
         *         Member member1
         *     where
         *         member1.username = ?2
         */

        /**
         * select
         *             ((member0_.username||?)||cast(member0_.age as character varying)) as col_0_0_
         *         from
         *             member member0_
         *         where
         *             member0_.username=?
         */

        // {username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
