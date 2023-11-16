package util;

import websockets.CustomWebSocketClient;

public interface ChatMethods {
    boolean sendMessage(CustomWebSocketClient sender, String text);

}
