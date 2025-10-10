# Simple JPA - 밑바닥부터 구현하기

## 프로젝트 개요
JPA(Java Persistence API)를 밑바닥부터 구현하여 ORM의 내부 동작 원리를 이해하는 프로젝트입니다.

## 아키텍처

### JPA 내부 구조 기반 설계

```
┌─────────────────────────────────────────────────┐
│                Core API Layer                   │
│        (EntityManager, EntityManagerFactory)    │
└────────────┬────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────┐
│           Metadata & Mapping Layer              │
│    (Entity 메타데이터, 애노테이션 처리, 매핑)    │
└────────────┬────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────┐
│         Persistence Context Layer               │
│  (영속성 컨텍스트, 1차 캐시, Dirty Checking)     │
└────────────┬────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────┐
│          Query & SQL Engine Layer               │
│        (JPQL 파싱, SQL 생성, 실행)              │
└────────────┬────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────┐
│          JDBC & Transaction Layer               │
│    (Connection 관리, 트랜잭션, DB 방언)         │
└─────────────────────────────────────────────────┘
```

## 핵심 컴포넌트

### 1. Core (핵심 API)
JPA의 공개 API를 제공하는 레이어
- **EntityManagerFactory**: EntityManager 생성 팩토리
- **EntityManager**: 영속성 관리의 진입점
- **EntityTransaction**: 트랜잭션 관리

### 2. Metadata (메타데이터 관리)
엔티티의 구조 정보를 관리
- **EntityMetadata**: 엔티티 클래스 메타정보
- **AttributeMetadata**: 필드/속성 메타정보
- **IdentifierMetadata**: Primary Key 정보
- **MetadataRegistry**: 메타데이터 저장소

### 3. Mapping (매핑 처리)
객체-테이블 매핑 처리
- **AnnotationProcessor**: 애노테이션 스캐닝
- **TableMapper**: 테이블-엔티티 매핑
- **ColumnMapper**: 컬럼-필드 매핑
- **RelationshipMapper**: 관계 매핑 (OneToMany, ManyToOne 등)

### 4. Persister (영속화 실행)
실제 CRUD 작업 수행
- **EntityPersister**: 엔티티 INSERT 처리
- **EntityLoader**: 엔티티 SELECT 처리
- **EntityUpdater**: 엔티티 UPDATE 처리
- **EntityDeleter**: 엔티티 DELETE 처리

### 5. Engine (SQL 엔진)
SQL 생성 및 실행
- **SqlGenerator**: SQL 문 생성
- **JdbcExecutor**: JDBC를 통한 SQL 실행
- **ResultSetExtractor**: ResultSet → 객체 변환
- **ParameterBinder**: PreparedStatement 파라미터 바인딩

### 6. Cache (캐시)
성능 최적화를 위한 캐싱
- **PersistenceContext**: 1차 캐시 (영속성 컨텍스트)
- **IdentityMap**: 엔티티 식별자 맵
- **SecondLevelCache**: 2차 캐시 (선택사항)

### 7. Transaction (트랜잭션)
트랜잭션 관리
- **TransactionCoordinator**: 트랜잭션 조정
- **JdbcTransaction**: JDBC 트랜잭션 구현
- **TransactionSynchronization**: 트랜잭션 동기화

### 8. Query (쿼리)
JPQL 및 쿼리 처리
- **JpqlParser**: JPQL 파싱
- **QueryTranslator**: JPQL → SQL 변환
- **Query**: 쿼리 실행 인터페이스
- **TypedQuery**: 타입 안전 쿼리

### 9. Proxy (프록시)
지연 로딩 구현 - JDK Dynamic Proxy 방식
- **ProxyFactory**: JDK Dynamic Proxy 기반 프록시 생성
- **LazyLoadingInvocationHandler**: InvocationHandler 구현체로 지연 로딩 처리
- **EntityInterface**: 엔티티가 구현해야 하는 인터페이스 규약

**프록시 방식**:
- 엔티티는 반드시 인터페이스를 구현해야 함
- `java.lang.reflect.Proxy`를 사용한 JDK Dynamic Proxy
- 외부 라이브러리 없이 순수 Java만 사용
- 메서드 호출 시점에 실제 엔티티 로딩

### 10. Dialect (DB 방언)
데이터베이스별 차이 처리
- **Dialect**: DB 방언 추상화
- **MySQLDialect**: MySQL 특화 처리
- **PostgreSQLDialect**: PostgreSQL 특화 처리

## 핵심 개념

### Entity Lifecycle (엔티티 생명주기)
```
NEW (Transient)
    ↓ persist()
MANAGED (Persistent) ←→ DETACHED
    ↓ remove()           ↑ detach()
REMOVED              ←───┘
```

### Persistence Context (영속성 컨텍스트)
```
PersistenceContext
├── identityMap: Map<EntityKey, Entity>     # 1차 캐시
├── entityStates: Map<Entity, EntityEntry>  # 엔티티 상태
├── actionQueue: ActionQueue                # 실행 대기 액션
│   ├── insertions: List<InsertAction>
│   ├── updates: List<UpdateAction>
│   └── deletions: List<DeleteAction>
└── loadedCollections: Set<Collection>      # 로드된 컬렉션
```

### Dirty Checking (변경 감지)
```
EntityEntry
├── entity: Object                 # 관리 중인 엔티티
├── loadedState: Object[]          # 로드 시점의 스냅샷
├── status: EntityStatus           # 상태 (MANAGED, DELETED 등)
└── isModified(): boolean          # 변경 여부 감지
```

### Lazy Loading with JDK Dynamic Proxy (지연 로딩)
```
// 1. 엔티티 인터페이스 정의
interface UserEntity {
    Long getId();
    String getName();
    void setName(String name);
}

// 2. 프록시 생성
UserEntity proxy = (UserEntity) Proxy.newProxyInstance(
    classLoader,
    new Class[]{UserEntity.class},
    new LazyLoadingInvocationHandler(userId, entityManager)
);

// 3. 메서드 호출 시 실제 로딩
String name = proxy.getName();  // 이 시점에 DB에서 로드
```

## 구현 단계

### Phase 1: 메타데이터 & 매핑 (최우선 - 모든 것의 기반)

#### Step 1.1: 애노테이션 정의
- [x] @Entity - 엔티티 클래스 표시
- [x] @Table - 테이블 이름 매핑
- [x] @Id - Primary Key 표시
- [x] @Column - 컬럼 매핑

#### Step 1.2: EntityMetadata 구조 설계
- [x] EntityMetadata 클래스
- [x] AttributeMetadata 클래스
- [x] IdentifierMetadata 클래스
- [x] 테이블-컬럼 매핑 정보 저장

#### Step 1.3: 리플렉션 기반 메타데이터 추출
- [x] AnnotationProcessor 구현
- [x] 클래스 스캔 및 애노테이션 읽기
- [x] 필드 정보 추출
- [x] 메타데이터 객체 생성

#### Step 1.4: MetadataRegistry (메타데이터 저장소)
- [x] 메타데이터 캐싱
- [x] 클래스 → 메타데이터 조회
- [x] 테스트 엔티티로 검증

### Phase 2: 기반 인프라
- [x] JDBC Connection 관리
- [x] Transaction 기본 구조
- [x] SQL 실행 엔진 (JdbcExecutor)
- [x] ResultSet 처리

### Phase 3: SQL 생성 엔진
- [x] INSERT SQL 생성 (메타데이터 활용)
- [x] SELECT SQL 생성
- [x] UPDATE SQL 생성
- [x] DELETE SQL 생성
- [x] WHERE 절 생성

### Phase 4: 핵심 API (최소 구현)
- [x] EntityManagerFactory 기본 구조
- [x] EntityManager 기본 구조 (CRUD 시그니처만)
- [x] EntityTransaction 기본 구조

**Phase 4 설계 결정사항**:
- **트랜잭션 정책**: 현재는 모든 DB 작업(조회 포함)에 트랜잭션 필수
- **향후 개선 계획**: Phase 6에서 조회 작업의 트랜잭션 선택적 지원 추가 가능

### Phase 5: 영속성 컨텍스트
- [x] PersistenceContext 구현
- [x] 1차 캐시 (IdentityMap)
- [x] Entity 상태 관리 (EntityEntry)
- [x] ActionQueue (쓰기 지연)

### Phase 6: CRUD 연산 (Persister)
- [ ] persist() - INSERT 실행
- [ ] find() - SELECT by ID 실행 (트랜잭션 선택적 지원 고려)
- [ ] merge() - UPDATE 실행
- [ ] remove() - DELETE 실행
- [ ] flush() - ActionQueue 실행
- [ ] Dirty Checking

**Phase 6 개선 TODO**:
- [ ] find() 메서드에서 트랜잭션 없이도 조회 가능하도록 개선 (선택사항)
  - 활성 트랜잭션 있으면 해당 Connection 사용
  - 없으면 임시 Connection 생성 (auto-commit 모드)

### Phase 7: 쿼리 처리 (기본)
- [ ] JPQL 파서 (간단한 SELECT만)
- [ ] JPQL → SQL 변환
- [ ] Query 인터페이스
- [ ] TypedQuery
- [ ] Parameter Binding

### Phase 8: 관계 매핑
- [ ] @OneToOne
- [ ] @ManyToOne
- [ ] @OneToMany
- [ ] @ManyToMany
- [ ] FetchType (EAGER/LAZY)
- [ ] Cascade

### Phase 9: 지연 로딩 (JDK Dynamic Proxy)
- [ ] 엔티티 인터페이스 규약 정의
- [ ] JDK Dynamic Proxy 기반 ProxyFactory
- [ ] LazyLoadingInvocationHandler 구현
- [ ] 프록시 초기화 로직

### Phase 10: 고급 기능
- [ ] 2차 캐시
- [ ] Batch Processing
- [ ] Optimistic Locking (@Version)
- [ ] DB 방언 (Dialect)

## 디렉토리 구조
```
src/main/java/
├── core/                           # 핵심 API
│   ├── EntityManagerFactory.java
│   ├── EntityManager.java
│   └── EntityTransaction.java
│
├── metadata/                       # 메타데이터
│   ├── EntityMetadata.java
│   ├── AttributeMetadata.java
│   ├── IdentifierMetadata.java
│   └── MetadataRegistry.java
│
├── mapping/                        # 매핑 처리
│   ├── AnnotationProcessor.java
│   ├── TableMapper.java
│   ├── ColumnMapper.java
│   └── RelationshipMapper.java
│
├── persister/                      # 영속화 실행
│   ├── EntityPersister.java
│   ├── EntityLoader.java
│   ├── EntityUpdater.java
│   └── EntityDeleter.java
│
├── engine/                         # SQL 엔진
│   ├── SqlGenerator.java
│   ├── JdbcExecutor.java
│   ├── ResultSetExtractor.java
│   └── ParameterBinder.java
│
├── cache/                          # 캐시
│   ├── PersistenceContext.java
│   ├── IdentityMap.java
│   ├── EntityEntry.java
│   └── ActionQueue.java
│
├── transaction/                    # 트랜잭션
│   ├── TransactionCoordinator.java
│   ├── JdbcTransaction.java
│   └── TransactionSynchronization.java
│
├── query/                          # 쿼리
│   ├── JpqlParser.java
│   ├── QueryTranslator.java
│   ├── Query.java
│   └── TypedQuery.java
│
├── proxy/                          # 프록시 (JDK Dynamic Proxy)
│   ├── ProxyFactory.java
│   ├── LazyLoadingInvocationHandler.java
│   └── EntityInterfaceValidator.java
│
└── dialect/                        # DB 방언
    ├── Dialect.java
    ├── MySQLDialect.java
    └── PostgreSQLDialect.java
```

## 핵심 패턴

### 아키텍처 패턴
- **Unit of Work**: 트랜잭션 내 변경사항 추적
- **Identity Map**: 동일 엔티티 중복 방지
- **Lazy Load**: 프록시를 통한 지연 로딩
- **Data Mapper**: 객체-DB 매핑 분리

### 디자인 패턴
- **Factory Pattern**: EntityManagerFactory
- **Proxy Pattern**: 지연 로딩
- **Strategy Pattern**: Dialect (DB 방언)
- **Command Pattern**: ActionQueue
- **Template Method**: Persister 계층

## 학습 목표
1. JPA/Hibernate의 내부 동작 원리 완전 이해
2. 영속성 컨텍스트와 1차 캐시 메커니즘
3. Dirty Checking과 변경 감지 알고리즘
4. 프록시와 지연 로딩 구현 방법
5. JPQL 파싱과 SQL 생성 로직
6. 트랜잭션과 Connection 관리
7. 리플렉션과 메타프로그래밍
8. 객체-관계 매핑 전략

## 기술 스택
- Java 17+
- JDBC API
- Reflection API
- JDK Dynamic Proxy (java.lang.reflect.Proxy)
- 애노테이션 처리

## 참고 자료
- JPA 2.2 Specification
- Hibernate ORM Documentation
- "Java Persistence with Hibernate" - Christian Bauer
- "Pro JPA 2" - Mike Keith, Merrick Schincariol
