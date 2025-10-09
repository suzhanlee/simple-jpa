# Phase 4: 핵심 API 구현 (EntityManager, EntityManagerFactory, EntityTransaction)

## 개요

**왜 이 단계?** Phase 1~3에서 구축한 메타데이터, JDBC 인프라, SQL 생성 엔진을 통합하여 실제 사용자가 사용할 수 있는 JPA 핵심 API를 구현합니다. 이는 ORM의 공개 인터페이스로서, 모든 영속성 작업의 진입점입니다.

---

## 구현 단계

### Step 4.1: EntityManagerFactory 구현

- [x] EntityManagerFactory 인터페이스 및 구현체
- [x] Configuration 로딩 (persistence.xml 없이 코드 기반)
- [x] MetadataRegistry 초기화
- [x] ConnectionProvider 설정
- [x] EntityManager 생성 팩토리 메서드

**주요 컴포넌트**:
- `EntityManagerFactory`: 팩토리 인터페이스
- `EntityManagerFactoryImpl`: 구현체
- `PersistenceConfiguration`: 설정 관리
- `ConnectionProvider`: Connection 제공 (Phase 2 재사용)

**핵심 기능**:
- 애플리케이션 당 하나의 인스턴스 (Singleton 패턴)
- EntityManager 생성 및 관리
- 리소스 초기화 및 정리
- Thread-safe 구현

**예시**:
```java
// EntityManagerFactory 생성
EntityManagerFactory emf = Persistence.createEntityManagerFactory(config);

// EntityManager 생성
EntityManager em = emf.createEntityManager();

// 종료
em.close();
emf.close();
```

---

### Step 4.2: EntityTransaction 구현

- [ ] EntityTransaction 인터페이스 및 구현체
- [ ] Transaction 시작/커밋/롤백
- [ ] JdbcTransaction 연계 (Phase 2)
- [ ] Transaction 상태 관리
- [ ] Connection 생명주기 관리

**주요 컴포넌트**:
- `EntityTransaction`: 트랜잭션 인터페이스
- `EntityTransactionImpl`: 구현체
- `TransactionCoordinator`: 트랜잭션 조정 (Phase 2 재사용)

**핵심 기능**:
- `begin()`: 트랜잭션 시작
- `commit()`: 변경사항 커밋
- `rollback()`: 변경사항 롤백
- `isActive()`: 활성 상태 확인
- Connection과 트랜잭션 1:1 매핑

**예시**:
```java
EntityTransaction tx = em.getTransaction();
try {
    tx.begin();
    // 비즈니스 로직
    em.persist(user);
    tx.commit();
} catch (Exception e) {
    if (tx.isActive()) {
        tx.rollback();
    }
}
```

---

### Step 4.3: EntityManager 기본 구조

- [ ] EntityManager 인터페이스 및 구현체
- [ ] 생명주기 관리 (open/close)
- [ ] Transaction 관리 위임
- [ ] Connection 관리 위임
- [ ] CRUD 메서드 시그니처 정의 (구현은 Phase 6)

**주요 컴포넌트**:
- `EntityManager`: 인터페이스
- `EntityManagerImpl`: 구현체
- `PersistenceContext`: 영속성 컨텍스트 (Phase 5에서 완성)

**핵심 기능**:
- `persist(entity)`: 엔티티 영속화 (Phase 6 구현)
- `find(Class, id)`: 엔티티 조회 (Phase 6 구현)
- `merge(entity)`: 엔티티 병합 (Phase 6 구현)
- `remove(entity)`: 엔티티 삭제 (Phase 6 구현)
- `flush()`: 변경사항 DB 반영 (Phase 6 구현)
- `getTransaction()`: 트랜잭션 획득
- `close()`: 리소스 정리

**예시**:
```java
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

try {
    tx.begin();

    // CRUD 작업 (Phase 6에서 구현)
    User user = new User("John", "john@email.com");
    em.persist(user);  // INSERT

    User found = em.find(User.class, 1L);  // SELECT
    found.setName("John Updated");  // UPDATE (Dirty Checking)

    em.remove(found);  // DELETE

    tx.commit();
} catch (Exception e) {
    tx.rollback();
} finally {
    em.close();
}
```

---

### Step 4.4: Persistence (유틸리티 클래스)

- [ ] Persistence 유틸리티 클래스
- [ ] createEntityManagerFactory() 메서드
- [ ] Configuration 빌더 패턴
- [ ] 편의 메서드 제공

**주요 컴포넌트**:
- `Persistence`: 정적 유틸리티 클래스
- `PersistenceConfiguration`: 설정 빌더

**핵심 기능**:
- EntityManagerFactory 생성 진입점
- 설정 기반 초기화
- 편의 메서드 제공

**예시**:
```java
// 설정 생성
PersistenceConfiguration config = PersistenceConfiguration.builder()
    .url("jdbc:h2:mem:testdb")
    .username("sa")
    .password("")
    .driver("org.h2.Driver")
    .addEntityClass(User.class)
    .addEntityClass(Order.class)
    .build();

// EntityManagerFactory 생성
EntityManagerFactory emf = Persistence.createEntityManagerFactory(config);
```

---

## 현재 디렉토리 구조

```
simple-jpa/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── io/
│   │           └── simplejpa/
│   │               ├── core/                           # 🆕 Phase 4
│   │               │   ├── EntityManagerFactory.java  # 팩토리 인터페이스
│   │               │   ├── EntityManagerFactoryImpl.java
│   │               │   ├── EntityManager.java         # EM 인터페이스
│   │               │   ├── EntityManagerImpl.java
│   │               │   ├── EntityTransaction.java     # 트랜잭션 인터페이스
│   │               │   ├── EntityTransactionImpl.java
│   │               │   ├── Persistence.java           # 유틸리티
│   │               │   └── PersistenceConfiguration.java
│   │               ├── engine/
│   │               │   ├── connection/
│   │               │   │   ├── ConnectionProvider.java
│   │               │   │   ├── DriverManagerConnectionProvider.java
│   │               │   │   └── ConnectionConfiguration.java
│   │               │   ├── jdbc/
│   │               │   │   ├── JdbcExecutor.java
│   │               │   │   ├── ParameterBinder.java
│   │               │   │   ├── ResultSetExtractor.java
│   │               │   │   └── EntityResultSetExtractor.java
│   │               │   └── sql/
│   │               │       ├── SqlBuilder.java
│   │               │       ├── SqlIndenter.java
│   │               │       ├── InsertSqlGenerator.java
│   │               │       ├── SelectSqlGenerator.java
│   │               │       ├── UpdateSqlGenerator.java
│   │               │       ├── DeleteSqlGenerator.java
│   │               │       ├── WhereClauseBuilder.java
│   │               │       ├── Operator.java
│   │               │       ├── Condition.java
│   │               │       ├── SqlWithParameters.java
│   │               │       └── ParameterCollector.java
│   │               ├── metadata/
│   │               │   ├── EntityMetadata.java
│   │               │   ├── AttributeMetadata.java
│   │               │   ├── IdentifierMetadata.java
│   │               │   └── MetadataRegistry.java
│   │               ├── transaction/
│   │               │   ├── TransactionCoordinator.java
│   │               │   ├── JdbcTransaction.java
│   │               │   └── TransactionStatus.java
│   │               └── util/
│   │                   ├── TypeConverter.java
│   │                   └── ReflectionUtils.java
│   └── test/
│       ├── java/
│       │   └── io/
│       │       └── simplejpa/
│       │           ├── core/                          # 🆕 Core API 테스트
│       │           │   ├── EntityManagerFactoryTest.java
│       │           │   ├── EntityManagerTest.java
│       │           │   └── EntityTransactionTest.java
│       │           └── integration/
│       │               └── CoreApiIntegrationTest.java
│       └── resources/
│           └── test-db.properties
```

---

## 구현 순서 요약

1. **EntityManagerFactory** → 모든 것의 시작점, 싱글톤 패턴
2. **EntityTransaction** → 트랜잭션 관리, Connection 생명주기
3. **EntityManager 기본 구조** → CRUD 시그니처만 (구현은 Phase 6)
4. **Persistence 유틸리티** → 편의 메서드, 설정 관리

---

## 핵심 의존성

- **Phase 1 메타데이터**: MetadataRegistry, EntityMetadata 활용
- **Phase 2 JDBC 인프라**: ConnectionProvider, TransactionCoordinator 활용
- **Phase 3 SQL 생성**: Phase 6에서 SQL Generator들을 사용할 준비
- **Thread Safety**: EntityManagerFactory는 Thread-safe, EntityManager는 Thread-unsafe

---

## 테스트 전략

- **단위 테스트**: 각 컴포넌트의 생명주기 및 상태 전이 검증
- **통합 테스트**: EntityManagerFactory → EntityManager → EntityTransaction 전체 플로우
- **리소스 관리 테스트**: Connection 누수 방지, 정상 종료 검증
- **예외 처리 테스트**: 롤백 시나리오, 리소스 정리 검증

---

## EntityManager 생명주기

```
EntityManagerFactory (Singleton, Thread-safe)
    ↓ createEntityManager()
EntityManager (Instance per thread, Thread-unsafe)
    ↓ getTransaction()
EntityTransaction
    ↓ begin()
[CRUD Operations - Phase 6에서 구현]
    ↓ commit() / rollback()
Transaction End
    ↓ close()
EntityManager Closed
    ↓ close()
EntityManagerFactory Closed
```

---

## 핵심 설계 원칙

### 1. EntityManagerFactory
- **Singleton per Application**: 애플리케이션 당 하나
- **Thread-safe**: 여러 스레드에서 동시 접근 가능
- **Expensive to create**: 초기화 비용이 높음 (한 번만 생성)
- **Cheap to use**: EntityManager 생성은 가벼움

### 2. EntityManager
- **Instance per Thread/Request**: 스레드마다 독립적인 인스턴스
- **Thread-unsafe**: 동시 접근 불가
- **Short-lived**: 트랜잭션 단위로 생성/종료
- **Stateful**: 영속성 컨텍스트 보유 (Phase 5)

### 3. EntityTransaction
- **1:1 with EntityManager**: EntityManager 당 하나
- **1:1 with Connection**: Transaction 당 하나의 Connection
- **Explicit Control**: 명시적 begin/commit/rollback

---

## Phase 4 완료 후 가능한 것

- EntityManagerFactory, EntityManager, EntityTransaction API 사용 가능
- 트랜잭션 기반 작업 구조 완성
- Phase 6에서 CRUD 구현 시 즉시 통합 가능
- 표준 JPA API와 유사한 사용 경험

---

## 다음 단계 (Phase 5 예고)

Phase 5에서는 **영속성 컨텍스트 (PersistenceContext)**를 구현하여 1차 캐시, 엔티티 상태 관리, 쓰기 지연 등 JPA의 핵심 메커니즘을 구축합니다.

---

## JPA 표준 API와의 비교

### 표준 JPA
```java
// persistence.xml 기반
EntityManagerFactory emf = Persistence.createEntityManagerFactory("myPersistenceUnit");
```

### Simple JPA
```java
// 코드 기반 설정
PersistenceConfiguration config = PersistenceConfiguration.builder()
    .url("jdbc:h2:mem:testdb")
    .addEntityClass(User.class)
    .build();

EntityManagerFactory emf = Persistence.createEntityManagerFactory(config);
```

**차이점**:
- XML 없이 코드로 설정 (간단함)
- 핵심 기능만 지원
- 학습 목적으로 단순화

**공통점**:
- API 구조 유사
- 사용 패턴 동일
- 생명주기 동일

---

## 예제: 전체 플로우

```java
// 1. 설정
PersistenceConfiguration config = PersistenceConfiguration.builder()
    .url("jdbc:h2:mem:testdb")
    .username("sa")
    .password("")
    .addEntityClass(User.class)
    .build();

// 2. EntityManagerFactory 생성 (애플리케이션 시작 시 1회)
EntityManagerFactory emf = Persistence.createEntityManagerFactory(config);

// 3. EntityManager 생성 (요청마다)
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

try {
    // 4. 트랜잭션 시작
    tx.begin();

    // 5. 비즈니스 로직 (Phase 6에서 구현)
    User user = new User("John", "john@email.com");
    em.persist(user);  // INSERT

    // 6. 커밋
    tx.commit();
} catch (Exception e) {
    // 7. 롤백
    if (tx.isActive()) {
        tx.rollback();
    }
    throw e;
} finally {
    // 8. EntityManager 종료
    em.close();
}

// 9. EntityManagerFactory 종료 (애플리케이션 종료 시)
emf.close();
```

---

## Phase 4 핵심 체크리스트

### EntityManagerFactory
- [ ] 싱글톤 패턴 구현
- [ ] Thread-safe 보장
- [ ] MetadataRegistry 초기화
- [ ] ConnectionProvider 초기화
- [ ] EntityManager 생성 메서드
- [ ] 리소스 정리 (close)

### EntityTransaction
- [ ] begin() 구현
- [ ] commit() 구현
- [ ] rollback() 구현
- [ ] isActive() 구현
- [ ] Connection 생명주기 관리
- [ ] JdbcTransaction 연계

### EntityManager
- [ ] 생명주기 관리 (open/close)
- [ ] Transaction 획득 메서드
- [ ] CRUD 메서드 시그니처 정의
- [ ] Connection 관리
- [ ] 상태 검증 (closed 체크)

### Persistence
- [ ] createEntityManagerFactory() 구현
- [ ] Configuration 빌더 제공
- [ ] 편의 메서드 제공

---

이제 Phase 4를 시작할 준비가 되었습니다! 🚀