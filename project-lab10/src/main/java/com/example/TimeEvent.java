package com.example;

import java.time.LocalDateTime;

public class TimeEvent {
    private LocalDateTime time;

    public TimeEvent() {
    }

    public TimeEvent(LocalDateTime time) {
        this.time = time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
