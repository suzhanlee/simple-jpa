# Phase 7: 쿼리 처리 (기본)

## 개요
**왜 필요한가?** Phase 6까지는 `find(id)`로 단일 엔티티 조회만 가능했습니다. 실무에서는 복잡한 조건의 다중 엔티티 조회가 필수입니다. JPQL(Java Persistence Query Language)을 구현하여 객체 지향 쿼리를 가능하게 합니다.

**Phase 7의 목표**: 간단한 SELECT 쿼리만 지원. WHERE 조건과 파라미터 바인딩에 집중.

## 구현 단계

### Step 7.1: Query 인터페이스 설계
- [x] Query 인터페이스 정의
- [x] TypedQuery 인터페이스 정의 (제네릭 타입 안전성)
- [x] QueryImpl 기본 구현체
- [x] TypedQueryImpl 기본 구현체

**핵심 메서드**:
```java
public interface Query {
    List<Object> getResultList();
    Object getSingleResult();
    Query setParameter(String name, Object value);
    Query setParameter(int position, Object value);
}

public interface TypedQuery<T> {
    List<T> getResultList();
    T getSingleResult();
    TypedQuery<T> setParameter(String name, Object value);
    TypedQuery<T> setParameter(int position, Object value);
}
```

### Step 7.2: JPQL 파서 (간단한 SELECT만)
- [ ] JpqlParser 클래스 구현
- [ ] SELECT 절 파싱
- [ ] FROM 절 파
- [ ] WHERE 절 파싱 (간단한 조건만)
- [ ] AST(Abstract Syntax Tree) 구조 정의

**지원 범위 (Phase 7)**:
```sql
-- 지원
SELECT u FROM User u
SELECT u FROM User u WHERE u.name = :name
SELECT u FROM User u WHERE u.id = ?1
SELECT u FROM User u WHERE u.age > :age AND u.name = :name

-- 미지원 (Phase 8 이후)
SELECT u FROM User u JOIN u.orders o
SELECT u FROM User u ORDER BY u.name
SELECT COUNT(u) FROM User u
```

### Step 7.3: JPQL → SQL 변환
- [ ] QueryTranslator 클래스 구현
- [ ] 엔티티 이름 → 테이블 이름 변환 (메타데이터 활용)
- [ ] 필드 이름 → 컬럼 이름 변환
- [ ] WHERE 조건 변환
- [ ] NamedParameter 처리 (:name)
- [ ] PositionalParameter 처리 (?1)

**변환 예시**:
```
JPQL: SELECT u FROM User u WHERE u.name = :name
 ↓
SQL:  SELECT * FROM users WHERE name = ?
```

### Step 7.4: Parameter Binding
- [ ] ParameterBinder 확장
- [ ] Named Parameter 바인딩 (:name → ?)
- [ ] Positional Parameter 바인딩 (?1 → ?)
- [ ] 파라미터 타입 검증
- [ ] NULL 처리

### Step 7.5: EntityManager 쿼리 메서드 추가
- [ ] `createQuery(String jpql)` 구현
- [ ] `createQuery(String jpql, Class<T> resultClass)` 구현
- [ ] Query 실행 → ResultSet → Entity 변환
- [ ] PersistenceContext와 통합 (캐시 활용)

---

## 현재 디렉토리 구조

```
simple-jpa/
└── src/
    └── main/
        └── java/
            └── io/
                └── simplejpa/
                    ├── query/
                    │   ├── Query.java              # Query 인터페이스
                    │   ├── TypedQuery.java         # TypedQuery 인터페이스
                    │   ├── QueryImpl.java          # Query 구현체
                    │   ├── TypedQueryImpl.java     # TypedQuery 구현체
                    │   ├── jpql/
                    │   │   ├── JpqlParser.java     # JPQL 파서
                    │   │   ├── QueryTranslator.java # JPQL → SQL 변환
                    │   │   ├── ast/                # AST 노드
                    │   │   │   ├── SelectStatement.java
                    │   │   │   ├── FromClause.java
                    │   │   │   ├── WhereClause.java
                    │   │   │   └── Condition.java
                    │   │   └── parameter/          # 파라미터 처리
                    │   │       ├── NamedParameter.java
                    │   │       └── PositionalParameter.java
                    │   └── criteria/               # (Phase 8+)
                    │       └── ...
                    └── engine/
                        └── jdbc/
                            └── ParameterBinder.java # (확장)
```

---

## 구현 순서 요약

1. **Query 인터페이스** → 기본 API 설계
2. **JPQL 파서** → 간단한 SELECT/FROM/WHERE 파싱
3. **QueryTranslator** → 엔티티/필드 → 테이블/컬럼 변환
4. **Parameter Binding** → Named/Positional 파라미터 처리
5. **EntityManager 통합** → createQuery() 메서드 구현

---

## 핵심 알고리즘

### JPQL 파싱 알고리즘
```
Input: "SELECT u FROM User u WHERE u.name = :name"
  ↓
1. Tokenize: [SELECT, u, FROM, User, u, WHERE, u.name, =, :name]
2. Parse SELECT: alias = "u"
3. Parse FROM: entity = "User", alias = "u"
4. Parse WHERE: condition = "u.name = :name"
  ↓
Output: SelectStatement {
  alias: "u",
  entityName: "User",
  whereClause: "u.name = :name",
  parameters: [":name"]
}
```

### JPQL → SQL 변환 알고리즘
```
1. EntityMetadata 조회: User → users 테이블
2. 필드 → 컬럼 매핑: name → name 컬럼
3. Named Parameter 변환: :name → ?
4. SQL 생성: SELECT * FROM users WHERE name = ?
```

### Parameter Binding 순서
```
JPQL: WHERE u.name = :name AND u.age > :age
Parameters: {name: "John", age: 25}
  ↓
1. Named Parameter 추출: [:name, :age]
2. 순서 매핑: :name → 1, :age → 2
3. PreparedStatement 바인딩:
   stmt.setString(1, "John")
   stmt.setInt(2, 25)
```

---

## 테스트 시나리오

### 기본 조회
```java
TypedQuery<User> query = em.createQuery(
    "SELECT u FROM User u WHERE u.name = :name",
    User.class
);
query.setParameter("name", "John");
List<User> users = query.getResultList();
```

### 다중 조건
```java
TypedQuery<User> query = em.createQuery(
    "SELECT u FROM User u WHERE u.age > :age AND u.name = :name",
    User.class
);
query.setParameter("age", 20);
query.setParameter("name", "John");
List<User> users = query.getResultList();
```

### Positional Parameter
```java
TypedQuery<User> query = em.createQuery(
    "SELECT u FROM User u WHERE u.id = ?1",
    User.class
);
query.setParameter(1, 100L);
User user = query.getSingleResult();
```

---

## 설계 결정사항

### 1. JPQL 지원 범위
- **Phase 7**: SELECT, FROM, WHERE (단순 조건)만 지원
- **Phase 8 이후**: JOIN, ORDER BY, GROUP BY, 집계 함수 추가

### 2. 파라미터 바인딩 방식
- Named Parameter (`:name`) 우선 권장
- Positional Parameter (`?1`) 보조 지원
- 혼용 불가 (에러 처리)

### 3. 캐시 통합
- Query 결과도 PersistenceContext에 저장
- 동일 ID 엔티티는 1차 캐시에서 반환

### 4. 예외 처리
- JPQL 파싱 실패 → `JpqlSyntaxException`
- 엔티티 없음 → `EntityNotFoundException`
- 파라미터 누락 → `IllegalArgumentException`
- 단일 결과 기대 but 다중 반환 → `NonUniqueResultException`
- 결과 없음 (getSingleResult) → `NoResultException`

---

## Phase 7 완료 조건

1. 간단한 JPQL SELECT 쿼리 파싱 성공
2. JPQL → SQL 변환 정상 작동
3. Named/Positional Parameter 바인딩 정상 작동
4. Query/TypedQuery 인터페이스 정상 작동
5. PersistenceContext와 통합 (캐시 활용)
6. 통합 테스트 통과

---

## 다음 Phase 미리보기

**Phase 8: 관계 매핑**
- @OneToOne, @ManyToOne, @OneToMany, @ManyToMany
- JOIN 쿼리 지원
- FetchType (EAGER/LAZY)
- Cascade 옵션

**Phase 9: 지연 로딩 (Proxy)**
- JDK Dynamic Proxy
- LazyLoadingInvocationHandler
- 프록시 초기화 전략
