package com.example.onsite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

class MeasurementsBuffer
{
    private String sync = "";
    private JsonParser parser = JsonParserFactory.getJsonParser();
    private String path;

    public ArrayList<Measurement> getMeasurements() {
        return Measurements;
    }

    public ArrayList<Measurement> Measurements;

    ObjectMapper objectMapper;

    MeasurementsBuffer() {
        JavaTimeModule module = new JavaTimeModule();
        LocalDateTimeDeserializer deserializer = new LocalDateTimeDeserializer(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        module.addDeserializer(LocalDateTime.class, deserializer);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        objectMapper.registerModule(module);
    }

    public void Load(String path)
    {
        synchronized(sync)
        {
            Measurements = new ArrayList<Measurement>();
            var file = new File(path);
            if (file.exists())
            {
                Scanner scanner = null;
                try {
                    scanner = new Scanner(file);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                // end of file delimiter
                scanner.useDelimiter("\\Z");

                if (scanner.hasNext()) {
                    var content = scanner.next();
                    var list = JsonParserFactory.getJsonParser().parseList(content);
                    for (var element : list) {
                        if (element instanceof Map) {
                            Map < String, Object > map = (Map < String, Object > ) element;
                            Measurements.add(new Measurement(
                                    LocalDateTime.parse((String) map.get("Timestamp")),
                                    map.get("Unit").toString(),
                                    new BigDecimal((double) map.get("Value"))));
                        }
                    }
                }
                int i = 10;
            }
            this.path = path;            
        }
    }

    public void Save() {
        Save(null);
    }

    public void Save(String path)
    {
        synchronized(sync)
        {
            if (path == null) {
                path = this.path;
            }

            String x;
            try {
                FileWriter fileWriter = new FileWriter(path);
                fileWriter.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Measurements));
                fileWriter.close();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void Insert(int position, Measurement measurement)
    {
        synchronized(sync)
        {
            Measurements.add(position, measurement);
        }
    }

    public ArrayList<Measurement> GetContentAndEmptyIt()
    {
        synchronized(sync)
        {
            var content = Measurements;
            Measurements = new ArrayList<Measurement>();
            Save(null);
            return content;
        }
    }
}