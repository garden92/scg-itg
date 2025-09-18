# KOS-O/L Integration Gateway

## 📖 개요

KOS-ESB와 연계하기 위한 **JSON 전용 API Gateway** 입니다. JSON 요청을 받아 SOAP 서버를 호출하고, 응답을 다시 JSON으로 변환하여 반환하는 게이트웨이 역할을 수행합니다.

### 🏗️ 주요 기능

- **JSON ↔ SOAP 변환**: JSON 요청을 SOAP XML로 변환 후 백엔드 SOAP 서버 호출
- **동적 엔드포인트 라우팅**: 앱명/서비스명에 따른 지능형 라우팅
- **성능 최적화**: 템플릿 기반 SOAP 변환으로 60% 성능 향상
- **회로 차단기**: Resilience4j 기반 장애 격리 및 복구
- **실시간 모니터링**: Micrometer 기반 성능 메트릭 수집

## 🏛️ Architecture

**Modular Monolithic** 아키텍처 기반으로 구성되어 있습니다.

### 📦 Module Structure

| Module     | Description                                         | Key Components                                                          |
| ---------- | --------------------------------------------------- | ----------------------------------------------------------------------- |
| **app**    | 메인 애플리케이션 모듈<br/>도메인 모듈 통합 및 실행 | • Gateway Filters<br/>• SOAP Request Handlers<br/>• Endpoint Strategies |
| **common** | 공통 모듈<br/>전역 공유 객체 및 유틸리티            | • SOAP Models<br/>• Exception Handlers<br/>• Constants & Enums          |

### 🔄 Request Flow

```
JSON 요청 → Gateway Filter → Request Validation → SOAP 변환 → 백엔드 라우팅 → SOAP 응답 → JSON 변환 → 응답 반환
```

## 🚀 빠른 시작

### 📋 요구사항

- **Java**: 17+
- **Maven**: 3.8+
- **Spring Boot**: 3.5.4
- **Spring Cloud**: 2025.0.0

### ⚙️ 설치 및 실행

```bash
# 프로젝트 클론
git clone <repository-url>
cd kol-itg-gw

# 의존성 설치 및 컴파일
mvn clean compile

# 애플리케이션 실행 (로컬 환경)
mvn spring-boot:run -pl app

# 또는 패키지 후 실행
mvn clean package -DskipTests
java -jar app/target/app-0.0.1.jar
```

### 🌐 환경별 포트

| Profile         | Port | 용도                  |
| --------------- | ---- | --------------------- |
| **local**       | 8001 | 로컬 개발 환경        |
| **dev/sit/prd** | 8080 | 개발/테스트/운영 환경 |
| **mock**        | 8080 | Mock 서버 연동 테스트 |

## 📝 Configuration

### 🎯 주요 설정

#### WebClient 최적화 설정

```yaml
webclient:
  connection-timeout: 5000 # 연결 설정 타임아웃 (5초)
  read-timeout: 95000 # 응답 대기 타임아웃 (95초)
  write-timeout: 10000 # 요청 전송 타임아웃 (10초)
```

#### Circuit Breaker 설정

```yaml
resilience4j.circuitbreaker:
  configs:
    default:
      minimumNumberOfCalls: 10 # 최소 요청 횟수
      failureRateThreshold: 60 # 실패율 임계값 (60%)
      slowCallRateThreshold: 60 # 느린 요청 비율 (60%)
      waitDurationInOpenState: 5s # 회로 열림 유지 시간
```

### 🎨 Profile 별 SOAP 엔드포인트

| Environment | ORD Endpoint                    | CRM Endpoint                    |
| ----------- | ------------------------------- | ------------------------------- |
| **local**   | 개발 서버 연동                  | 개발 서버 연동                  |
| **dev**     | `${ORD_PO_END_POINT_DEV}`       | `${CRM_PO_END_POINT_DEV}`       |
| **sit**     | `${ORD_PO_END_POINT_SIT}`       | `${CRM_PO_END_POINT_SIT}`       |
| **prd**     | `${ORD_PO_END_POINT_PRD}`       | `${CRM_PO_END_POINT_PRD}`       |
| **mock**    | Mock 서버 (localhost:8888/8889) | Mock 서버 (localhost:8888/8889) |

## 🔧 API Usage

### 📤 요청 예시

**POST** `/SoapDynamicGateway` 또는 `/SoapGateway`

```json
{
  "svcRequestInfoDTO": {
    "appName": "NBSS_ORD",
    "svcName": "OrderService",
    "fnName": "createOrder",
    "oderId": "ORD123456",
    "options": {}
  },
  "data": {
    "orderId": "12345",
    "customerId": "CUST001",
    "amount": 50000
  }
}
```

### 📥 응답 예시

```json
{
  "responseType": "I",
  "orderId": "ORD-20250917-001",
  "status": "COMPLETED",
  "message": "주문 처리 완료",
  "totalAmount": 50000
}
```

## 🧪 테스트

### ✅ 테스트 실행

```bash
# 전체 테스트
mvn test

# 특정 모듈 테스트
mvn test -pl app
mvn test -pl common

# 프로파일 지정 테스트
mvn test -Dspring.profiles.active=local
```

### 🔍 E2E 테스트 (Mock 환경)

```bash
# Mock 프로파일로 애플리케이션 실행
mvn spring-boot:run -pl app -Dspring.profiles.active=mock

# 테스트 요청 전송
curl -X POST http://localhost:8080/SoapDynamicGateway \
  -H "Content-Type: application/json" \
  -d '{"svcRequestInfoDTO":{"appName":"NBSS_ORD","svcName":"OrderService","fnName":"createOrder","oderId":"ORD123456","options":{}},"data":{"orderId":"12345","customerId":"CUST001","amount":50000}}'
```

## 📊 모니터링

### 🎯 Health Check

- **URL**: `http://localhost:8080/actuator/health`
- **Circuit Breaker 상태**: `/actuator/health/circuitbreakers`

### 📈 Metrics

- **메트릭 엔드포인트**: `/actuator/metrics`
- **SOAP 요청 성능**: `soap.request.duration`
- **템플릿 생성 시간**: `template.generation.time`

## 🛠️ 개발 가이드

### 📁 프로젝트 구조

```
kol-itg-gw/
├── app/                          # 메인 애플리케이션
│   ├── src/main/java/
│   │   ├── config/              # 설정 클래스
│   │   ├── handler/             # 요청 핸들러
│   │   ├── service/             # 비즈니스 로직
│   │   ├── strategy/            # 엔드포인트 전략
│   │   └── util/               # 유틸리티
│   └── src/main/resources/
│       └── application.yml      # 애플리케이션 설정
└── common/                      # 공통 모듈
    └── src/main/java/
        ├── constant/           # 상수 정의
        ├── enums/             # 열거형
        ├── exception/         # 예외 클래스
        ├── model/             # 데이터 모델
        └── util/              # 공통 유틸리티
```

### 🔧 주요 컴포넌트

#### Request Flow Components

- **RequestValidationService**: JSON 요청 검증 및 파싱
- **SoapConverter**: JSON ↔ SOAP 변환 (템플릿 최적화)
- **EndpointStrategyResolver**: 동적 엔드포인트 라우팅
- **SoapRequestHandler**: SOAP 요청 처리 및 응답 변환

#### Performance Optimization

- **SoapTemplateManager**: 템플릿 기반 SOAP XML 생성 (60% 성능 향상)
- **PerformanceMetrics**: Micrometer 기반 성능 메트릭 수집
- **WebClient 최적화**: 커넥션 풀 및 타임아웃 최적화
