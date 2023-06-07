package com.example.bigserver;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Measurement {
    public Measurement(LocalDateTime timestamp, String unit, BigDecimal value) {
        Timestamp = timestamp;
        Unit = unit;
        Value = value;
    }
    public LocalDateTime Timestamp;
    public String Unit;
    public BigDecimal Value;
}
