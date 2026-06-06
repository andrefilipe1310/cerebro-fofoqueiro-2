package com.fofoqueiro.recording.dto.response;

import java.util.List;

public record TimelineResponse(List<RecordingResponse> segments, List<GapResponse> gaps) {

    public record GapResponse(String from, String to) {}
}
