import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Server {
    public static void main(String[] args) {
        // File path for JSON data
        //String filepath = "C:\\Users\\lenny\\Desktop\\Grit_projekt\\web_services\\SlutProjekt_V4\\Server\\src\\data.json";
        //C:\Users\lenny\Desktop\Grit_projekt\web_services\SlutProjekt_V4
        String filepath = ".\\src\\data.json";

        ServerSocket serverSocket;

        // Create a new server socket and bind it to port 8300
        try {
            serverSocket = new ServerSocket(8300);
        } catch (Exception e) {
            System.out.println(e);
            return;
        }

        // Continuously listen for incoming connections
        while (true) {
            try {
                // Accept a new incoming connection and create a socket
                Socket socket = serverSocket.accept();

                // Set up input and output streams for the socket
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

                // Flag to control the keep-alive behavior of the server
                boolean keepAlive = true;

                // Keep processing requests while the connection is alive
                while (keepAlive) {
                    // Read the request line
                    String requestLine = bufferedReader.readLine();

                    // If the request line is null, break the loop
                    if (requestLine == null) {
                        break;
                    }

                    // Initialize variables to store request headers
                    int contentLength = -1;
                    String message;
                    String connectionHeader = "";

                    // Read and process request headers
                    while (true) {
                        message = bufferedReader.readLine();
                        if (message == null || message.trim().isEmpty()) {
                            break;
                        }

                        // Get the content length from the request headers
                        if (message.startsWith("Content-Length:")) {
                            contentLength = Integer.parseInt(message.substring("Content-Length:".length()).trim());
                        }

                        // Get the connection header from the request headers
                        if (message.startsWith("Connection:")) {
                            connectionHeader = message.substring("Connection:".length()).trim();
                        }
                    }

                    // Handle GET requests
                    if (requestLine.startsWith("GET")) {
                        JSONParser parser = new JSONParser();
                        JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(filepath));

                        // Filter pets by species if requested
                        if (requestLine.contains("/pets?species=")) {
                            String species = requestLine.split("species=")[1].split(" ")[0];
                            JSONArray filteredArray = new JSONArray();
                            for (Object obj : jsonArray) {
                                JSONObject jsonObject = (JSONObject) obj;
                                if (species.equalsIgnoreCase((String) jsonObject.get("species"))) {
                                    filteredArray.add(jsonObject);
                                }
                            }
                            jsonArray = filteredArray;
                        }

                        // Prepare and send the response
                        String response = jsonArray.toJSONString();
                        bufferedWriter.write("HTTP/1.1 200 OK\r\n");
                        bufferedWriter.write("Content-Type: application/json\r\n");
                        bufferedWriter.write("Content-Length: " + response.length() + "\r\n");
                        bufferedWriter.write("Connection: keep-alive\r\n");
                        bufferedWriter.write("\r\n");
                        bufferedWriter.write(response);
                    }

                    // Handle unsupported requests
                    else {
                        bufferedWriter.write("HTTP/1.1 400 Bad Request\r\n");
                        bufferedWriter.write("Content-Length: 0\r\n");
                        bufferedWriter.write("Connection: keep-alive\r\n");
                        bufferedWriter.write("\r\n");
                    }

                    // Flush the output buffer
                    bufferedWriter.flush();

                    // Check the connection header and update the keep-alive flag accordingly
                    if (!"keep-alive".equalsIgnoreCase(connectionHeader)) {
                        keepAlive = false;
                    }
                }

                // Close all resources
                socket.close();
                inputStreamReader.close();
                outputStreamWriter.close();
                bufferedReader.close();
                bufferedWriter.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}

