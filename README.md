### Introduce

Modular Monolithic 기반의 KOS-ESB와 연계하기 위한 API Gateway(/w, Spring Cloud Gateway)

### Architecture

Modular Monolithic 아키텍처이므로, 다음과 같은 모듈들로 구성

| Modules | Description                                                         |
| ------- | ------------------------------------------------------------------- |
| app     | 각 도메인 별 모듈을 통합하고 실행하는 Main 애플리케이션이 있는 모듈 |
| common  | 모든 모듈에서 전역적으로 사용하는 공통 개체가 담긴 모듈입니다.      |
