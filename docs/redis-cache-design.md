# Redis 캐시 도입 설계 문서

## 1. 개요

### 1.1 목적
- SOAP 템플릿 및 변환 결과 캐싱으로 응답 시간 단축
- 연결 풀 상태 및 메트릭 데이터 중앙 집중화
- 분산 환경에서 세션 데이터 공유

### 1.2 예상 효과
- **응답 시간**: 20-30% 감소
- **DB 부하**: 40-50% 감소
- **메모리 효율성**: 15-20% 개선

## 2. 아키텍처 설계

### 2.1 캐시 계층 구조
```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
┌──────▼──────┐
│  Gateway    │
└──────┬──────┘
       │
┌──────▼──────┐     ┌─────────┐
│ Redis Cache │◄────┤ Cluster │
└──────┬──────┘     └─────────┘
       │
┌──────▼──────┐
│ SOAP Service│
└─────────────┘
```

### 2.2 캐시 전략

#### 캐시 키 설계
- **템플릿 캐시**: `template:{service}:{function}:{version}`
- **SOAP 응답**: `soap:response:{hash(request)}`
- **메트릭 데이터**: `metrics:{type}:{timestamp}`
- **세션 데이터**: `session:{userId}:{sessionId}`

#### TTL 정책
- 템플릿: 24시간
- SOAP 응답: 5분
- 메트릭: 1시간
- 세션: 30분

### 2.3 캐시 무효화 전략
- **LRU (Least Recently Used)** 정책
- 이벤트 기반 무효화
- 버전 기반 캐시 키 관리

## 3. 구현 계획

### 3.1 의존성 추가
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
</dependency>
```

### 3.2 환경별 Redis 설정
```yaml
# application.yml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 10
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
        cluster:
          refresh:
            adaptive: true
            period: 30s
```

## 4. 성능 최적화 고려사항

### 4.1 연결 풀 설정
- 최대 연결 수: CPU 코어 수 * 2
- 유휴 연결 유지: 최소 2개
- 연결 타임아웃: 2초

### 4.2 직렬화 최적화
- MessagePack 또는 Protobuf 사용 고려
- JSON 대신 바이너리 형식으로 15-20% 성능 향상

### 4.3 파이프라이닝
- 다중 명령 일괄 처리
- 네트워크 왕복 시간 최소화

## 5. 모니터링 및 운영

### 5.1 주요 메트릭
- 캐시 히트율 (목표: >80%)
- 평균 응답 시간
- 메모리 사용률
- 연결 풀 상태

### 5.2 알림 설정
- 캐시 히트율 < 60%
- 메모리 사용률 > 80%
- 연결 실패 > 5회/분

## 6. 롤백 계획
- Feature Toggle로 Redis 캐시 ON/OFF
- 기존 로직으로 자동 폴백
- 캐시 장애 시 직접 서비스 호출