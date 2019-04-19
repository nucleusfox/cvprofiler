/*********************************************************************************************
 *
 *  Simple helper to get CV details in form of JSON. Requires credentials
 *  to SF where details are stored and connected app configured with usage of oAuth 2.0.
 *
 *  @author nucleusfox
 *
 *********************************************************************************************/

package com.nucleusfox.idle.cvprofiler;

import com.google.gson.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Scanner;

public class CVProfile {
    private static String token;
    private static String instanceURL;
    private static String tokenType;
    private static String username;
    private static String password;
    private static String clientId;
    private static String clientSecret;

    private static void loadCredentials() {
        // Load properties
        try (InputStream input = CVProfile.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();

            if (input != null) {

                prop.load(input);

                clientId = prop.getProperty("sf.client_id");
                clientSecret = prop.getProperty("sf.client_secret");
                username = prop.getProperty("sf.username");
                password = prop.getProperty("sf.password");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Input credentials
        if (username == null || username.isEmpty()) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Properties file was not found or credentials were not set up. Please provide client id, client secret, username and password for Salesforce instance.");
            System.out.print("Client id: ");
            clientId = sc.nextLine();
            System.out.print("Client secret: ");
            clientSecret = sc.nextLine();
            System.out.print("Username: ");
            username = sc.nextLine();
            System.out.print("Password: ");
            password = sc.nextLine();
//            System.out.print("Do you want properties file to be generated? Y/n: ");
//            String save = sc.nextLine();
//            if (save == null || save.isEmpty() || save.equals("Y")) generateProperties();
        }
    }

//    private static void generateProperties() {
//
//    }


    private static boolean authorize() {
        loadCredentials();
//        Console console = System.console();
//        String username = console.readLine("Username: ");
//        String password = new String(console.readPassword("Password: "));
//        System.out.println(password);

        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) new URL("https://login.salesforce.com/services/oauth2/token"
                    + "?grant_type=password"
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret
                    + "&username=" + username
                    + "&password=" + password
            ).openConnection();


            connection.setRequestMethod("POST");
            String contentEncoding = "UTF-8";
            Charset cs = Charset.forName(contentEncoding);

            StringBuilder sb = new StringBuilder();
            String line;

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), cs))) {
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(sb.toString());
//        String prettyJsonString = gson.toJson(je);


            JsonObject jobject = je.getAsJsonObject();
            token = jobject.get("access_token").getAsString();
            instanceURL = jobject.get("instance_url").getAsString();
            tokenType = jobject.get("token_type").getAsString();

        } catch (IOException e) {
            System.out.println("Unable to authorize in Salesforce. Try checking credentials provided.");
            return false;
        }

        return true;
    }

    private static String connect(String query) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(instanceURL + "/services/apexrest/whois?query" + query).openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", tokenType + " " + token);

            String contentEncoding = "UTF-8";
            Charset cs = Charset.forName(contentEncoding);

            StringBuilder sb = new StringBuilder();
            String line;

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), cs))) {
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(sb.toString());

            return gson.toJson(je);

        } catch (IOException e) {
            return "Don't know anything about \'" + query + "\'. Try checking spelling or someone whom I might know.";
        }
    }

    public static void main(String[] args) {
        System.out.println("Hi! Do you want to get who is Jane Ivanova? Y/y: ");
        String query = new Scanner(System.in).nextLine();
        query = "Jane+Ivanova";
        if (authorize())
            System.out.println(connect(query));

    }
}
