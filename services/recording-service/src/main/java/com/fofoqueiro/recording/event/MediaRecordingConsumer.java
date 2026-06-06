package com.fofoqueiro.recording.event;

import com.fofoqueiro.recording.service.RecordingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaRecordingConsumer {

    private final RecordingService recordingService;

    @KafkaListener(topics = "recording.events", groupId = "recording-service")
    public void consume(String message) {
        try {
            recordingService.registerRecording(message);
        } catch (Exception e) {
            log.error("Error processing recording event: {}", e.getMessage(), e);
        }
    }
}
