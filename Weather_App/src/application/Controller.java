package application;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Controller {
    
    @FXML
    private Text temperature;
    @FXML
    private Text humidity;
    @FXML
    private Text wind;
    @FXML
    private Text clText;
    @FXML
    private Text emoji;
    @FXML
    private TextField locationTxt;

    private String locationSvd = ""; 

    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            // Attempt to create connection
            URL url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String readApiResponse(HttpURLConnection apiConnection) {
        try {
            StringBuilder resultJsonBuilder = new StringBuilder();
            Scanner scanner = new Scanner(apiConnection.getInputStream());
            
            while (scanner.hasNext()) {
                resultJsonBuilder.append(scanner.nextLine());
            }
            
            scanner.close();
            
            return resultJsonBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getLocationData(String locationSvd) {
        locationSvd = locationSvd.replaceAll(" ", "+");

        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + locationSvd + "&count=1&language=en&format=json";
        
        try {
            HttpURLConnection apiConnection = fetchApiResponse(urlString);
            
            if (apiConnection.getResponseCode() != 200) {
                showError("Could not connect to the location API");
                return null;
            }
            
            String jSONResponseString = readApiResponse(apiConnection);
            
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObject = (JSONObject) parser.parse(jSONResponseString);
            
            JSONArray locationDatArray = (JSONArray) resultJsonObject.get("results");
            return (JSONObject) locationDatArray.get(0);  
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error fetching location data");
        }
        return null;
    }

    private static void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void displayWeatherData(double latitude, double longitude) {
        try {
            String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m";
            HttpURLConnection apiConnection = fetchApiResponse(urlString);
            
            if (apiConnection.getResponseCode() != 200) {
                showError("Could not connect to the weather API");
                return;
            }
            
            String jsonresponseString = readApiResponse(apiConnection);
            
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonresponseString);
            JSONObject currentWeatherJsonObject = (JSONObject) jsonObject.get("current");
            
            temperature.setText(currentWeatherJsonObject.get("temperature_2m").toString() + "°");
            humidity.setText(currentWeatherJsonObject.get("relative_humidity_2m").toString() + "%");
            wind.setText(currentWeatherJsonObject.get("wind_speed_10m").toString() + " m/s");
            clText.setText(currentWeatherJsonObject.get("weather_code").toString());
            
            String clString = clText.getText();
            String[] weatherStrings = {"Sunny", "Partly cloudy", "Cloudy", "Raining", "Snowing"};
            String pcloudString = "1,2,3";
            String cloudString = "45,48,51,53,55,56,57";
            String rainString = "61,63,65,66,67,80,81,82,95*,96,99*";
            String snowString = "71,73,75,77,85,86";
            
            if (clString.equals("0")) {
                clText.setText(weatherStrings[0]);
                emoji.setText("☀");
            } else if (pcloudString.contains(clString)) {
                clText.setText(weatherStrings[1]);
                emoji.setText("🌤");
            } else if (cloudString.contains(clString)) {
                clText.setText(weatherStrings[2]);
                emoji.setText("☁");
            } else if (rainString.contains(clString)) {
                clText.setText(weatherStrings[3]);
                emoji.setText("🌧");
            } else if (snowString.contains(clString)) {
                clText.setText(weatherStrings[4]);
                emoji.setText("❄");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error fetching weather data");
        }
    }

    @FXML 
    private void handleSave() {
        try {
            locationSvd = locationTxt.getText();
            
            // Get location data
            JSONObject cityLocationDataJsonObject = getLocationData(locationSvd);
            if (cityLocationDataJsonObject != null) {
                double latitude = (double) cityLocationDataJsonObject.get("latitude");
                double longitude = (double) cityLocationDataJsonObject.get("longitude");
                displayWeatherData(latitude, longitude);
            }
            
            locationTxt.clear();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error saving location data");
        }
    }
    
    public String getSavedText() {
        return locationSvd;
    }
}
