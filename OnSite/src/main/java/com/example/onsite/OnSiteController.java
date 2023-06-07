package com.example.onsite;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;

@RestController
public class OnSiteController {
    MeasurementsBuffer buffer;

    public OnSiteController() {
        this.buffer = new MeasurementsBuffer();
        buffer.Load("OnSiteBuffer.json");

        // check sites for new data
        new Thread(new Runnable() {
            private MeasurementsBuffer buffer;

            public Runnable init(MeasurementsBuffer buffer) {
                this.buffer = buffer;
                return this;
            }

            @Override
            public void run() {
                var rand = new Random();
                var supportedUnits = new String []{"kV", "A", "VA"};
                while(true)
                {
                    buffer.Insert(0, new Measurement(
                            Timestamp.from(Instant.now()).toLocalDateTime(),
                            supportedUnits[rand.nextInt(supportedUnits.length)],
                            new BigDecimal(100.0 + 30.0 * rand.nextDouble()).setScale(2, BigDecimal.ROUND_HALF_UP)));
                    buffer.Save();
                    try {
                        Thread.sleep(5 * 1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }.init(buffer)).start();
    }

    @GetMapping("/api/all")
    ArrayList<Measurement> GetAll() {
        return buffer.getMeasurements();
    }

    @GetMapping("/api/get_saved_values")
    ArrayList<Measurement> GetSavedValues() {
        return buffer.GetContentAndEmptyIt();
    }
}
