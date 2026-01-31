package com.planit.domain.trip.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiItineraryQueue {

    private static final Logger logger = LoggerFactory.getLogger(AiItineraryQueue.class);

    private final BlockingQueue<AiItineraryJob> queue = new LinkedBlockingQueue<>();
    private final AiItineraryProcessor processor;
    private Thread worker;
    private volatile boolean running = true;

    public AiItineraryQueue(AiItineraryProcessor processor) {
        this.processor = processor;
    }

    public void enqueue(AiItineraryJob job) {
        // 간단한 인메모리 큐 (v1 임시)
        queue.offer(job);
    }

    @PostConstruct
    void startWorker() {
        // 앱 시작 시 큐 소비 스레드 시작
        worker = new Thread(this::runLoop, "ai-itinerary-queue");
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    void stopWorker() {
        // 종료 시 소비 스레드 중단
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void runLoop() {
        while (running) {
            try {
                // 큐에 작업이 들어올 때까지 대기
                AiItineraryJob job = queue.take();
                processor.process(job);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                logger.error("AI itinerary job failed", ex);
            }
        }
    }
}
