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
        // Simple in-memory queue (temporary for v1)
        queue.offer(job);
    }

    @PostConstruct
    void startWorker() {
        // Start consumer thread on app boot
        worker = new Thread(this::runLoop, "ai-itinerary-queue");
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    void stopWorker() {
        // Stop consumer on shutdown
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void runLoop() {
        while (running) {
            try {
                // Wait until a job arrives
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
