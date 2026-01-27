package com.tearsdeepmind.service;

import com.tearsdeepmind.model.CrawlerEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitoringService {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String jobId) {
        SseEmitter emitter = new SseEmitter(600000L); // 10 minutes timeout
        emitters.put(jobId, emitter);
        
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));
        emitter.onError((e) -> emitters.remove(jobId));
        
        return emitter;
    }

    public void publish(CrawlerEvent event) {
        SseEmitter emitter = emitters.get(event.jobId());
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("crawler-update")
                        .data(event));
                
                if ("JOB_FINISHED".equals(event.type()) || "JOB_FAILED".equals(event.type())) {
                    emitter.complete();
                }
            } catch (IOException e) {
                emitters.remove(event.jobId());
            }
        }
    }
}
