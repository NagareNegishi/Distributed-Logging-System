package io.github.nagare.logging.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.net.ConnectException;
import java.io.IOException;


/**
 * Command-line client for downloading log statistics from the server.
 * Usage:
 *   java -cp <classpath> io.github.nagare.logging.client.Client <type> <fileName>
 * <type> must be "csv" or "excel". The client connects to
 * <a href="http://localhost:8080/logstore/stats/">...</a><type>
 * and writes the response to the specified file.
 * CSV responses are text, Excel responses are binary (.xlsx).
 * Exits with error messages if arguments are invalid, the server is unavailable, or the response status is not 200.
 */
public class Client {

    /**
     * Main entry point for the client program.
     * @param args command line arguments: <type> <fileName>
     */
    public static void main(String[] args) {
        // Validate arguments
        if (args.length != 2) {
            System.err.println("Usage: java -cp <classpath> io.github.nagare.logging.client.Client <type> <fileName>");
            System.exit(1);
        }
        String type = args[0];
        String fileName = args[1];
        if (!type.equals("csv") && !type.equals("excel")) {
            System.err.println("Error: type must be 'csv' or 'excel'");
            System.exit(1);
        }
        String url = "http://localhost:8080/logstore/stats/" + type;

        // Try to connect to server
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .build();
        try {
            // excel is binary content
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                System.err.println("Error: Server returned status " + response.statusCode());
                System.exit(1);
            }
            Files.write(Paths.get(fileName), response.body());
        } catch (ConnectException e) {
            System.err.println("Error: Server is not available at http://localhost:8080/logstore/");
        } catch (IOException e) {
            // Could be network error or file write error
            System.err.println("Error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Error: Request interrupted");
        }
    }
}
