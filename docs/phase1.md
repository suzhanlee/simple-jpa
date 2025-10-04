# Phase 1: 메타데이터 & 매핑 구현

## 개요
**왜 먼저?** 메타데이터 없이는 아무것도 할 수 없음. DB 연결 없이도 구현/테스트 가능.

## 구현 단계

### Step 1.1: 애노테이션 정의
- [x] @Entity - 엔티티 클래스 표시
- [x] @Table - 테이블 이름 매핑
- [x] @Id - Primary Key 표시
- [x] @Column - 컬럼 매핑

### Step 1.2: EntityMetadata 구조 설계
- [x] EntityMetadata 클래스
- [x] AttributeMetadata 클래스
- [x] IdentifierMetadata 클래스
- [x] 테이블-컬럼 매핑 정보 저장

### Step 1.3: 리플렉션 기반 메타데이터 추출'
- [x] AnnotationProcessor 구현
- [x] 클래스 스캔 및 애노테이션 읽기
- [x] 필드 정보 추출
- [x] 메타데이터 객체 생성

### Step 1.4: MetadataRegistry (메타데이터 저장소)
- [x] 메타데이터 캐싱
- [x] 클래스 → 메타데이터 조회
- [ ] 테스트 엔티티로 검증

---

## 현재 디렉토리 구조

```
simple-jpa/
├── CLAUDE.md
├── docs/
│   └── phase1.md
├── src/
│   ├── main/
│   │   └── java/
│   │       └── io/
│   │           └── simplejpa/
│   │               ├── annotation/         # 애노테이션 정의 (@Entity, @Table, @Id, @Column)
│   │               ├── cache/              # 캐시 (PersistenceContext, IdentityMap)
│   │               ├── core/               # 핵심 API (EntityManager, EntityManagerFactory)
│   │               ├── dialect/            # DB 방언 (MySQL, PostgreSQL 등)
│   │               ├── engine/
│   │               │   ├── connection/     # Connection 관리
│   │               │   ├── jdbc/           # JDBC 실행
│   │               │   └── sql/            # SQL 생성
│   │               ├── exception/          # 예외 클래스
│   │               ├── mapping/            # 매핑 처리 (AnnotationProcessor 등)
│   │               ├── metadata/           # 메타데이터 (EntityMetadata, AttributeMetadata)
│   │               ├── persister/          # 영속화 실행 (EntityPersister, EntityLoader)
│   │               ├── proxy/              # 프록시 (JDK Dynamic Proxy)
│   │               ├── query/
│   │               │   ├── criteria/       # Criteria API
│   │               │   └── jpql/           # JPQL 파싱
│   │               ├── transaction/        # 트랜잭션 관리
│   │               └── util/               # 유틸리티
│   └── test/
│       ├── java/
│       │   └── io/
│       │       └── simplejpa/
│       │           ├── entity/             # 테스트용 엔티티
│       │           ├── integration/        # 통합 테스트
│       │           └── metadata/           # 메타데이터 테스트
│       └── resources/
```

---

## 구현 순서 요약

1. **애노테이션 정의** → 엔티티 클래스에 사용할 @Entity, @Table, @Id, @Column
2. **메타데이터 구조** → EntityMetadata, AttributeMetadata, IdentifierMetadata
3. **리플렉션 처리** → AnnotationProcessor로 애노테이션 읽고 메타데이터 생성
4. **메타데이터 저장소** → MetadataRegistry로 메타데이터 캐싱 및 조회
