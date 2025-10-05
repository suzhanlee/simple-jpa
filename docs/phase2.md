# Phase 2: 기반 인프라 구현

## 개요
**왜 이 단계?** 메타데이터를 활용하여 실제 데이터베이스와 통신하기 위한 핵심 인프라를 구축합니다. JDBC 연결, 트랜잭션, SQL 실행의 기반을 마련합니다.

## 구현 단계

### Step 2.1: JDBC Connection 관리
- [x] ConnectionProvider 인터페이스 정의
- [x] DriverManagerConnectionProvider 구현
- [x] Connection Pool 기본 구조 (선택사항)
- [x] Connection 생성 및 해제 로직

**주요 컴포넌트**:
- `ConnectionProvider`: Connection 제공 인터페이스
- `DriverManagerConnectionProvider`: DriverManager 기반 구현
- `ConnectionConfiguration`: DB 연결 설정 정보

**핵심 기능**:
- JDBC URL, username, password 설정
- Connection 생성 및 검증
- Connection 해제 및 리소스 관리
- 연결 오류 처리

### Step 2.2: Transaction 기본 구조
- [x] TransactionCoordinator 인터페이스
- [x] JdbcTransaction 구현
- [x] Transaction 상태 관리 (NOT_ACTIVE, ACTIVE, COMMITTED, ROLLED_BACK)
- [x] begin(), commit(), rollback() 메서드

**주요 컴포넌트**:
- `TransactionCoordinator`: 트랜잭션 조정 인터페이스
- `JdbcTransaction`: JDBC 기반 트랜잭션 구현
- `TransactionStatus`: 트랜잭션 상태 열거형

**핵심 기능**:
- 트랜잭션 시작 (begin)
- 트랜잭션 커밋 (commit)
- 트랜잭션 롤백 (rollback)
- 자동 커밋 모드 제어
- 트랜잭션 상태 추적

### Step 2.3: SQL 실행 엔진 (JdbcExecutor)
- [ ] JdbcExecutor 클래스 구현
- [ ] PreparedStatement 생성 및 실행
- [ ] SQL 파라미터 바인딩
- [ ] 실행 결과 반환 (int, ResultSet)

**주요 컴포넌트**:
- `JdbcExecutor`: SQL 실행 엔진
- `ParameterBinder`: PreparedStatement 파라미터 바인딩
- `SqlCommand`: SQL 명령 추상화

**핵심 기능**:
- INSERT/UPDATE/DELETE 실행 (executeUpdate)
- SELECT 실행 (executeQuery)
- PreparedStatement 파라미터 바인딩
- Statement 리소스 관리
- SQL 예외 처리

### Step 2.4: ResultSet 처리
- [ ] ResultSetExtractor 인터페이스
- [ ] 기본 타입 매핑 (Long, String, Integer, etc.)
- [ ] ResultSet → 객체 변환 로직
- [ ] 컬럼명 → 필드 매핑 (메타데이터 활용)

**주요 컴포넌트**:
- `ResultSetExtractor`: ResultSet 추출 인터페이스
- `EntityResultSetExtractor`: 엔티티 추출 구현체
- `TypeConverter`: JDBC 타입 → Java 타입 변환

**핵심 기능**:
- ResultSet 행 순회
- 컬럼 데이터 추출
- Java 타입 변환 (JDBC Type → Java Type)
- 엔티티 인스턴스 생성 및 필드 설정
- null 값 처리

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
│   │               │   └── sql/
│   │               │       └── (Phase 3에서 구현)
│   │               ├── transaction/
│   │               │   ├── TransactionCoordinator.java
│   │               │   ├── JdbcTransaction.java
│   │               │   └── TransactionStatus.java
│   │               └── util/
│   │                   └── TypeConverter.java
│   └── test/
│       ├── java/
│       │   └── io/
│       │       └── simplejpa/
│       │           ├── engine/
│       │           │   ├── connection/
│       │           │   ├── jdbc/
│       │           │   └── transaction/
│       │           └── integration/
│       └── resources/
│           └── test-db.properties
```

---

## 구현 순서 요약

1. **Connection 관리** → JDBC Connection 생성 및 관리 인프라
2. **Transaction 구조** → 트랜잭션 시작/커밋/롤백 로직
3. **SQL 실행 엔진** → PreparedStatement 기반 SQL 실행
4. **ResultSet 처리** → 쿼리 결과를 Java 객체로 변환

---

## 핵심 의존성

- **Phase 1 메타데이터**: EntityMetadata, AttributeMetadata 활용
- **JDBC API**: Connection, PreparedStatement, ResultSet
- **Exception 처리**: SQLException → 커스텀 예외 변환

---

## 테스트 전략

- **Connection 테스트**: 실제 DB 연결 테스트 (H2, MySQL 등)
- **Transaction 테스트**: begin/commit/rollback 동작 검증
- **JdbcExecutor 테스트**: SQL 실행 및 ResultSet 변환 검증
- **통합 테스트**: Connection → Transaction → SQL 실행 전체 플로우
