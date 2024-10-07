package guissica_soho;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;



public class PrimaryController {
    @FXML
    private TextField textInput;

    // Labels for real-time and forecast data
    @FXML private Label todayTempLabel;
    @FXML private Label townLabel;
    @FXML private Label countryLabel;
    @FXML private ImageView todayIcon;

    // Labels for the forecast (for 7 days of the week)
    @FXML private Label tempLableOne;
    @FXML private Label tempLableTwo;
    @FXML private Label tempLableThree;
    @FXML private Label tempLableFour;
    @FXML private Label tempLableFive;
    @FXML private Label tempLableSix;

    @FXML private Label dayLabelOne;
    @FXML private Label dayLabelTwo;
    @FXML private Label dayLabelThree;
    @FXML private Label dayLabelFour;
    @FXML private Label dayLabelFive;
    @FXML private Label dayLabelSix;

    @FXML private ImageView iconOne;
    @FXML private ImageView iconTwo;
    @FXML private ImageView iconThree;
    @FXML private ImageView iconFour;
    @FXML private ImageView iconFive;
    @FXML private ImageView iconSix;

    @FXML private Button btnInput;
    @FXML private Label timeLabel;
    @FXML private Label timeAmPmLabel;
    @FXML private Label dateLabel;

    private ContextMenu autocompleteMenu = new ContextMenu();
    private ObservableList<String> cityList = FXCollections.observableArrayList();
    private boolean searchPressed = false;
    private boolean running = true;
    private boolean fetchingTime = false;

    private LocalTime locationBaseTime;
    private LocalDate locationBaseDate;

    private int hourOffset = 0;
    private int minuteOffset = 0;

    @FXML
    private void initialize() {
        updateTime();
        startClockThread();
        startWeatherThread();
        btnInput.setOnAction(event -> performSearch());
        getWeatherData("Baguio"); // Initial data fetch with default location

    }
    
    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] words = input.split(" ");
        StringBuilder capitalized = new StringBuilder();
    
        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase())
                            .append(" ");
            }
        }
        
        return capitalized.toString().trim(); // Remove trailing space
    }
    

    private void performSearch() {
        String location = textInput.getText().trim(); // Get input and trim whitespace
        if (location != null && !location.isEmpty()) {
            String formattedLocation = capitalizeWords(location); // Capitalize the location
            getWeatherData(formattedLocation);
            getTimeData(location);
        } else {
            townLabel.setText("---");
        }
    }


    @FXML
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handlebtnInput(); //PARA MAGSEARCH PAG NAGENTER
        }
    }

    private void startClockThread() {
        Thread clockThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(1000); 
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Platform.runLater(this::updateTime);
            }
        });
        clockThread.setDaemon(true); 
        clockThread.start();
    }

    //BINAGO NI MP
    private void updateTime() {
        LocalTime currentTime = LocalTime.now().plusHours(hourOffset).plusMinutes(minuteOffset);
        LocalDate currentDate = LocalDate.now().plusDays(calculateDayDifference());
        
        int hour = currentTime.getHour();
        int minute = currentTime.getMinute();
    
        String period = (hour >= 12) ? "PM" : "AM";
        if (hour > 12) {
            hour -= 12;
        } else if (hour == 0) {
            hour = 12;  
        }
    
        String formattedTime = String.format("%02d:%02d", hour, minute);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
        String formattedDate = currentDate.format(dateFormatter);

        //UPDATE LABEL
        timeLabel.setText(formattedTime);
        timeAmPmLabel.setText(period); 
        dateLabel.setText(formattedDate); 
    }
    
    private int calculateDayDifference() {
        if (locationBaseDate != null) {
            LocalDate systemDate = LocalDate.now();
            return locationBaseDate.getDayOfYear() - systemDate.getDayOfYear();
        }
        return 0;
    }

    @FXML
    private void handlebtnInput() {
        String newLocation = textInput.getText().trim();
        String formattedLocation = capitalizeWords(newLocation); // Capitalize the input
    
        if (!formattedLocation.isEmpty()) {
            fetchingTime = true; // fetch time 
            Task<Void> weatherTask = new Task<Void>() {
                @Override
                protected Void call() {
                    getWeatherData(formattedLocation);
                    return null;
                }
            };
    
            Task<Void> timeTask = new Task<Void>() {
                @Override
                protected Void call() {
                    getTimeData(formattedLocation);
                    return null;
                }
            };
    
            // When weatherTask is complete, update the weather UI
            weatherTask.setOnSucceeded(event -> townLabel.setText(formattedLocation));
    
            // When timeTask is complete, update the time UI
            timeTask.setOnSucceeded(event -> {
                townLabel.setText(formattedLocation);
                fetchingTime = false; // Reset fetching flag
            });
    
            // Start the tasks in separate threads
            new Thread(weatherTask).start();
            new Thread(timeTask).start();
        }
    }
    

    private void startWeatherThread() {
        Thread weatherThread = new Thread(() -> {
            while (running) {
                try {
                    String location = textInput.getText().trim().replace(" ", "_");
                    if (!location.isEmpty()) {
                        getWeatherData(location); // Fetch weather data
                    }
                    Thread.sleep(60000); // Update every 1 minutes (60000 milliseconds)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        weatherThread.setDaemon(true); 
        weatherThread.start();
    }


// Method to get weather data in a background thread
    private void getWeatherData(String location) {
        // API key and location
        String apiKey = "M6BLJlaEZ6onax1XGUj3twdJtM24AQew";

        // Real-time weather URL
        String realtimeWeatherURL = "https://api.tomorrow.io/v4/weather/realtime?location=" + location + "&apikey=" + apiKey;
            
        // Forecast weather URL
        String forecastWeatherURL = "https://api.tomorrow.io/v4/weather/forecast?location=" + location + "&apikey=" + apiKey;

        // Fetch real-time weather
        try {
            URL url = new URL(realtimeWeatherURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                JsonObject jsonObject = JsonParser.parseString(content.toString()).getAsJsonObject();
                JsonObject dataObject = jsonObject.getAsJsonObject("data");
                JsonObject valuesObject = dataObject.getAsJsonObject("values");
                int temperature = valuesObject.get("temperature").getAsInt();

                System.out.println("Temperature in " + location + ": " + temperature + "°C");

                // Set weather description based on temperature
                String weatherDescription;
                String imagePath;
                if (temperature >= 30) {
                    weatherDescription = "Hot and sunny";
                    imagePath = "/guissica_soho/sunIcon.png";
                } else if (temperature >= 20) {
                    weatherDescription = "Warm and pleasant";
                    imagePath = "/guissica_soho/partlyCloudyIcon.png";
                } else if (temperature >= 10) {
                    weatherDescription = "Mild to cool";
                    imagePath = "/guissica_soho/cloudy.png";
                } else if (temperature >= 0) {
                    weatherDescription = "Chilly, possible frost";
                    imagePath = "/guissica_soho/frostorrain.png";
                } else if (temperature >= -10) {
                    weatherDescription = "Cold and wintry";
                    imagePath = "/guissica_soho/heavysnoworrain.png";
                } else {
                    weatherDescription = "Very cold, severe winter conditions";
                    imagePath = "/guissica_soho/severesnow.png";
                }

                Platform.runLater(() -> {
                    todayTempLabel.setText(String.valueOf(temperature) + "°C");
                    townLabel.setText(capitalizeWords(location)); // Capitalize the town name
                    countryLabel.setText(weatherDescription);
                    setImage(todayIcon, imagePath);
                });
                

            } else {
                System.out.println("Error: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fetch weather forecast
        try {
            URL url = new URL(forecastWeatherURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                JsonObject jsonObject = JsonParser.parseString(content.toString()).getAsJsonObject();
                JsonElement dailyArray = jsonObject.getAsJsonObject("timelines").getAsJsonArray("daily");

                // Iterate through each day to display the forecast
                for (int i = 0; i < dailyArray.getAsJsonArray().size(); i++) {
                    JsonObject dayData = dailyArray.getAsJsonArray().get(i).getAsJsonObject();
                    JsonObject values = dayData.getAsJsonObject("values");

                    // Extract relevant forecast data
                    int temperatureMin = values.get("temperatureMin").getAsInt();
                    int temperatureMax = values.get("temperatureMax").getAsInt();
                    String time = dayData.get("time").getAsString(); // The date for each day

                    // Parse the date string to LocalDate
                    LocalDate date = LocalDate.parse(time, DateTimeFormatter.ISO_DATE_TIME);

                    // Get the day of the week (e.g., "Wed", "Thu")
                    String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

                    // Calculate the average temperature for the day
                    int averageTemperature = (temperatureMin + temperatureMax) / 2;

                    // Print the forecast data and average temperature for verification
                    System.out.println("Day: " + dayOfWeek);
                    System.out.println("Min Temp: " + temperatureMin + "°C");
                    System.out.println("Max Temp: " + temperatureMax + "°C");
                    System.out.println("Average Temp: " + averageTemperature + "°C");

                    // Set weather description based on temperature
                    String imagePath;
                    if (averageTemperature >= 30) {
                        imagePath = "/guissica_soho/sunIcon.png";
                    } else if (averageTemperature >= 20) {
                        imagePath = "/guissica_soho/partlyCloudyIcon.png";
                    } else if (averageTemperature >= 10) {
                        imagePath = "/guissica_soho/cloudy.png";
                    } else if (averageTemperature >= 0) {
                        imagePath = "/guissica_soho/frostorrain.png";
                    } else if (averageTemperature >= -10) {
                        imagePath = "/guissica_soho/heavysnoworrain.png";
                    } else {
                        imagePath = "/guissica_soho/severesnow.png";
                    }

                    int dayIndex = i + 1;
                    Platform.runLater(() -> {
                        switch (dayIndex) {
                            case 1:
                                dayLabelOne.setText(dayOfWeek);
                                setImage(iconOne, imagePath);
                                tempLableOne.setText(String.valueOf(averageTemperature) + "°C");
                                break;
                            case 2:
                                dayLabelTwo.setText(dayOfWeek);
                                setImage(iconTwo, imagePath);
                                tempLableTwo.setText(String.valueOf(averageTemperature) + "°C");
                                break;
                            case 3:
                                dayLabelThree.setText(dayOfWeek);
                                setImage(iconThree, imagePath);
                                tempLableThree.setText(String.valueOf(averageTemperature) + "°C");
                                break;
                            case 4:
                                dayLabelFour.setText(dayOfWeek);
                                setImage(iconFour, imagePath);
                                tempLableFour.setText(String.valueOf(averageTemperature) + "°C");
                                break;
                            case 5:
                                dayLabelFive.setText(dayOfWeek);
                                setImage(iconFive, imagePath);
                                tempLableFive.setText(String.valueOf(averageTemperature) + "°C");
                                break;
                            case 6:
                                dayLabelSix.setText(dayOfWeek);
                                setImage(iconSix, imagePath);
                                tempLableSix.setText(String.valueOf(averageTemperature) + "°C");
                                break;
                        }
                    });

                    // Stop after 6 days
                    if (i == 6) break;
                }
            } else {
                    System.out.println("Error: " + responseCode);
            }
        } catch (Exception e) {
                e.printStackTrace();
        }
    }
    
    private void setImage(ImageView imageView, String imagePath) {
        if (imagePath != null) {
            try {
                Image weatherImage = new Image(getClass().getResourceAsStream(imagePath));
                imageView.setImage(weatherImage);
            } catch (Exception e) {
                System.out.println("Error loading image from path: " + imagePath);
                e.printStackTrace();
            }
        } else {
            System.out.println("Image path is null.");
        }
    }

    // NILIPAT NI MP. PARA MAKUHA TIME SA API TO
    private void getTimeData(String location) {
        String apiKey2 = "780f7aed999f43269890e7c62328c164";
        String timeURL = "https://api.ipgeolocation.io/timezone?apiKey=" + apiKey2 + "&location=" + location;

        Task<Void> timeTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    URL url = new URL(timeURL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder content = new StringBuilder();
                        String inputLine;

                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();

                        JsonObject jsonObject = JsonParser.parseString(content.toString()).getAsJsonObject();
                        String currentTime = jsonObject.get("time_24").getAsString();  // Time in HH:mm:ss
                        String currentDate = jsonObject.get("date").getAsString();    // Date in YYYY-MM-DD

                        // Parse the time from the API
                        String[] timeParts = currentTime.split(":");
                        int locationHour = Integer.parseInt(timeParts[0]);
                        int locationMinute = Integer.parseInt(timeParts[1]);

                        // Parse the date from the API
                        locationBaseDate = LocalDate.parse(currentDate);

                        // Store the base location time
                        locationBaseTime = LocalTime.of(locationHour, locationMinute);

                        // Calculate time difference between system local time and location time
                        LocalTime systemLocalTime = LocalTime.now();
                        hourOffset = locationBaseTime.getHour() - systemLocalTime.getHour();
                        minuteOffset = locationBaseTime.getMinute() - systemLocalTime.getMinute();

                        // Update the labels on the UI thread
                        Platform.runLater(() -> {
                            townLabel.setText(capitalizeWords(location));
                            dateLabel.setText(locationBaseDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        new Thread(timeTask).start();
    }

}
