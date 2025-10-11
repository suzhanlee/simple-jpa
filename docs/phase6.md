# Phase 6: CRUD 연산 구현 (Persister Layer)

## 개요

**왜 이 단계?** Phase 5에서 영속성 컨텍스트를 구축했지만, 아직 실제 데이터베이스와 통신하지 않습니다.
Phase 6에서는 **Persister 계층**을 구현하여 실제 CRUD 작업을 수행하고, Phase 3의 SQL Generator와 Phase 5의 PersistenceContext를 완전히 통합합니다.

이 단계를 완료하면 드디어 엔티티를 데이터베이스에 저장하고 조회할 수 있는 완전한 JPA 구현체가 됩니다!

---

## 핵심 개념

### Persister란?

- **엔티티와 데이터베이스 사이의 브릿지**: 객체를 SQL로, SQL 결과를 객체로 변환
- **영속성 컨텍스트와 긴밀히 협력**: 1차 캐시, Dirty Checking, ActionQueue와 통합
- **책임 분리**: 각 작업(INSERT, SELECT, UPDATE, DELETE)을 전문 클래스로 분리

### 주요 Persister 클래스

- **EntityPersister**: INSERT 작업 수행 (persist)
- **EntityLoader**: SELECT 작업 수행 (find)
- **EntityUpdater**: UPDATE 작업 수행 (merge, Dirty Checking)
- **EntityDeleter**: DELETE 작업 수행 (remove)

### 핵심 플로우

```
EntityManager.persist(entity)
    ↓
PersistenceContext.addInsertAction(entity)
    ↓
ActionQueue에 InsertAction 추가
    ↓
flush() 호출 시
    ↓
EntityPersister.insert(entity)
    ↓
SQL Generator → JDBC Executor → DB
```

---

## 구현 단계

### Step 6.1: EntityLoader (SELECT 구현)

- [x] EntityLoader 클래스 구현
- [x] ID로 엔티티 조회
- [x] ResultSet → 엔티티 객체 변환
- [x] 1차 캐시 통합
- [x] 트랜잭션 선택적 지원 (개선)

**주요 컴포넌트**:

- `EntityLoader`: 엔티티 로딩 전문 클래스

**핵심 기능**:

- SELECT SQL 생성 (Phase 3의 SelectSqlGenerator 활용)
- JDBC로 쿼리 실행
- ResultSet을 엔티티 객체로 변환
- 로드된 엔티티를 영속성 컨텍스트에 추가 (스냅샷 포함)

**설계**:

```java
public class EntityLoader {
    private final MetadataRegistry metadataRegistry;
    private final SelectSqlGenerator selectSqlGenerator;
    private final JdbcExecutor jdbcExecutor;

    public EntityLoader(
            MetadataRegistry metadataRegistry,
            SelectSqlGenerator selectSqlGenerator,
            JdbcExecutor jdbcExecutor
    ) {
        this.metadataRegistry = metadataRegistry;
        this.selectSqlGenerator = selectSqlGenerator;
        this.jdbcExecutor = jdbcExecutor;
    }

    public <T> T load(Connection connection, Class<T> entityClass, Object id) {
        // 1. 메타데이터 조회
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);

        // 2. SELECT SQL 생성
        SqlWithParameters sql = selectSqlGenerator.generateFindById(metadata, id);

        // 3. SQL 실행 (Connection은 파라미터로 전달)
        return jdbcExecutor.executeQuery(
            connection,
            sql.sql(),
            new EntityResultSetExtractor<>(metadata, new TypeConverter()),
            sql.parameters()
        );
    }
}
```

**EntityManager.find() 통합**:

```java
@Override
public <T> T find(Class<T> entityClass, Object primaryKey) {
    checkOpen();
    requireActiveTransaction();

    // 1. 1차 캐시 확인
    T cached = persistenceContext.getEntity(entityClass, primaryKey);
    if (cached != null) {
        return cached;
    }

    // 2. DB에서 로드 (트랜잭션의 Connection 전달)
    T entity = entityLoader.load(
        transaction.getConnection(),
        entityClass,
        primaryKey
    );

    if (entity != null) {
        // 3. 영속성 컨텍스트에 추가 (스냅샷 자동 생성)
        persistenceContext.addEntity(entity);
    }

    return entity;
}
```

**트랜잭션 선택적 지원 (개선안)**:

```java

@Override
public <T> T find(Class<T> entityClass, Object primaryKey) {
    checkOpen();

    // 1. 1차 캐시 확인 (트랜잭션 여부 무관)
    T cached = persistenceContext.getEntity(entityClass, primaryKey);
    if (cached != null) {
        return cached;
    }

    // 2. Connection 확보
    Connection conn;
    boolean shouldClose = false;

    if (transaction.isActive()) {
        // 활성 트랜잭션이 있으면 해당 Connection 사용
        conn = transaction.getConnection();
    } else {
        // 없으면 임시 Connection 생성 (auto-commit)
        conn = factory.getDataSource().getConnection();
        conn.setAutoCommit(true);
        shouldClose = true;
    }

    try {
        // 3. DB에서 로드
        JdbcExecutor executor = new JdbcExecutor(conn);
        EntityLoader loader = new EntityLoader(executor, metadataRegistry);
        T entity = loader.load(entityClass, primaryKey);

        if (entity != null && transaction.isActive()) {
            // 트랜잭션 내에서만 영속성 컨텍스트에 추가
            EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
            persistenceContext.addEntity(entity, metadata);
        }

        return entity;
    } finally {
        if (shouldClose) {
            conn.close();
        }
    }
}
```

---

### Step 6.2: EntityPersister (INSERT 구현)

- [ ] EntityPersister 클래스 구현
- [ ] 엔티티를 INSERT SQL로 변환
- [ ] 생성된 ID 자동 설정 (Auto-increment 지원)
- [ ] InsertAction과 통합

**주요 컴포넌트**:

- `EntityPersister`: 엔티티 삽입 전문 클래스

**핵심 기능**:

- INSERT SQL 생성 (Phase 3의 InsertSqlGenerator 활용)
- JDBC로 INSERT 실행
- 생성된 ID (Auto-increment) 엔티티에 자동 설정
- 영속성 컨텍스트에 엔티티 추가

**설계**:

```java
public class EntityPersister {
    private final JdbcExecutor jdbcExecutor;
    private final MetadataRegistry metadataRegistry;

    public void insert(Object entity) {
        // 1. 메타데이터 조회
        EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());

        // 2. INSERT SQL 생성
        SqlWithParameters sql = InsertSqlGenerator.generate(entity, metadata);

        // 3. SQL 실행 및 생성된 ID 반환
        Long generatedId = jdbcExecutor.executeInsert(sql);

        // 4. 생성된 ID를 엔티티에 설정 (Auto-increment인 경우)
        if (generatedId != null && metadata.getIdentifier().isAutoGenerated()) {
            metadata.getIdentifier().setValue(entity, generatedId);
        }
    }
}
```

**JdbcExecutor.executeInsert() 구현**:

```java
public Long executeInsert(SqlWithParameters sqlWithParams) throws SQLException {
    String sql = sqlWithParams.getSql();
    List<Object> params = sqlWithParams.getParameters();

    // Statement.RETURN_GENERATED_KEYS 사용
    try (PreparedStatement pstmt = connection.prepareStatement(
            sql, Statement.RETURN_GENERATED_KEYS)) {

        // 파라미터 바인딩
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }

        // 실행
        pstmt.executeUpdate();

        // 생성된 ID 조회
        try (ResultSet rs = pstmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
    }

    return null;
}
```

**InsertAction 구현**:

```java
public class InsertAction implements EntityAction {
    private final Object entity;

    public InsertAction(Object entity) {
        this.entity = entity;
    }

    @Override
    public void execute(JdbcExecutor executor, MetadataRegistry registry) {
        EntityPersister persister = new EntityPersister(executor, registry);
        persister.insert(entity);
    }

    @Override
    public Object getEntity() {
        return entity;
    }
}
```

**EntityManager.persist() 구현**:

```java

@Override
public void persist(Object entity) {
    checkOpen();
    requireActiveTransaction();

    // 엔티티가 이미 관리 중이면 예외
    if (persistenceContext.contains(entity)) {
        throw new IllegalArgumentException("Entity is already managed");
    }

    // ActionQueue에 INSERT 액션 추가
    EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());
    persistenceContext.addInsertAction(entity, metadata);
}
```

**PersistenceContext에 addInsertAction 추가**:

```java
public void addInsertAction(Object entity, EntityMetadata metadata) {
    // INSERT 액션을 큐에 추가
    actionQueue.addInsertAction(entity);

    // 엔티티를 영속 상태로 관리 (상태: MANAGED)
    Object[] snapshot = extractState(entity, metadata);
    EntityEntry entry = new EntityEntry(entity, snapshot);
    entityEntries.put(entity, entry);
}
```

---

### Step 6.3: EntityUpdater (UPDATE 구현)

- [ ] EntityUpdater 클래스 구현
- [ ] 변경된 필드만 UPDATE
- [ ] Dirty Checking과 통합
- [ ] UpdateAction과 통합

**주요 컴포넌트**:

- `EntityUpdater`: 엔티티 업데이트 전문 클래스

**핵심 기능**:

- UPDATE SQL 생성 (Phase 3의 UpdateSqlGenerator 활용)
- 변경된 필드만 UPDATE (성능 최적화)
- JDBC로 UPDATE 실행
- 스냅샷 갱신

**설계**:

```java
public class EntityUpdater {
    private final JdbcExecutor jdbcExecutor;
    private final MetadataRegistry metadataRegistry;

    public void update(Object entity, EntityEntry entityEntry) {
        // 1. 메타데이터 조회
        EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());

        // 2. 변경된 필드 감지
        Object[] currentState = extractCurrentState(entity, metadata);
        Object[] loadedState = entityEntry.getLoadedState();
        Map<String, Object> changedFields = detectChanges(
                metadata, currentState, loadedState);

        if (changedFields.isEmpty()) {
            return; // 변경사항 없음
        }

        // 3. UPDATE SQL 생성
        SqlWithParameters sql = UpdateSqlGenerator.generate(
                entity, metadata, changedFields);

        // 4. SQL 실행
        jdbcExecutor.executeUpdate(sql);

        // 5. 스냅샷 갱신
        entityEntry.updateSnapshot(currentState);
    }

    private Map<String, Object> detectChanges(EntityMetadata metadata,
                                              Object[] currentState,
                                              Object[] loadedState) {
        Map<String, Object> changes = new HashMap<>();
        List<AttributeMetadata> attributes = metadata.getAttributes();

        for (int i = 0; i < attributes.size(); i++) {
            Object current = currentState[i];
            Object loaded = loadedState[i];

            if (!Objects.equals(current, loaded)) {
                AttributeMetadata attr = attributes.get(i);
                changes.put(attr.getColumnName(), current);
            }
        }

        return changes;
    }
}
```

**UpdateAction 구현**:

```java
public class UpdateAction implements EntityAction {
    private final Object entity;
    private final EntityEntry entityEntry;

    public UpdateAction(Object entity, EntityEntry entityEntry) {
        this.entity = entity;
        this.entityEntry = entityEntry;
    }

    @Override
    public void execute(JdbcExecutor executor, MetadataRegistry registry) {
        EntityUpdater updater = new EntityUpdater(executor, registry);
        updater.update(entity, entityEntry);
    }

    @Override
    public Object getEntity() {
        return entity;
    }
}
```

**PersistenceContext의 Dirty Checking 개선**:

```java
public void detectDirtyEntities(MetadataRegistry registry) {
    for (Map.Entry<Object, EntityEntry> entry : entityEntries.entrySet()) {
        Object entity = entry.getKey();
        EntityEntry entityEntry = entry.getValue();

        // MANAGED 상태인 엔티티만 체크
        if (entityEntry.getStatus() == EntityStatus.MANAGED) {
            EntityMetadata metadata = registry.getMetadata(entity.getClass());
            if (entityEntry.isModified(metadata)) {
                // UpdateAction에 EntityEntry도 함께 전달
                actionQueue.addUpdateAction(entity, entityEntry);
            }
        }
    }
}
```

**EntityManager.merge() 구현**:

```java

@Override
public <T> T merge(T entity) {
    checkOpen();
    requireActiveTransaction();

    EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());
    Object id = metadata.getIdentifier().getValue(entity);

    if (id == null) {
        throw new IllegalArgumentException("Entity must have an ID");
    }

    // 1. 영속성 컨텍스트에서 찾기
    T managedEntity = (T) persistenceContext.getEntity(entity.getClass(), id);

    if (managedEntity == null) {
        // 2. DB에서 로드
        managedEntity = find((Class<T>) entity.getClass(), id);

        if (managedEntity == null) {
            throw new IllegalArgumentException("Entity not found: " + id);
        }
    }

    // 3. 필드 복사 (detached → managed)
    copyFields(entity, managedEntity, metadata);

    // 4. Dirty Checking이 자동으로 UPDATE 처리

    return managedEntity;
}

private void copyFields(Object source, Object target, EntityMetadata metadata) {
    for (AttributeMetadata attr : metadata.getAttributes()) {
        Object value = attr.getValue(source);
        attr.setValue(target, value);
    }
}
```

---

### Step 6.4: EntityDeleter (DELETE 구현)

- [ ] EntityDeleter 클래스 구현
- [ ] DELETE SQL 실행
- [ ] 영속성 컨텍스트에서 제거
- [ ] DeleteAction과 통합

**주요 컴포넌트**:

- `EntityDeleter`: 엔티티 삭제 전문 클래스

**핵심 기능**:

- DELETE SQL 생성 (Phase 3의 DeleteSqlGenerator 활용)
- JDBC로 DELETE 실행
- 영속성 컨텍스트에서 엔티티 제거

**설계**:

```java
public class EntityDeleter {
    private final JdbcExecutor jdbcExecutor;
    private final MetadataRegistry metadataRegistry;

    public void delete(Object entity) {
        // 1. 메타데이터 조회
        EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());

        // 2. DELETE SQL 생성
        SqlWithParameters sql = DeleteSqlGenerator.generate(entity, metadata);

        // 3. SQL 실행
        jdbcExecutor.executeUpdate(sql);
    }
}
```

**DeleteAction 구현**:

```java
public class DeleteAction implements EntityAction {
    private final Object entity;

    public DeleteAction(Object entity) {
        this.entity = entity;
    }

    @Override
    public void execute(JdbcExecutor executor, MetadataRegistry registry) {
        EntityDeleter deleter = new EntityDeleter(executor, registry);
        deleter.delete(entity);
    }

    @Override
    public Object getEntity() {
        return entity;
    }
}
```

**EntityManager.remove() 구현**:

```java

@Override
public void remove(Object entity) {
    checkOpen();
    requireActiveTransaction();

    // 관리 중인 엔티티만 삭제 가능
    if (!persistenceContext.contains(entity)) {
        throw new IllegalArgumentException("Entity is not managed");
    }

    // REMOVED 상태로 마킹 + DELETE 액션 추가
    persistenceContext.removeEntity(entity);
}
```

**PersistenceContext.removeEntity() 개선**:

```java
public void removeEntity(Object entity) {
    EntityEntry entry = entityEntries.get(entity);
    if (entry != null) {
        // 상태를 REMOVED로 변경
        entry.markAsRemoved();

        // DELETE 액션 추가
        actionQueue.addDeleteAction(entity);

        // 1차 캐시에서 제거 (flush 후)
        // 주의: flush 전까지는 캐시에 남아있어야 함
    }
}
```

**ActionQueue.executeActions() 개선**:

```java
public void executeActions(JdbcExecutor executor,
                           MetadataRegistry registry,
                           PersistenceContext context) {
    // 실행 순서: INSERT → UPDATE → DELETE
    executeList(insertions, executor, registry);
    executeList(updates, executor, registry);
    executeList(deletions, executor, registry);

    // DELETE된 엔티티는 1차 캐시에서 제거
    for (EntityAction action : deletions) {
        context.removeFromCache(action.getEntity());
    }

    clear();
}
```

---

### Step 6.5: flush() 완전 구현

- [ ] flush() 메서드 완성
- [ ] Dirty Checking 자동 수행
- [ ] ActionQueue 실행
- [ ] 트랜잭션 커밋 시 자동 flush

**주요 변경사항**:

- EntityManager.flush() 완전 구현
- EntityTransaction.commit() 시 자동 flush

**EntityManager.flush() 완성**:

```java

@Override
public void flush() {
    checkOpen();
    requireActiveTransaction();

    // 1. Dirty Checking 수행
    persistenceContext.detectDirtyEntities(metadataRegistry);

    // 2. ActionQueue 실행
    Connection conn = transaction.getConnection();
    JdbcExecutor executor = new JdbcExecutor(conn);
    persistenceContext.flush(executor, metadataRegistry);
}
```

**EntityTransaction.commit() 개선**:

```java

@Override
public void commit() {
    if (!active) {
        throw new IllegalStateException("Transaction not active");
    }

    try {
        // 1. flush 수행 (자동)
        entityManager.flush();

        // 2. JDBC 트랜잭션 커밋
        connection.commit();

        active = false;
    } catch (Exception e) {
        rollback();
        throw new RuntimeException("Failed to commit transaction", e);
    }
}
```

---

### Step 6.6: 예외 처리 및 검증

- [ ] 엔티티 상태 검증
- [ ] 트랜잭션 필수 체크
- [ ] 예외 상황 처리
- [ ] 에러 메시지 개선

**주요 예외 상황**:

- 트랜잭션 없이 persist/remove 호출
- 이미 관리 중인 엔티티 persist
- 관리되지 않는 엔티티 remove
- ID가 없는 엔티티 merge

**예시**:

```java

@Override
public void persist(Object entity) {
    checkOpen();
    requireActiveTransaction();

    if (entity == null) {
        throw new IllegalArgumentException("Entity cannot be null");
    }

    if (persistenceContext.contains(entity)) {
        throw new IllegalArgumentException(
                "Entity is already managed: " + entity);
    }

    EntityMetadata metadata = metadataRegistry.getMetadata(entity.getClass());
    if (metadata == null) {
        throw new IllegalArgumentException(
                "Not an entity: " + entity.getClass().getName());
    }

    persistenceContext.addInsertAction(entity, metadata);
}

private void requireActiveTransaction() {
    if (!transaction.isActive()) {
        throw new IllegalStateException(
                "No active transaction. Call transaction.begin() first.");
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
│   │               ├── persister/                      # 🆕 Phase 6
│   │               │   ├── EntityPersister.java       # INSERT 처리
│   │               │   ├── EntityLoader.java          # SELECT 처리
│   │               │   ├── EntityUpdater.java         # UPDATE 처리
│   │               │   └── EntityDeleter.java         # DELETE 처리
│   │               ├── cache/
│   │               │   ├── PersistenceContext.java    # 🔄 flush 완성
│   │               │   ├── EntityEntry.java           # 🔄 스냅샷 갱신
│   │               │   └── action/
│   │               │       ├── InsertAction.java      # 🔄 Persister 통합
│   │               │       ├── UpdateAction.java      # 🔄 Updater 통합
│   │               │       └── DeleteAction.java      # 🔄 Deleter 통합
│   │               ├── core/
│   │               │   ├── EntityManager.java
│   │               │   ├── EntityManagerImpl.java     # 🔄 CRUD 완성
│   │               │   └── EntityTransactionImpl.java # 🔄 auto flush
│   │               ├── engine/
│   │               │   └── JdbcExecutor.java          # 🔄 executeInsert 추가
│   │               ├── metadata/
│   │               └── mapping/
│   └── test/
│       └── java/
│           └── io/
│               └── simplejpa/
│                   ├── persister/                     # 🆕 Persister 테스트
│                   │   ├── EntityPersisterTest.java
│                   │   ├── EntityLoaderTest.java
│                   │   ├── EntityUpdaterTest.java
│                   │   └── EntityDeleterTest.java
│                   └── integration/
│                       └── CrudIntegrationTest.java   # 🆕 전체 CRUD 테스트
```

---

## 구현 순서 요약

1. **EntityLoader** → find() 구현 (SELECT)
2. **EntityPersister** → persist() 구현 (INSERT)
3. **EntityUpdater** → Dirty Checking & merge() 구현 (UPDATE)
4. **EntityDeleter** → remove() 구현 (DELETE)
5. **flush() 완성** → 모든 것을 통합
6. **예외 처리** → 견고성 확보

---

## 핵심 통합 플로우

### persist() 전체 플로우

```
em.persist(user)
    ↓
EntityManagerImpl.persist()
    ↓ 검증 (트랜잭션, 중복 등)
PersistenceContext.addInsertAction()
    ↓ ActionQueue에 추가
    ↓ EntityEntry 생성 (MANAGED)
em.flush() or tx.commit()
    ↓
ActionQueue.executeActions()
    ↓
InsertAction.execute()
    ↓
EntityPersister.insert()
    ↓
InsertSqlGenerator.generate()
    ↓
JdbcExecutor.executeInsert()
    ↓
DB에 INSERT 실행
    ↓
생성된 ID를 엔티티에 설정
```

### find() 전체 플로우

```
em.find(User.class, 1L)
    ↓
EntityManagerImpl.find()
    ↓ 1차 캐시 확인
PersistenceContext.getEntity()
    ↓ 캐시 미스
EntityLoader.load()
    ↓
SelectSqlGenerator.generateById()
    ↓
JdbcExecutor.executeQuery()
    ↓
ResultSet → Entity 변환
    ↓
PersistenceContext.addEntity()
    ↓ 스냅샷 생성
1차 캐시에 저장
    ↓
엔티티 반환
```

### Dirty Checking & UPDATE 플로우

```
User user = em.find(User.class, 1L)  // 스냅샷 저장
    ↓
user.setName("updated")              // 엔티티만 변경
    ↓
em.flush() or tx.commit()
    ↓
PersistenceContext.detectDirtyEntities()
    ↓ 스냅샷 비교
EntityEntry.isModified() → true
    ↓
ActionQueue.addUpdateAction()
    ↓
UpdateAction.execute()
    ↓
EntityUpdater.update()
    ↓ 변경된 필드만
UpdateSqlGenerator.generate()
    ↓
JdbcExecutor.executeUpdate()
    ↓
DB에 UPDATE 실행
    ↓
스냅샷 갱신
```

### remove() 전체 플로우

```
em.remove(user)
    ↓
EntityManagerImpl.remove()
    ↓ 검증 (관리 중인 엔티티인지)
PersistenceContext.removeEntity()
    ↓ EntityEntry 상태 → REMOVED
    ↓ ActionQueue에 DELETE 추가
em.flush() or tx.commit()
    ↓
ActionQueue.executeActions()
    ↓
DeleteAction.execute()
    ↓
EntityDeleter.delete()
    ↓
DeleteSqlGenerator.generate()
    ↓
JdbcExecutor.executeUpdate()
    ↓
DB에서 DELETE 실행
    ↓
1차 캐시에서 제거
```

---

## 핵심 설계 원칙

### 1. 책임 분리 (Single Responsibility)

- 각 Persister는 하나의 작업만 담당
- SQL 생성은 Generator에 위임
- JDBC 실행은 Executor에 위임

### 2. 영속성 컨텍스트와의 긴밀한 통합

- 모든 작업은 영속성 컨텍스트를 통해 수행
- 1차 캐시 자동 관리
- 스냅샷 자동 생성 및 갱신

### 3. 쓰기 지연 유지

- persist/remove는 즉시 실행하지 않음
- ActionQueue에 추가만
- flush 시점에 일괄 실행

### 4. 자동 변경 감지

- 명시적 update() 호출 불필요
- flush 시 자동으로 Dirty Checking
- 변경된 필드만 UPDATE

---

## Phase 6 완료 후 가능한 것

✅ **완전한 CRUD 작업**:

- `persist()` - 엔티티 저장
- `find()` - 엔티티 조회 (1차 캐시 포함)
- `merge()` - 준영속 엔티티 병합
- `remove()` - 엔티티 삭제

✅ **영속성 컨텍스트 기능**:

- 1차 캐시를 통한 성능 최적화
- 동일성 보장 (== 비교)
- 쓰기 지연 (Write-Behind)
- 자동 변경 감지 (Dirty Checking)

✅ **트랜잭션 관리**:

- 트랜잭션 내 작업 보장
- 커밋 시 자동 flush
- 롤백 시 영속성 컨텍스트 초기화

**아직 안 되는 것**:

- JPQL 쿼리 (Phase 7)
- 관계 매핑 (Phase 8)
- 지연 로딩 (Phase 9)
- 2차 캐시 (Phase 10)

---

## 다음 단계 (Phase 7 예고)

Phase 7에서는 **JPQL 쿼리 처리**를 구현합니다:

- JPQL 파서 (간단한 SELECT만)
- JPQL → SQL 변환
- `Query` 인터페이스
- `TypedQuery` 구현
- Parameter Binding

---

## 테스트 전략

### 단위 테스트

- **EntityPersisterTest**: INSERT 로직 검증
- **EntityLoaderTest**: SELECT 로직 검증
- **EntityUpdaterTest**: UPDATE 로직 검증
- **EntityDeleterTest**: DELETE 로직 검증

### 통합 테스트

- **CrudIntegrationTest**:
    - persist → find 시나리오
    - Dirty Checking → 자동 UPDATE
    - remove → flush 시나리오
    - 1차 캐시 동작 검증
    - 트랜잭션 롤백 시나리오

---

## 예제: 전체 CRUD 시나리오

```java
// EntityManagerFactory 생성
EntityManagerFactory emf = Persistence.createEntityManagerFactory(config);
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

try{
        tx.

begin();

// === 1. persist (INSERT) ===
User user = new User("John", "john@email.com");
    em.

persist(user);
// 아직 DB에 저장 안 됨, ActionQueue에만 추가
    System.out.

println("User ID before flush: "+user.getId()); // null

        em.

flush();
// 이제 DB에 저장됨
    System.out.

println("User ID after flush: "+user.getId()); // 1

// === 2. find (SELECT) ===
User foundUser = em.find(User.class, user.getId());
    assert foundUser ==user; // 동일성 보장 (1차 캐시)

// === 3. Dirty Checking (자동 UPDATE) ===
    foundUser.

setName("John Updated");
// em.update() 같은 메서드 호출 불필요!

    em.

flush();
// UPDATE users SET name = 'John Updated' WHERE id = 1
// 자동으로 UPDATE 실행됨!

// === 4. merge (준영속 엔티티 병합) ===
    em.

clear(); // 영속성 컨텍스트 초기화

User detachedUser = new User(1L, "John Merged", "john@email.com");
User mergedUser = em.merge(detachedUser);
// DB에서 로드 → 필드 복사 → Dirty Checking → UPDATE

// === 5. remove (DELETE) ===
    em.

remove(mergedUser);
// 아직 DB에서 삭제 안 됨, ActionQueue에만 추가

    assert!em.

contains(mergedUser); // 더 이상 관리되지 않음

    tx.

commit(); // 자동 flush → DELETE 실행
// DELETE FROM users WHERE id = 1

}catch(
Exception e){
        if(tx.

isActive()){
        tx.

rollback();
    }
            e.

printStackTrace();
}finally{
        em.

close();
    emf.

close();
}
```

---

## Phase 6 핵심 체크리스트

### EntityLoader

- [ ] load() 메서드 구현
- [ ] ResultSet → Entity 변환
- [ ] 리플렉션으로 객체 생성 및 필드 설정
- [ ] find()와 통합

### EntityPersister

- [ ] insert() 메서드 구현
- [ ] 생성된 ID 자동 설정
- [ ] InsertAction과 통합
- [ ] persist()와 통합

### EntityUpdater

- [ ] update() 메서드 구현
- [ ] 변경된 필드만 UPDATE
- [ ] 스냅샷 갱신
- [ ] UpdateAction과 통합
- [ ] merge() 구현

### EntityDeleter

- [ ] delete() 메서드 구현
- [ ] DeleteAction과 통합
- [ ] remove()와 통합
- [ ] 1차 캐시에서 제거

### flush() 완성

- [ ] Dirty Checking 자동 수행
- [ ] ActionQueue 실행
- [ ] 트랜잭션 커밋 시 자동 flush

### 예외 처리

- [ ] 엔티티 상태 검증
- [ ] 트랜잭션 필수 체크
- [ ] 명확한 에러 메시지

---

이제 Phase 6을 시작할 준비가 되었습니다! 🚀

Phase 6를 완료하면 드디어 완전히 동작하는 JPA 구현체가 완성됩니다. 실제로 데이터베이스에 엔티티를 저장하고 조회하며, 변경 감지를 통해 자동으로 업데이트되는 것을 확인할 수 있습니다!