# Phase 3: SQL 생성 엔진 구현

## 개요
**왜 이 단계?** Phase 1의 메타데이터와 Phase 2의 JDBC 인프라를 활용하여 실제 SQL 문을 동적으로 생성하는 엔진을 구축합니다. 이는 ORM의 핵심 기능인 객체-관계 매핑의 실제 동작을 담당합니다.

## 구현 단계

### Step 3.1: INSERT SQL 생성 (메타데이터 활용)
- [ ] InsertSqlGenerator 클래스 구현
- [ ] EntityMetadata를 활용한 INSERT 문 생성
- [ ] 컬럼명 및 VALUES 절 자동 생성
- [ ] Primary Key 처리 (자동 생성 vs 수동 할당)

**주요 컴포넌트**:
- `InsertSqlGenerator`: INSERT SQL 생성기
- `SqlBuilder`: SQL 문자열 빌더 유틸리티
- `IdentifierGenerator`: ID 생성 전략 (IDENTITY, SEQUENCE, AUTO)

**핵심 기능**:
- EntityMetadata에서 테이블명 추출
- AttributeMetadata에서 컬럼명 목록 추출
- INSERT INTO table_name (col1, col2, ...) VALUES (?, ?, ...) 생성
- Generated ID 처리 (IDENTITY, SEQUENCE)
- null 값 처리

**예시**:
```java
// Entity: User(id, name, email)
// 생성 결과: INSERT INTO users (id, name, email) VALUES (?, ?, ?)
```

### Step 3.2: SELECT SQL 생성
- [ ] SelectSqlGenerator 클래스 구현
- [ ] Primary Key 기반 단일 조회 (findById)
- [ ] 전체 컬럼 SELECT 문 생성
- [ ] WHERE 절 생성 (ID 기반)

**주요 컴포넌트**:
- `SelectSqlGenerator`: SELECT SQL 생성기
- `WhereClauseBuilder`: WHERE 절 빌더
- `ColumnSelector`: 컬럼 선택 로직

**핵심 기능**:
- SELECT col1, col2, ... FROM table_name WHERE id = ? 생성
- EntityMetadata에서 모든 컬럼 추출
- Primary Key 컬럼 식별
- 별칭(alias) 처리 (선택사항)

**예시**:
```java
// Entity: User(id, name, email)
// 생성 결과: SELECT id, name, email FROM users WHERE id = ?
```

### Step 3.3: UPDATE SQL 생성
- [ ] UpdateSqlGenerator 클래스 구현
- [ ] SET 절 생성 (변경된 필드만 포함)
- [ ] WHERE 절 생성 (Primary Key 기반)
- [ ] Dirty Checking과 연계 (Phase 6에서 완성)

**주요 컴포넌트**:
- `UpdateSqlGenerator`: UPDATE SQL 생성기
- `SetClauseBuilder`: SET 절 빌더
- `DirtyFieldDetector`: 변경 필드 감지 (Phase 6에서 완성)

**핵심 기능**:
- UPDATE table_name SET col1 = ?, col2 = ? WHERE id = ? 생성
- Primary Key를 제외한 컬럼만 SET 절에 포함
- null 값 업데이트 처리
- 변경된 필드만 선택적으로 포함 (최적화)

**예시**:
```java
// Entity: User(id, name, email) - name만 변경
// 생성 결과: UPDATE users SET name = ? WHERE id = ?
// 또는 전체: UPDATE users SET name = ?, email = ? WHERE id = ?
```

### Step 3.4: DELETE SQL 생성
- [ ] DeleteSqlGenerator 클래스 구현
- [ ] WHERE 절 생성 (Primary Key 기반)
- [ ] 단순 삭제 구조

**주요 컴포넌트**:
- `DeleteSqlGenerator`: DELETE SQL 생성기
- `WhereClauseBuilder`: WHERE 절 빌더 (재사용)

**핵심 기능**:
- DELETE FROM table_name WHERE id = ? 생성
- Primary Key 기반 삭제만 지원 (단일 엔티티)
- Cascade 삭제는 Phase 8에서 구현

**예시**:
```java
// Entity: User(id)
// 생성 결과: DELETE FROM users WHERE id = ?
```

### Step 3.5: WHERE 절 생성 (공통)
- [ ] WhereClauseBuilder 클래스 구현
- [ ] Primary Key 기반 조건
- [ ] 복합 조건 지원 (AND, OR) - 기본 구조만
- [ ] 파라미터 바인딩 위치 관리

**주요 컴포넌트**:
- `WhereClauseBuilder`: WHERE 절 빌더
- `Condition`: 조건 표현 객체
- `ParameterIndex`: 파라미터 위치 추적

**핵심 기능**:
- WHERE id = ? 생성
- WHERE col1 = ? AND col2 = ? 지원
- IN, LIKE, BETWEEN 등은 Phase 7에서 확장
- 파라미터 순서 및 개수 추적

---

## 현재 디렉토리 구조

```
simple-jpa/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── io/
│   │           └── simplejpa/
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
│   │               │   └── sql/                           # 🆕 Phase 3
│   │               │       ├── SqlGenerator.java          # SQL 생성 인터페이스
│   │               │       ├── InsertSqlGenerator.java    # INSERT 생성
│   │               │       ├── SelectSqlGenerator.java    # SELECT 생성
│   │               │       ├── UpdateSqlGenerator.java    # UPDATE 생성
│   │               │       ├── DeleteSqlGenerator.java    # DELETE 생성
│   │               │       ├── WhereClauseBuilder.java    # WHERE 절 빌더
│   │               │       ├── SqlBuilder.java            # SQL 문자열 빌더
│   │               │       └── ParameterIndex.java        # 파라미터 위치 추적
│   │               ├── metadata/
│   │               │   ├── EntityMetadata.java
│   │               │   ├── AttributeMetadata.java
│   │               │   └── IdentifierMetadata.java
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
│       │           ├── engine/
│       │           │   └── sql/                           # 🆕 SQL 생성 테스트
│       │           │       ├── InsertSqlGeneratorTest.java
│       │           │       ├── SelectSqlGeneratorTest.java
│       │           │       ├── UpdateSqlGeneratorTest.java
│       │           │       ├── DeleteSqlGeneratorTest.java
│       │           │       └── WhereClauseBuilderTest.java
│       │           └── integration/
│       │               └── SqlGenerationIntegrationTest.java
│       └── resources/
│           └── test-db.properties
```

---

## 구현 순서 요약

1. **INSERT SQL 생성** → 가장 기본적인 CRUD 시작점
2. **SELECT SQL 생성** → findById 구현을 위한 조회 SQL
3. **UPDATE SQL 생성** → 엔티티 수정을 위한 UPDATE 문
4. **DELETE SQL 생성** → 엔티티 삭제를 위한 DELETE 문
5. **WHERE 절 생성** → SELECT, UPDATE, DELETE에서 공통으로 사용

---

## 핵심 의존성

- **Phase 1 메타데이터**: EntityMetadata, AttributeMetadata, IdentifierMetadata 활용
- **Phase 2 JDBC 인프라**: JdbcExecutor, ParameterBinder와 연계
- **Reflection API**: 필드 값 추출 및 타입 변환

---

## 테스트 전략

- **단위 테스트**: 각 SQL Generator가 올바른 SQL 문을 생성하는지 검증
- **메타데이터 기반 테스트**: 실제 엔티티 클래스의 메타데이터로 SQL 생성
- **파라미터 바인딩 테스트**: 생성된 SQL의 ? 개수와 실제 파라미터 개수 일치 검증
- **통합 테스트**: SQL 생성 → JdbcExecutor 실행 → ResultSet 변환 전체 플로우

---

## SQL 생성 예시

### User 엔티티 예시
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;
}
```

### 생성되는 SQL

**INSERT**:
```sql
INSERT INTO users (id, name, email) VALUES (?, ?, ?)
```

**SELECT**:
```sql
SELECT id, name, email FROM users WHERE id = ?
```

**UPDATE**:
```sql
UPDATE users SET name = ?, email = ? WHERE id = ?
```

**DELETE**:
```sql
DELETE FROM users WHERE id = ?
```

---

## Phase 3 완료 후 가능한 것

- EntityMetadata를 기반으로 모든 CRUD SQL 자동 생성
- Phase 4의 EntityManager 구현 시 즉시 활용 가능
- Phase 6의 Persister 계층에서 SQL 생성 로직 재사용
- 수동 SQL 작성 없이 완전히 메타데이터 기반 동작

---

## 다음 단계 (Phase 4 예고)

Phase 4에서는 **핵심 API (EntityManager, EntityManagerFactory, EntityTransaction)**를 구현하여 이제까지 만든 컴포넌트들을 통합하고, 실제 사용자가 사용할 수 있는 JPA API를 제공합니다.