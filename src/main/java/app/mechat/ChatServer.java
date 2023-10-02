package app.mechat;/*
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
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class ChatServer extends WebSocketServer {

  private Map<WebSocket, String> connectedUsersMap;

  public ChatServer(int port) throws UnknownHostException {
    super(new InetSocketAddress(port));
  }

  public ChatServer(InetSocketAddress address) {
    super(address);
  }

  public ChatServer(int port, Draft_6455 draft) {
    super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
    connectedUsersMap = new ConcurrentHashMap<>();
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    // Extract the session token from the WebSocket client query
    String sessionToken = extractSessionFromQuery(handshake.getResourceDescriptor());

    // Look up the associated username from the session token (your database logic here)
    String username = getUsernameForSession(sessionToken);

    if (username != null && !connectedUsersMap.containsValue(username) && !connectedUsersMap.containsValue(conn)) {
      // Authentication is successful, proceed with WebSocket communication
      connectedUsersMap.put(conn, username);
      onMessage(conn,"SYSTEM//CONNECT//" + username);
      System.out.println(username + " has entered the room!");
    } else {
      // Invalid session token, reject the WebSocket connection
      System.out.println("ERROR!");
      conn.closeConnection(401, "Unauthorized");
    }
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    // Get the associated username
    String username = connectedUsersMap.get(conn);

    if (username != null) {
      connectedUsersMap.remove(conn); // Remove the connection from the map
      Database.removeUserSession(username);
      onMessage(conn,"SYSTEM//DISCONNECT//" + username);
      System.out.println(username + " has left the room!");
    }
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    broadcast(message);
    //System.out.println(connectedUsersMap.get(conn) + ": " + message);
  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer message) {
    broadcast(message.array());
    //System.out.println(conn + ": " + message);
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

  private String getUsernameForSession(String sessionToken) {
    // Return the username or null if not found
    return Database.getUsernameBySession(sessionToken); // returns null if not found
  }
  // // // // // // // // // // // // // // // // // // // // // // //

  public static void main(String[] args) throws InterruptedException, IOException {
    int port = 8887; // 843 flash policy port
    try {
      port = Integer.parseInt(args[0]);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ChatServer s = new ChatServer(port, new Draft_6455());
    s.start();
    System.out.println("ChatServer started on port: " + s.getPort());

    BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      String in = sysin.readLine();
      s.broadcast(in);
      if (in.equals("exit")) {
        s.stop(1000);
        break;
      }
    }
  }



}
