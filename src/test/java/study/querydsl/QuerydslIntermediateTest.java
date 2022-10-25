package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

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
         * 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
         * DTO의 package이름을 다 적어줘야해서 지저분함
         * 생성자 방식만 지원함
         */

        String qlString = "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m";
        List<MemberDto> result = em.createQuery(qlString, MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl 빈 생성(Bean population)
     *  결과를 DTO 반환할 때 사용
     *
     *  다음 3가지 방법 지원
     *      ㄴ   프로퍼티 접근
     *      ㄴ   필드 직접 접근
     *      ㄴ   생성자 사용
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
                .select(Projections.fields(MemberDto.class, //getter & setter 무시하고 필드값에 프로젝션값을 직접 입력(삽입)
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
                .select(Projections.constructor(MemberDto.class, //생성자 방식
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
                .select(Projections.constructor(UserDto.class, //생성자 방식
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
     * ExpressionUtils.as(source,alias) : 필드나, 서브 쿼리에 별칭 적용
     * username.as("memberName") : 필드에 별칭 적용
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
                        //서브쿼리는 ExpressionUtils로 감싸서 사용해야한다. (alias 줄 때)
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


}
