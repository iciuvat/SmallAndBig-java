package com.example.bigserver;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class LocationsController {
    Connection connection;

    LocationsController(){
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5243/measurements_db?user=dboperator&password=passw75");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // check sites for new data
        new Thread(new Runnable() {
            private Connection connection;

            public Runnable init(Connection connection) {
                this.connection = connection;
                return this;
            }

            @Override
            public void run() {
                JsonParser springParser = JsonParserFactory.getJsonParser();

                while(true) {
                    var locations = GetLocationsInternal(false);

                    for (var location : locations)
                    {
                        final var restTemplate = new RestTemplate();
                        var requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
                        requestFactory.setReadTimeout(1 * 1000);
                        requestFactory.setConnectTimeout(1 * 1000);

                        String response = restTemplate.getForObject(location.Web + "api/get_saved_values", String.class);
                        var list = JsonParserFactory.getJsonParser().parseList(response);
                        if (! list.isEmpty())
                        {
                            var insertQuery = "INSERT INTO measurements (location_id, timestamp, value, unit) VALUES";
                            for (var element : list) {
                                if (element instanceof Map) {
                                    Map < String, Object > map = (Map < String, Object > ) element;
                                    var measurement = new Measurement(
                                            LocalDateTime.parse((String) map.get("Timestamp")),
                                            (String) map.get("Unit").toString(),
                                            new BigDecimal(map.get("Value").toString()));
                                    insertQuery += " ('" + location.Id + "', '" + measurement.Timestamp + "', '" +measurement.Value + "', '" + measurement.Unit + "'),";
                                }
                            }
                            // remove last comma
                            insertQuery = insertQuery.substring(0, insertQuery.length() - 1);
                            try {
                                var statement = connection.createStatement();
                                var resultSet = statement.execute(insertQuery);
                                int test_111 = 0;
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    try {
                        Thread.sleep(5 * 1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }.init(connection)).start();
    }

    ArrayList<Location> GetLocationsInternal(boolean addMeasurements) {
        var locations = new ArrayList<Location>();
        try {
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery("SELECT * FROM locations");
        while (resultSet.next()) {
            var location = new Location();
            location.Id = resultSet.getInt("id");
            location.Address = resultSet.getString("address");
            location.Web = resultSet.getString("web");
            location.Name = resultSet.getString("name");
            if (addMeasurements) {
                location.measurements = GetMeasurementByLocation(location.Id);
            }
            locations.add(location);
        }

        return locations.size() == 0 ? null : locations;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/api/locations")
    ArrayList<Location> GetLocations() {
        return GetLocationsInternal(false);
    }

    @GetMapping("/api/locations_and_measurements")
    ArrayList<Location> GetLocationsAndMeasurements() {
        return GetLocationsInternal(true);
    }

    @GetMapping("/api/locations/{locationId:int}")
    ArrayList<Measurement> GetMeasurementByLocation(int locationId)
    {
        try {
            var measurements = new ArrayList<Measurement>();
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery("SELECT timestamp, value, unit FROM measurements WHERE location_id = " + locationId + " ORDER BY timestamp DESC LIMIT 100");
            while (resultSet.next()) {
                measurements.add(new Measurement(
                        resultSet.getTimestamp("timestamp").toLocalDateTime(),
                        resultSet.getString("unit"),
                        new BigDecimal(resultSet.getDouble("value"))));
            }
            resultSet.close();
            statement.close();

            return measurements.size() == 0 ? null : measurements;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
