package com.kt.kol.gateway.itg.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 성능 메트릭 수집 및 모니터링
 * - SOAP 요청/응답 시간 측정
 * - 연결 풀 상태 모니터링
 * - 에러율 및 처리량 추적
 */
@Component
@Slf4j
public class PerformanceMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // 카운터 메트릭
    private final Counter soapRequestCounter;
    private final Counter soapSuccessCounter; 
    private final Counter soapErrorCounter;
    private final Counter connectionPoolExhaustedCounter;
    
    // 타이머 메트릭
    private final Timer soapProcessingTimer;
    private final Timer xmlConversionTimer;
    private final Timer templateGenerationTimer;
    
    // 게이지 메트릭 (실시간 값)
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong pendingRequests = new AtomicLong(0);
    
    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 카운터 초기화
        this.soapRequestCounter = Counter.builder("kol.soap.requests.total")
                .description("Total SOAP requests processed")
                .register(meterRegistry);
                
        this.soapSuccessCounter = Counter.builder("kol.soap.requests.success")
                .description("Successful SOAP requests")
                .register(meterRegistry);
                
        this.soapErrorCounter = Counter.builder("kol.soap.requests.error")
                .description("Failed SOAP requests")
                .register(meterRegistry);
                
        this.connectionPoolExhaustedCounter = Counter.builder("kol.connection.pool.exhausted")
                .description("Connection pool exhausted events")
                .register(meterRegistry);
        
        // 타이머 초기화
        this.soapProcessingTimer = Timer.builder("kol.soap.processing.duration")
                .description("SOAP request processing time")
                .register(meterRegistry);
                
        this.xmlConversionTimer = Timer.builder("kol.xml.conversion.duration")
                .description("XML conversion processing time")
                .register(meterRegistry);
                
        this.templateGenerationTimer = Timer.builder("kol.template.generation.duration")
                .description("SOAP template generation time")
                .register(meterRegistry);
        
        // 게이지 등록
        meterRegistry.gauge("kol.connections.active", activeConnections);
        meterRegistry.gauge("kol.requests.pending", pendingRequests);
    }
    
    /**
     * SOAP 요청 시작 추적
     */
    public Timer.Sample startSoapRequest() {
        soapRequestCounter.increment();
        incrementPendingRequests();
        return Timer.start(meterRegistry);
    }
    
    /**
     * SOAP 요청 성공 기록
     */
    public void recordSoapSuccess(Timer.Sample sample) {
        sample.stop(soapProcessingTimer);
        soapSuccessCounter.increment();
        decrementPendingRequests();
        
        if (log.isDebugEnabled()) {
            log.debug("SOAP request succeeded. Success rate: {:.2f}%", 
                     getSuccessRate());
        }
    }
    
    /**
     * SOAP 요청 실패 기록
     */
    public void recordSoapError(Timer.Sample sample, String errorType) {
        sample.stop(soapProcessingTimer);
        soapErrorCounter.increment();
        decrementPendingRequests();
        
        Counter.builder("kol.soap.errors")
                .tag("type", errorType)
                .register(meterRegistry)
                .increment();
        
        log.warn("SOAP request failed. Error rate: {:.2f}%, Type: {}", 
                getErrorRate(), errorType);
    }
    
    /**
     * XML 변환 시간 측정
     */
    public Timer.Sample startXmlConversion() {
        return Timer.start(meterRegistry);
    }
    
    public void recordXmlConversion(Timer.Sample sample) {
        sample.stop(xmlConversionTimer);
    }
    
    /**
     * 템플릿 생성 시간 측정  
     */
    public Timer.Sample startTemplateGeneration() {
        return Timer.start(meterRegistry);
    }
    
    public void recordTemplateGeneration(Timer.Sample sample) {
        sample.stop(templateGenerationTimer);
    }
    
    /**
     * 연결 풀 사용량 업데이트
     */
    public void updateActiveConnections(long count) {
        activeConnections.set(count);
    }
    
    public void recordConnectionPoolExhausted() {
        connectionPoolExhaustedCounter.increment();
        log.error("Connection pool exhausted! Current metrics - Active: {}, Pending: {}", 
                 activeConnections.get(), pendingRequests.get());
    }
    
    // Private helper methods
    private void incrementPendingRequests() {
        pendingRequests.incrementAndGet();
    }
    
    private void decrementPendingRequests() {
        pendingRequests.decrementAndGet();
    }
    
    private double getSuccessRate() {
        double total = soapRequestCounter.count();
        if (total == 0) return 0.0;
        return (soapSuccessCounter.count() / total) * 100.0;
    }
    
    private double getErrorRate() {
        double total = soapRequestCounter.count();
        if (total == 0) return 0.0;
        return (soapErrorCounter.count() / total) * 100.0;
    }
    
    /**
     * 현재 성능 상태 로깅
     */
    public void logCurrentMetrics() {
        log.info("Performance Metrics - " +
                "Total Requests: {}, " +
                "Success Rate: {:.2f}%, " +
                "Error Rate: {:.2f}%, " +
                "Active Connections: {}, " +
                "Pending Requests: {}, " +
                "Avg Processing Time: {:.2f}ms",
                soapRequestCounter.count(),
                getSuccessRate(),
                getErrorRate(),
                activeConnections.get(),
                pendingRequests.get(),
                soapProcessingTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
    }
}