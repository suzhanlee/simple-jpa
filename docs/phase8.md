# Phase 8: 관계 매핑

## 개요
**왜 필요한가?** Phase 7까지는 단일 엔티티만 다룰 수 있었습니다. 실무에서는 엔티티 간의 관계(User-Order, Post-Comment 등)가 필수적입니다. JPA의 핵심 기능인 관계 매핑(@OneToOne, @ManyToOne, @OneToMany, @ManyToMany)을 구현하여 객체 그래프 탐색을 가능하게 합니다.

**Phase 8의 목표**: 기본 관계 매핑 구현. EAGER/LAZY 로딩과 Cascade 옵션 지원.

## 구현 단계

### Step 8.1: 관계 매핑 애노테이션 정의
- [x] @OneToOne 애노테이션
- [x] @ManyToOne 애노테이션
- [x] @OneToMany 애노테이션
- [x] @ManyToMany 애노테이션
- [x] @JoinColumn 애노테이션 (외래키 지정) 
- [x] @JoinTable 애노테이션 (다대다 중간 테이블)
- [x] FetchType 열거형 (EAGER, LAZY)
- [x] CascadeType 열거형 (PERSIST, MERGE, REMOVE, REFRESH, DETACH, ALL)

**애노테이션 설계**:
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManyToOne {
    FetchType fetch() default FetchType.EAGER;
    CascadeType[] cascade() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneToMany {
    String mappedBy() default "";  // 양방향 관계에서 주인 지정
    FetchType fetch() default FetchType.LAZY;
    CascadeType[] cascade() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinColumn {
    String name();  // 외래키 컬럼 이름
    boolean nullable() default true;
}
```

### Step 8.2: RelationshipMetadata 구조 설계
- [x] RelationshipMetadata 클래스
- [x] RelationType 열거형 (ONE_TO_ONE, MANY_TO_ONE, ONE_TO_MANY, MANY_TO_MANY)
- [x] ForeignKeyMetadata 클래스
- [x] JoinTableMetadata 클래스 (ManyToMany용)
- [x] EntityMetadata에 관계 정보 통합

**메타데이터 구조**:
```java
public class RelationshipMetadata {
    private RelationType relationType;
    private Class<?> targetEntityClass;
    private String targetEntityName;
    private FetchType fetchType;
    private CascadeType[] cascadeTypes;

    // 외래키 정보 (@ManyToOne, @OneToOne)
    private String foreignKeyColumn;

    // 양방향 관계 정보 (@OneToMany)
    private String mappedBy;

    // 다대다 중간 테이블 정보 (@ManyToMany)
    private JoinTableMetadata joinTable;
}
```

### Step 8.3: 관계 매핑 메타데이터 추출
- [ ] AnnotationProcessor에 관계 애노테이션 처리 추가
- [ ] 외래키 정보 추출
- [ ] 양방향 관계 검증 (mappedBy 일관성)
- [ ] 다대다 중간 테이블 정보 추출
- [ ] MetadataRegistry에 관계 정보 저장

**처리 예시**:
```java
// User 엔티티
@Entity
public class User {
    @Id
    private Long id;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;
}

// Order 엔티티
@Entity
public class Order {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
}

// → RelationshipMetadata 생성
// User.orders: ONE_TO_MANY, target=Order, mappedBy="user", LAZY
// Order.user: MANY_TO_ONE, target=User, foreignKey="user_id", EAGER
```

### Step 8.4: @ManyToOne 구현
- [ ] ManyToOne 로딩 로직 (EntityLoader 확장)
- [ ] 외래키 기반 조인 쿼리 생성
- [ ] EAGER 로딩: 즉시 연관 엔티티 로드
- [ ] LAZY 로딩: 프록시 반환 (Phase 9 연동 준비)
- [ ] PersistenceContext 캐시 통합

**SQL 생성**:
```
JPQL: SELECT o FROM Order o
  ↓ (ManyToOne EAGER)
SQL:  SELECT o.*, u.*
      FROM orders o
      LEFT JOIN users u ON o.user_id = u.id
```

### Step 8.5: @OneToMany 구현
- [ ] OneToMany 로딩 로직
- [ ] mappedBy 처리 (역방향 외래키 참조)
- [ ] 컬렉션 초기화 (List, Set 지원)
- [ ] EAGER: 추가 SELECT 쿼리 실행
- [ ] LAZY: 컬렉션 프록시 반환 (Phase 9)
- [ ] N+1 문제 인지 (Phase 8+에서 해결)

**로딩 전략**:
```
EAGER 로딩:
1. User 조회: SELECT * FROM users WHERE id = ?
2. Orders 조회: SELECT * FROM orders WHERE user_id = ?

LAZY 로딩:
1. User 조회: SELECT * FROM users WHERE id = ?
2. orders 필드: 빈 프록시 컬렉션 반환
3. user.getOrders() 호출 시: SELECT * FROM orders WHERE user_id = ?
```

### Step 8.6: @OneToOne 구현
- [ ] OneToOne 로딩 로직
- [ ] 양방향 OneToOne 처리
- [ ] 외래키 소유 측 결정
- [ ] EAGER/LAZY 로딩 지원
- [ ] Optional OneToOne (nullable 처리)

**설계 결정**:
```
// User ←→ Profile (1:1)
// 외래키는 Profile 테이블에 위치

@Entity
public class User {
    @OneToOne(mappedBy = "user")
    private Profile profile;
}

@Entity
public class Profile {
    @OneToOne
    @JoinColumn(name = "user_id")  // 외래키 소유 측
    private User user;
}
```

### Step 8.7: @ManyToMany 구현 (선택사항)
- [ ] ManyToMany 로딩 로직
- [ ] 중간 테이블(Join Table) 자동 생성
- [ ] @JoinTable 애노테이션 처리
- [ ] 양방향 ManyToMany 지원
- [ ] 컬렉션 양쪽 동기화

**중간 테이블 구조**:
```
// Student ←→ Course (N:M)

@Entity
public class Student {
    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses;
}

// SQL 생성:
// CREATE TABLE student_course (
//     student_id BIGINT,
//     course_id BIGINT,
//     PRIMARY KEY (student_id, course_id)
// )
```

### Step 8.8: Cascade 옵션 구현
- [ ] CascadeType.PERSIST: 연관 엔티티도 함께 저장
- [ ] CascadeType.MERGE: 연관 엔티티도 함께 병합
- [ ] CascadeType.REMOVE: 연관 엔티티도 함께 삭제
- [ ] CascadeType.REFRESH: 연관 엔티티도 함께 새로고침
- [ ] CascadeType.DETACH: 연관 엔티티도 함께 분리
- [ ] CascadeType.ALL: 모든 연산 전파

**Cascade 처리**:
```java
@Entity
public class User {
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;
}

// em.persist(user) 호출 시:
// 1. User INSERT
// 2. orders의 각 Order도 자동 INSERT (Cascade)
```

### Step 8.9: EntityPersister 관계 통합
- [ ] persist() 시 Cascade 처리
- [ ] 외래키 자동 설정
- [ ] 양방향 관계 동기화
- [ ] ActionQueue에 관계 액션 추가
- [ ] 순환 참조 방지

### Step 8.10: JPQL JOIN 쿼리 지원
- [ ] JpqlParser에 JOIN 구문 추가
- [ ] INNER JOIN 파싱
- [ ] LEFT JOIN 파싱
- [ ] Fetch Join 지원 (JOIN FETCH)
- [ ] QueryTranslator에 JOIN SQL 생성

**JPQL JOIN 예시**:
```sql
-- Inner Join
SELECT o FROM Order o JOIN o.user u WHERE u.name = :name

-- Fetch Join (N+1 문제 해결)
SELECT u FROM User u JOIN FETCH u.orders

-- Left Join
SELECT u FROM User u LEFT JOIN u.orders o
```

---

## 현재 디렉토리 구조

```
simple-jpa/
└── src/
    └── main/
        └── java/
            └── io/
                └── simplejpa/
                    ├── annotation/
                    │   ├── relation/          # 새로 추가
                    │   │   ├── OneToOne.java
                    │   │   ├── ManyToOne.java
                    │   │   ├── OneToMany.java
                    │   │   ├── ManyToMany.java
                    │   │   ├── JoinColumn.java
                    │   │   └── JoinTable.java
                    │   └── FetchType.java     # 새로 추가
                    │   └── CascadeType.java   # 새로 추가
                    │
                    ├── metadata/
                    │   ├── relation/          # 새로 추가
                    │   │   ├── RelationshipMetadata.java
                    │   │   ├── RelationType.java
                    │   │   ├── ForeignKeyMetadata.java
                    │   │   └── JoinTableMetadata.java
                    │   └── EntityMetadata.java  # (확장: relationships 필드 추가)
                    │
                    ├── mapping/
                    │   └── AnnotationProcessor.java  # (확장: 관계 애노테이션 처리)
                    │
                    ├── persister/
                    │   ├── EntityPersister.java      # (확장: Cascade 처리)
                    │   ├── EntityLoader.java         # (확장: 관계 로딩)
                    │   ├── RelationshipLoader.java   # 새로 추가
                    │   └── CascadeProcessor.java     # 새로 추가
                    │
                    ├── query/
                    │   └── jpql/
                    │       ├── JpqlParser.java       # (확장: JOIN 파싱)
                    │       ├── QueryTranslator.java  # (확장: JOIN SQL 생성)
                    │       └── ast/
                    │           ├── JoinClause.java   # 새로 추가
                    │           └── JoinType.java     # 새로 추가
                    │
                    └── cache/
                        └── PersistenceContext.java   # (확장: 관계 캐싱)
```

---

## 구현 순서 요약

1. **관계 애노테이션 정의** → @ManyToOne, @OneToMany, @JoinColumn 등
2. **RelationshipMetadata** → 관계 메타정보 구조 설계
3. **메타데이터 추출** → AnnotationProcessor 확장
4. **@ManyToOne 구현** → 외래키 기반 로딩
5. **@OneToMany 구현** → 역방향 컬렉션 로딩
6. **@OneToOne 구현** → 양방향 1:1 관계
7. **@ManyToMany 구현** → 중간 테이블 처리 (선택)
8. **Cascade 구현** → 연산 전파
9. **JPQL JOIN** → 조인 쿼리 지원
10. **통합 테스트** → 다양한 관계 시나리오 검증

---

## 핵심 알고리즘

### @ManyToOne EAGER 로딩
```
1. Order 엔티티 조회 시 RelationshipMetadata 확인
2. ManyToOne + EAGER 발견
3. JOIN SQL 생성:
   SELECT o.*, u.* FROM orders o LEFT JOIN users u ON o.user_id = u.id
4. ResultSet에서 Order + User 동시 추출
5. Order.user 필드에 User 엔티티 설정
6. PersistenceContext에 둘 다 저장
```

### @OneToMany LAZY 로딩 (Phase 9 연계)
```
1. User 엔티티 조회 시 RelationshipMetadata 확인
2. OneToMany + LAZY 발견
3. orders 필드: 빈 프록시 컬렉션 할당
4. user.getOrders() 호출 시:
   a. 프록시 초기화 체크
   b. SELECT * FROM orders WHERE user_id = ? 실행
   c. Order 엔티티들 로드
   d. 컬렉션에 추가
   e. 프록시 초기화 완료 표시
```

### Cascade.PERSIST 처리
```
User user = new User();
Order order1 = new Order();
Order order2 = new Order();
user.setOrders(List.of(order1, order2));

em.persist(user);  // Cascade.PERSIST 발생

처리 순서:
1. User 엔티티 persist() 호출
2. RelationshipMetadata 확인: orders → Cascade.PERSIST 있음
3. order1.persist() 자동 호출
4. order2.persist() 자동 호출
5. ActionQueue에 순서대로 추가:
   - InsertAction(user)
   - InsertAction(order1)
   - InsertAction(order2)
6. flush() 시 순차 실행
```

### 양방향 관계 동기화
```
// User ←→ Order (1:N, N:1)

// 설정 시 양쪽 동기화 필요
order.setUser(user);           // 외래키 설정
user.getOrders().add(order);   // 컬렉션 추가

// 편의 메서드 패턴 권장:
public void addOrder(Order order) {
    this.orders.add(order);
    order.setUser(this);  // 양방향 동기화
}
```

---

## 테스트 시나리오

### @ManyToOne EAGER 로딩
```java
@Entity
public class Order {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
}

// 테스트
Order order = em.find(Order.class, 1L);
assertNotNull(order.getUser());  // 즉시 로드됨
assertEquals("John", order.getUser().getName());
```

### @OneToMany LAZY 로딩
```java
@Entity
public class User {
    @Id
    private Long id;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;
}

// 테스트
User user = em.find(User.class, 1L);
// 이 시점에는 orders 로드 안됨 (프록시)

List<Order> orders = user.getOrders();  // 이 시점에 로드
assertEquals(3, orders.size());
```

### Cascade.PERSIST
```java
@Entity
public class User {
    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
    private List<Order> orders;
}

// 테스트
User user = new User();
Order order1 = new Order();
Order order2 = new Order();
user.addOrder(order1);
user.addOrder(order2);

em.persist(user);  // user + order1 + order2 모두 저장
em.flush();

assertNotNull(user.getId());
assertNotNull(order1.getId());
assertNotNull(order2.getId());
```

### JPQL JOIN FETCH (N+1 문제 해결)
```java
// N+1 문제 발생 (LAZY 로딩)
List<User> users = em.createQuery("SELECT u FROM User u", User.class)
    .getResultList();
// 1. SELECT * FROM users (1번)
// 2. 각 user마다 SELECT * FROM orders WHERE user_id = ? (N번)

// JOIN FETCH로 해결
List<User> users = em.createQuery(
    "SELECT u FROM User u JOIN FETCH u.orders",
    User.class
).getResultList();
// SELECT u.*, o.* FROM users u LEFT JOIN orders o ON u.id = o.user_id
// (1번의 쿼리로 모두 로드)
```

### @ManyToMany
```java
@Entity
public class Student {
    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses;
}

@Entity
public class Course {
    @ManyToMany(mappedBy = "courses")
    private List<Student> students;
}

// 테스트
Student student = new Student();
Course math = new Course("Math");
Course physics = new Course("Physics");

student.getCourses().add(math);
student.getCourses().add(physics);

em.persist(student);
em.persist(math);
em.persist(physics);
em.flush();

// student_course 테이블에 2개 레코드 삽입:
// (student.id, math.id)
// (student.id, physics.id)
```

---

## 설계 결정사항

### 1. 외래키 소유 측 (Owning Side)
- **다대일(@ManyToOne)**: 항상 외래키 소유 측
- **일대다(@OneToMany)**: mappedBy로 반대편 지정
- **일대일(@OneToOne)**: @JoinColumn이 있는 쪽이 소유 측
- **다대다(@ManyToMany)**: @JoinTable이 있는 쪽이 소유 측

### 2. 기본 FetchType
- **@ManyToOne**: EAGER (실무에서는 LAZY 권장)
- **@OneToMany**: LAZY
- **@OneToOne**: EAGER
- **@ManyToMany**: LAZY

### 3. Cascade 전략
- 부모-자식 관계(User-Order): CascadeType.ALL 권장
- 독립 엔티티 참조: Cascade 사용 자제
- REMOVE 사용 시 주의 (의도치 않은 삭제 방지)

### 4. 양방향 관계 관리
- 편의 메서드(addXxx, removeXxx) 패턴 권장
- 양쪽 동기화 필수
- mappedBy로 관계 주인 명시

### 5. N+1 문제 대응
- **Phase 8**: JPQL JOIN FETCH 제공
- **Phase 10**: Batch Fetching 추가 예정
- **Phase 10**: @EntityGraph 지원 검토

### 6. 프록시 전략 (Phase 9 연계)
- LAZY 관계는 프록시 객체 반환
- 컬렉션은 PersistentList/PersistentSet 래퍼
- 초기화 시점: 실제 메서드 호출 시

---

## 예외 처리

- **순환 참조 감지**: `CircularReferenceException`
- **mappedBy 불일치**: `MappingException`
- **외래키 제약 위반**: `ForeignKeyConstraintException`
- **Cascade 타입 불일치**: `IllegalCascadeException`
- **다대다 중간 테이블 없음**: `JoinTableNotFoundException`
- **프록시 초기화 실패**: `LazyInitializationException` (Phase 9)

---

## Phase 8 완료 조건

1. @ManyToOne, @OneToMany, @OneToOne 정상 작동
2. EAGER/LAZY FetchType 구분 동작
3. Cascade 옵션 정상 전파
4. JPQL JOIN / JOIN FETCH 쿼리 실행 성공
5. 양방향 관계 동기화 정상 작동
6. PersistenceContext와 관계 캐싱 통합
7. 통합 테스트 통과 (복잡한 관계 시나리오)

---

## 다음 Phase 미리보기

**Phase 9: 지연 로딩 (JDK Dynamic Proxy)**
- JDK Dynamic Proxy 기반 프록시 구현
- LazyLoadingInvocationHandler 개발
- 엔티티 인터페이스 규약 정의
- 프록시 초기화 전략
- PersistentCollection (List/Set 프록시)

**Phase 10: 고급 기능**
- 2차 캐시 (L2 Cache)
- Batch Fetching (N+1 최적화)
- @Version을 통한 Optimistic Locking
- DB 방언 (Dialect) - MySQL, PostgreSQL 등
- @EntityGraph (선택적 Fetch 전략)

---

## 성능 고려사항

### EAGER vs LAZY 선택 기준
```
EAGER 사용:
- 항상 함께 사용되는 관계 (Order → User)
- 조회 빈도가 높은 관계
- 단순 1:1 관계

LAZY 사용:
- 선택적으로 사용되는 관계 (User → Orders)
- 컬렉션 관계 (1:N, N:M)
- 대용량 연관 데이터
```

### N+1 문제 해결 전략
```
1. JOIN FETCH 사용 (즉시 로딩)
   SELECT u FROM User u JOIN FETCH u.orders

2. @BatchSize (Phase 10)
   @OneToMany(fetch = LAZY)
   @BatchSize(size = 10)
   private List<Order> orders;

3. EntityGraph (Phase 10)
   @EntityGraph(attributePaths = {"orders"})
   List<User> findAll();
```

### Cascade 성능 영향
```
- CascadeType.ALL: 연관 엔티티 수만큼 추가 INSERT
- 대량 데이터 시 Batch Insert 고려 (Phase 10)
- Cascade.REMOVE: 연관 삭제 전 SELECT 발생
```

---

## 참고 자료

- JPA 2.2 Specification - Section 2.9 (Entity Relationships)
- Hibernate ORM - Chapter 7 (Association Mappings)
- "Java Persistence with Hibernate" - Chapter 6 (Mapping Associations)
- "Pro JPA 2" - Chapter 4 (Object-Relational Mapping)
- N+1 문제: https://vladmihalcea.com/n-plus-1-query-problem/
- Fetch Strategies: https://thorben-janssen.com/entity-mappings-introduction-jpa-fetchtypes/
