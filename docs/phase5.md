# Phase 5: 영속성 컨텍스트 구현 (Persistence Context)

## 개요

**왜 이 단계?** Phase 4에서 구축한 EntityManager는 아직 껍데기에 불과합니다. 
진짜 JPA의 핵심인 **영속성 컨텍스트 (Persistence Context)**를 구현하여 1차 캐시, 엔티티 상태 관리, 쓰기 지연(Write-Behind) 등 JPA의 가장 중요한 메커니즘을 구축합니다.

영속성 컨텍스트는 "엔티티를 영구 저장하는 환경"으로, 애플리케이션과 데이터베이스 사이에서 객체를 관리하는 일종의 캐시 계층입니다.

---

## 핵심 개념

### 영속성 컨텍스트란?
- **1차 캐시**: 같은 트랜잭션 내에서 동일한 엔티티는 한 번만 조회
- **동일성 보장**: 같은 ID의 엔티티는 항상 같은 객체 인스턴스 (== 비교 가능)
- **쓰기 지연**: INSERT/UPDATE/DELETE를 모아서 flush() 시점에 한 번에 실행
- **변경 감지 (Dirty Checking)**: 엔티티 변경을 자동으로 감지하여 UPDATE
- **지연 로딩**: 연관된 엔티티는 실제 사용 시점에 로딩 (Phase 9)

### 엔티티 생명주기 (Entity Lifecycle)
```
NEW (Transient)          - 영속성 컨텍스트와 무관한 새로운 객체
    ↓ persist()
MANAGED (Persistent)     - 영속성 컨텍스트가 관리하는 영속 상태
    ↓ remove()
REMOVED                  - 삭제 예정 상태
    ↓ commit()
DELETED                  - DB에서 삭제됨

MANAGED ←→ DETACHED      - 준영속 상태 (detach(), clear(), close())
```

---

## 구현 단계

### Step 5.1: EntityKey (엔티티 식별자)

- [x] EntityKey 클래스 구현
- [x] 엔티티 클래스 + ID로 고유 식별
- [x] equals(), hashCode() 구현
- [x] 1차 캐시의 Map 키로 사용

**주요 컴포넌트**:
- `EntityKey`: 엔티티 타입 + ID를 조합한 고유 키

**핵심 기능**:
- 엔티티 클래스와 ID를 조합하여 고유한 키 생성
- IdentityMap의 키로 사용
- Thread-safe하고 불변(immutable)

**예시**:
```java
// EntityKey 생성
EntityKey key = new EntityKey(User.class, 1L);

// Map에 저장
Map<EntityKey, Object> identityMap = new HashMap<>();
identityMap.put(key, userEntity);
```

**설계**:
```java
public class EntityKey {
    private final Class<?> entityClass;
    private final Object id;

    public EntityKey(Class<?> entityClass, Object id) {
        this.entityClass = entityClass;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityKey)) return false;
        EntityKey that = (EntityKey) o;
        return Objects.equals(entityClass, that.entityClass)
            && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass, id);
    }
}
```

---

### Step 5.2: EntityStatus (엔티티 상태)

- [x] EntityStatus Enum 정의
- [x] 엔티티 생명주기 상태 표현
- [x] 상태 전이 규칙 정의

**주요 컴포넌트**:
- `EntityStatus`: 엔티티의 생명주기 상태

**핵심 상태**:
- `MANAGED`: 영속 상태 (영속성 컨텍스트가 관리)
- `REMOVED`: 삭제 예정 상태
- `DETACHED`: 준영속 상태 (더 이상 관리하지 않음)

**예시**:
```java
public enum EntityStatus {
    MANAGED,    // 영속 상태
    REMOVED,    // 삭제 예정
    DETACHED    // 준영속 상태
}
```

---

### Step 5.3: EntityEntry (엔티티 상태 추적)

- [x] EntityEntry 클래스 구현
- [x] 엔티티별 상태 정보 저장
- [x] 로드 시점의 스냅샷 저장 (Dirty Checking용)
- [x] 상태 변경 메서드

**주요 컴포넌트**:
- `EntityEntry`: 엔티티의 상태 정보와 스냅샷

**핵심 기능**:
- 엔티티 상태 (MANAGED, REMOVED, DETACHED) 관리
- 로드 시점의 필드 값 스냅샷 보관
- 변경 감지를 위한 스냅샷 비교

**예시**:
```java
public class EntityEntry {
    private final Object entity;              // 관리 중인 엔티티
    private final Object[] loadedState;       // 로드 시점의 스냅샷
    private EntityStatus status;              // 현재 상태

    public EntityEntry(Object entity, Object[] loadedState) {
        this.entity = entity;
        this.loadedState = loadedState;
        this.status = EntityStatus.MANAGED;
    }

    public boolean isModified(EntityMetadata metadata) {
        // 현재 엔티티의 필드 값과 스냅샷 비교
        Object[] currentState = extractCurrentState(entity, metadata);
        return !Arrays.equals(loadedState, currentState);
    }

    public void markAsRemoved() {
        this.status = EntityStatus.REMOVED;
    }
}
```

**Dirty Checking 메커니즘**:
```
1. 엔티티 로드 시 → 필드 값들을 Object[]로 스냅샷 저장
2. flush() 호출 시 → 현재 필드 값과 스냅샷 비교
3. 변경 감지되면 → UPDATE 쿼리 생성 및 실행
```

---

### Step 5.4: ActionQueue (쓰기 지연)

- [x] ActionQueue 클래스 구현
- [x] INSERT/UPDATE/DELETE 액션 대기열
- [x] 실행 순서 보장 (INSERT → UPDATE → DELETE)
- [x] execute() 메서드로 일괄 실행

**주요 컴포넌트**:
- `ActionQueue`: 쓰기 작업 대기열
- `EntityAction`: 개별 작업 인터페이스

**핵심 기능**:
- persist() 호출 시 → INSERT 액션을 큐에 추가
- remove() 호출 시 → DELETE 액션을 큐에 추가
- Dirty Checking 시 → UPDATE 액션을 큐에 추가
- flush() 호출 시 → 큐의 모든 액션을 순서대로 실행

**예시**:
```java
public class ActionQueue {
    private final List<EntityAction> insertions = new ArrayList<>();
    private final List<EntityAction> updates = new ArrayList<>();
    private final List<EntityAction> deletions = new ArrayList<>();

    public void addInsertAction(Object entity) {
        insertions.add(new InsertAction(entity));
    }

    public void addUpdateAction(Object entity) {
        updates.add(new UpdateAction(entity));
    }

    public void addDeleteAction(Object entity) {
        deletions.add(new DeleteAction(entity));
    }

    public void executeActions(JdbcExecutor executor, MetadataRegistry registry) {
        // 실행 순서: INSERT → UPDATE → DELETE
        executeList(insertions, executor, registry);
        executeList(updates, executor, registry);
        executeList(deletions, executor, registry);
        clear();
    }

    public void clear() {
        insertions.clear();
        updates.clear();
        deletions.clear();
    }
}
```

**EntityAction 인터페이스**:
```java
public interface EntityAction {
    void execute(JdbcExecutor executor, MetadataRegistry registry);
    Object getEntity();
}

public class InsertAction implements EntityAction {
    private final Object entity;

    @Override
    public void execute(JdbcExecutor executor, MetadataRegistry registry) {
        // INSERT SQL 생성 및 실행
        EntityMetadata metadata = registry.getMetadata(entity.getClass());
        SqlWithParameters sql = InsertSqlGenerator.generate(entity, metadata);
        executor.executeUpdate(sql);
    }
}
```

---

### Step 5.5: PersistenceContext (영속성 컨텍스트)

- [x] PersistenceContext 클래스 구현
- [x] IdentityMap (1차 캐시) 관리
- [x] EntityEntry 맵 관리
- [x] ActionQueue 관리
- [x] 엔티티 추가/조회/삭제 메서드

**주요 컴포넌트**:
- `PersistenceContext`: 영속성 컨텍스트 핵심 구현체

**핵심 기능**:
- `addEntity()`: 엔티티를 영속성 컨텍스트에 추가
- `getEntity()`: 1차 캐시에서 엔티티 조회
- `removeEntity()`: 엔티티를 삭제 예정 상태로 마킹
- `clear()`: 영속성 컨텍스트 초기화
- `contains()`: 엔티티가 관리 중인지 확인

**예시**:
```java
public class PersistenceContext {
    // 1차 캐시: EntityKey → Entity
    private final Map<EntityKey, Object> identityMap = new HashMap<>();

    // 엔티티 상태 추적: Entity → EntityEntry
    private final Map<Object, EntityEntry> entityEntries = new IdentityHashMap<>();

    // 쓰기 지연 큐
    private final ActionQueue actionQueue = new ActionQueue();

    // 엔티티를 영속성 컨텍스트에 추가
    public void addEntity(Object entity, EntityMetadata metadata) {
        Object id = metadata.getIdentifier().getValue(entity);
        EntityKey key = new EntityKey(entity.getClass(), id);

        // 1차 캐시에 저장
        identityMap.put(key, entity);

        // 현재 상태를 스냅샷으로 저장
        Object[] snapshot = extractState(entity, metadata);
        EntityEntry entry = new EntityEntry(entity, snapshot);
        entityEntries.put(entity, entry);
    }

    // 1차 캐시에서 조회
    public <T> T getEntity(Class<T> entityClass, Object id) {
        EntityKey key = new EntityKey(entityClass, id);
        return (T) identityMap.get(key);
    }

    // 엔티티 삭제 (REMOVED 상태로 마킹)
    public void removeEntity(Object entity) {
        EntityEntry entry = entityEntries.get(entity);
        if (entry != null) {
            entry.markAsRemoved();
            actionQueue.addDeleteAction(entity);
        }
    }

    // 변경 감지 (Dirty Checking)
    public void detectDirtyEntities(MetadataRegistry registry) {
        for (Map.Entry<Object, EntityEntry> entry : entityEntries.entrySet()) {
            Object entity = entry.getKey();
            EntityEntry entityEntry = entry.getValue();

            if (entityEntry.getStatus() == EntityStatus.MANAGED) {
                EntityMetadata metadata = registry.getMetadata(entity.getClass());
                if (entityEntry.isModified(metadata)) {
                    actionQueue.addUpdateAction(entity);
                }
            }
        }
    }

    // flush: ActionQueue 실행
    public void flush(JdbcExecutor executor, MetadataRegistry registry) {
        // Dirty Checking
        detectDirtyEntities(registry);

        // 모든 액션 실행
        actionQueue.executeActions(executor, registry);
    }

    // 영속성 컨텍스트 초기화
    public void clear() {
        identityMap.clear();
        entityEntries.clear();
        actionQueue.clear();
    }

    // 엔티티가 관리 중인지 확인
    public boolean contains(Object entity) {
        return entityEntries.containsKey(entity);
    }
}
```

---

### Step 5.6: EntityManager와 통합

- [x] EntityManagerImpl에 PersistenceContext 추가
- [x] persist() 메서드와 연결
- [x] find() 메서드와 1차 캐시 연결
- [x] remove() 메서드와 연결
- [x] flush() 메서드 구현
- [x] clear() 메서드 구현

**주요 변경사항**:
- EntityManagerImpl이 PersistenceContext를 보유
- 모든 CRUD 작업이 PersistenceContext를 통해 수행

**예시**:
```java
public class EntityManagerImpl implements EntityManager {
    private final EntityManagerFactory factory;
    private final PersistenceContext persistenceContext;
    private final EntityTransaction transaction;
    private boolean open = true;

    public EntityManagerImpl(EntityManagerFactory factory) {
        this.factory = factory;
        this.persistenceContext = new PersistenceContext();  // 🆕
        this.transaction = new EntityTransactionImpl(this);
    }

    @Override
    public void persist(Object entity) {
        checkOpen();
        // ActionQueue에 INSERT 액션 추가
        persistenceContext.addInsertAction(entity, getMetadataRegistry());
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        checkOpen();

        // 1. 1차 캐시 확인
        T cached = persistenceContext.getEntity(entityClass, primaryKey);
        if (cached != null) {
            return cached;
        }

        // 2. DB에서 조회 (Phase 6에서 구현)
        // T entity = loadFromDatabase(entityClass, primaryKey);

        // 3. 영속성 컨텍스트에 추가
        // persistenceContext.addEntity(entity, metadata);

        return null;  // Phase 6에서 완성
    }

    @Override
    public void remove(Object entity) {
        checkOpen();
        // REMOVED 상태로 마킹 + DELETE 액션 추가
        persistenceContext.removeEntity(entity);
    }

    @Override
    public void flush() {
        checkOpen();
        requireActiveTransaction();

        // Dirty Checking + ActionQueue 실행
        Connection conn = transaction.getConnection();
        JdbcExecutor executor = new JdbcExecutor(conn);
        persistenceContext.flush(executor, getMetadataRegistry());
    }

    @Override
    public void clear() {
        checkOpen();
        // 영속성 컨텍스트 초기화
        persistenceContext.clear();
    }

    @Override
    public boolean contains(Object entity) {
        checkOpen();
        return persistenceContext.contains(entity);
    }
}
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
│   │               ├── cache/                          # 🆕 Phase 5
│   │               │   ├── PersistenceContext.java    # 영속성 컨텍스트
│   │               │   ├── EntityKey.java             # 엔티티 식별자
│   │               │   ├── EntityEntry.java           # 엔티티 상태 추적
│   │               │   ├── EntityStatus.java          # 엔티티 생명주기 상태
│   │               │   ├── ActionQueue.java           # 쓰기 지연 큐
│   │               │   └── action/
│   │               │       ├── EntityAction.java      # 액션 인터페이스
│   │               │       ├── InsertAction.java      # INSERT 액션
│   │               │       ├── UpdateAction.java      # UPDATE 액션
│   │               │       └── DeleteAction.java      # DELETE 액션
│   │               ├── core/
│   │               │   ├── EntityManagerFactory.java
│   │               │   ├── EntityManagerFactoryImpl.java
│   │               │   ├── EntityManager.java
│   │               │   ├── EntityManagerImpl.java     # 🔄 PersistenceContext 통합
│   │               │   ├── EntityTransaction.java
│   │               │   ├── EntityTransactionImpl.java
│   │               │   ├── Persistence.java
│   │               │   └── PersistenceConfiguration.java
│   │               ├── engine/
│   │               ├── metadata/
│   │               └── transaction/
│   └── test/
│       └── java/
│           └── io/
│               └── simplejpa/
│                   ├── cache/                         # 🆕 Cache 테스트
│                   │   ├── PersistenceContextTest.java
│                   │   ├── EntityKeyTest.java
│                   │   ├── EntityEntryTest.java
│                   │   └── ActionQueueTest.java
│                   └── integration/
│                       └── PersistenceContextIntegrationTest.java
```

---

## 구현 순서 요약

1. **EntityKey** → 1차 캐시의 키
2. **EntityStatus** → 엔티티 생명주기 상태
3. **EntityEntry** → 엔티티 상태 추적 + Dirty Checking
4. **ActionQueue** → 쓰기 지연 구현
5. **PersistenceContext** → 모든 것을 통합
6. **EntityManager 통합** → 실제 사용

---

## 핵심 메커니즘

### 1차 캐시 (Identity Map)
```
┌─────────────────────────────────┐
│      PersistenceContext         │
│                                 │
│  identityMap:                   │
│    EntityKey(User, 1) → user1   │
│    EntityKey(User, 2) → user2   │
│    EntityKey(Order, 5) → order5 │
└─────────────────────────────────┘

em.find(User.class, 1L)  // DB 조회
em.find(User.class, 1L)  // 캐시에서 반환 (DB 조회 안 함!)
```

### 쓰기 지연 (Write-Behind)
```
tx.begin();
em.persist(user1);        // INSERT 액션 큐에 추가만
em.persist(user2);        // INSERT 액션 큐에 추가만
user1.setName("updated"); // 아직 아무 일도 안 일어남
tx.commit();              // 이 시점에 flush() → SQL 실행!
```

### Dirty Checking (변경 감지)
```
tx.begin();
User user = em.find(User.class, 1L);  // 스냅샷 저장
user.setName("updated");               // 엔티티만 변경
tx.commit();                           // flush() → 스냅샷 비교 → UPDATE 자동 실행!
```

---

## 핵심 설계 원칙

### 1. 동일성 보장 (Identity)
- 같은 트랜잭션 내에서 같은 ID의 엔티티는 항상 같은 인스턴스
- `user1 == user2` 비교 가능 (equals()가 아니라 ==)

### 2. 쓰기 지연 (Write-Behind)
- SQL 실행을 최대한 지연시켜 성능 최적화
- flush() 시점에 일괄 실행 → 배치 가능
- 트랜잭션 커밋 직전에 flush() 자동 호출

### 3. 변경 감지 (Dirty Checking)
- 개발자가 명시적으로 update() 호출 불필요
- 엔티티만 변경하면 자동으로 UPDATE
- 스냅샷 비교 방식으로 구현

### 4. 1차 캐시
- 같은 엔티티 중복 조회 방지
- 트랜잭션 범위의 짧은 캐시
- 성능보다는 정합성이 목적

---

## Phase 5 완료 후 가능한 것

- 1차 캐시를 통한 엔티티 중복 조회 방지
- 동일성 보장 (== 비교 가능)
- 쓰기 지연을 통한 SQL 일괄 실행
- 자동 변경 감지 (Dirty Checking)
- 영속성 컨텍스트 수동 제어 (flush, clear, contains)

**아직 안 되는 것**:
- 실제 DB 조회/저장 (Phase 6에서 구현)
- JPQL 쿼리 (Phase 7)
- 관계 매핑 (Phase 8)
- 지연 로딩 (Phase 9)

---

## 다음 단계 (Phase 6 예고)

Phase 6에서는 **CRUD 연산 (Persister)**을 구현하여 실제로 데이터베이스와 통신합니다:
- `persist()` - INSERT 실행
- `find()` - SELECT by ID 실행
- `merge()` - UPDATE 실행
- `remove()` - DELETE 실행
- Phase 3의 SQL Generator와 Phase 5의 PersistenceContext를 통합

---

## 테스트 전략

### 단위 테스트
- **EntityKeyTest**: equals/hashCode 검증
- **EntityEntryTest**: 스냅샷 생성, 변경 감지 테스트
- **ActionQueueTest**: 액션 추가/실행 순서 검증
- **PersistenceContextTest**: 캐시 동작, 상태 관리 검증

### 통합 테스트
- **PersistenceContextIntegrationTest**:
  - 1차 캐시 동작 검증
  - 쓰기 지연 시나리오
  - Dirty Checking 시나리오
  - flush/clear 동작 검증

---

## 예제: 전체 플로우

```java
// EntityManagerFactory 생성
EntityManagerFactory emf = Persistence.createEntityManagerFactory(config);
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

try {
    tx.begin();

    // === 1차 캐시 테스트 ===
    User user1 = em.find(User.class, 1L);  // DB 조회
    User user2 = em.find(User.class, 1L);  // 캐시에서 반환
    assert user1 == user2;                 // 동일성 보장!

    // === 쓰기 지연 테스트 ===
    User newUser = new User("John", "john@email.com");
    em.persist(newUser);  // 아직 INSERT 안 함, 큐에만 추가

    // === Dirty Checking 테스트 ===
    user1.setName("Updated Name");  // 엔티티만 변경
    // em.update(user1) 같은 메서드 호출 불필요!

    // === flush: 모든 SQL 실행 ===
    em.flush();
    // INSERT INTO users ... (newUser)
    // UPDATE users SET name = ? WHERE id = ? (user1)

    tx.commit();
} catch (Exception e) {
    if (tx.isActive()) {
        tx.rollback();
    }
} finally {
    em.close();
}
```

---

## Phase 5 핵심 체크리스트

### EntityKey
- [ ] 엔티티 클래스 + ID 조합
- [ ] equals/hashCode 구현
- [ ] Immutable 보장

### EntityStatus
- [ ] MANAGED, REMOVED, DETACHED 상태 정의
- [ ] 상태 전이 규칙 문서화

### EntityEntry
- [ ] 엔티티 + 스냅샷 + 상태 보관
- [ ] 변경 감지 로직 (isModified)
- [ ] 상태 변경 메서드

### ActionQueue
- [ ] INSERT/UPDATE/DELETE 리스트
- [ ] 실행 순서 보장
- [ ] executeActions() 구현
- [ ] clear() 구현

### PersistenceContext
- [ ] IdentityMap (1차 캐시)
- [ ] EntityEntry 맵
- [ ] ActionQueue
- [ ] addEntity, getEntity, removeEntity
- [ ] detectDirtyEntities (Dirty Checking)
- [ ] flush, clear, contains

### EntityManager 통합
- [ ] PersistenceContext 필드 추가
- [ ] persist() 연결
- [ ] find() 1차 캐시 연결
- [ ] remove() 연결
- [ ] flush() 구현
- [ ] clear() 구현
- [ ] contains() 구현

---

이제 Phase 5를 시작할 준비가 되었습니다! 🚀

영속성 컨텍스트는 JPA의 핵심이자 영혼입니다. 이 단계를 완료하면 JPA가 어떻게 엔티티를 관리하고, 왜 그렇게 동작하는지 완전히 이해할 수 있습니다.