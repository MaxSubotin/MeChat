package websockets;/*
 * Copyright (c) 2010-2020 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import util.Message;
import database.Database;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class CustomWebSocketServer extends WebSocketServer {

  private Map<WebSocket, String> connectedUsersMap;
  private Map<String, WebSocket> reverseLookupMap;
  private Gson gson;

  public CustomWebSocketServer(int port, Draft_6455 draft) {
    super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
    connectedUsersMap = new ConcurrentHashMap<>();
    reverseLookupMap = new ConcurrentHashMap<>();
    gson = new Gson();
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    // Extract the session token from the WebSocket client query
    String sessionToken = extractSessionFromQuery(handshake.getResourceDescriptor());

    // Look up the associated username from the session token (your database logic here)
    String userId = getUserIdFormSession(sessionToken);

    if (userId != null && !connectedUsersMap.containsValue(userId)) {
      // Authentication is successful, proceed with WebSocket communication
      connectedUsersMap.put(conn, userId);
      reverseLookupMap.put(userId, conn);

      // Create a system message
      Message sysMessage = new Message("CONNECT//" + userId, userId, userId, new Timestamp(System.currentTimeMillis()).toString() ,"-1");
      sysMessage.setIsSystemMessage(true);

      // Serialize the message object to JSON
      String jsonMessage = gson.toJson(sysMessage);

      // Send the json system message
      onMessage(conn,jsonMessage);
      System.out.println(userId + " has entered the room!");
    } else {
      // Invalid session token, reject the WebSocket connection
      System.out.println("ERROR!");
      conn.closeConnection(401, "Unauthorized");
    }
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    // Get the associated username
    String userId = connectedUsersMap.get(conn);

    if (userId != null) {
      // Create a system message
      Message sysMessage = new Message("DISCONNECT//" + userId, userId, userId, new Timestamp(System.currentTimeMillis()).toString() ,"-1");
      sysMessage.setIsSystemMessage(true);

      // Serialize the message object to JSON
      String jsonMessage = gson.toJson(sysMessage);

      // Send the json system message
      onMessage(conn,jsonMessage);

      // Remove the connection from the map
      connectedUsersMap.remove(conn);
      reverseLookupMap.remove(userId);

      // Remove the user from the connected users table in the database
      Database.removeUserSession(userId);
      System.out.println(userId + " has left the room!");
    }
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    // Deserialize the received JSON string back to your custom object
    Message receivedMessage = gson.fromJson(message, Message.class);

    if (receivedMessage.getIsSystemMessage()) { // it is a system message
      broadcast(message);
    }
    else {
      // Access the receiver and send him the message. If he is disconnected then don't send him a message.
      String receiverUserId = receivedMessage.getReceiver();
      if (connectedUsersMap.containsValue(receiverUserId)) {
        WebSocket Receiver = reverseLookupMap.get(receiverUserId);
        Receiver.send(message);
      }
      // Using send method to show the message on the sender's screen
      conn.send(message);
    }
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    ex.printStackTrace();
    if (conn != null) {
      // some errors like port binding failed may not be assignable to a specific websocket
    }
  }

  @Override
  public void onStart() {
    System.out.println("Server started!");
    setConnectionLostTimeout(0);
    setConnectionLostTimeout(100);
  }


  // Helper Functions  // // // // // // // // // // // // // // // //
  private String extractSessionFromQuery(String query) {
    try {
      URI uri = new URI(query);
      String sessionToken = uri.getQuery(); // Get the query portion
      if (sessionToken != null && sessionToken.startsWith("session=")) {
        return sessionToken.substring("session=".length());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null; // Session token not found or invalid
  }

  private String getUserIdFormSession(String sessionToken) {
    // Return the username or null if not found
    return Database.getUserIdBySession(sessionToken); // returns null if not found
  }
  // // // // // // // // // // // // // // // // // // // // // // //

  public static void main(String[] args) throws InterruptedException, IOException {
    int port = 8888; // 843 flash policy port
    try {
      port = Integer.parseInt(args[0]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    CustomWebSocketServer s = new CustomWebSocketServer(port, new Draft_6455());
    s.start();
    System.out.println("ChatServer started on port: " + s.getPort());

    BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      String in = sysin.readLine();
      s.broadcast(in);
      if (in.equals("q")) {
        s.stop(1000);
        break;
      }
    }
    Database.cleanUserSessionTabel();
  }



}
