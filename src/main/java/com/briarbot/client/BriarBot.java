package com.briarbot.client;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import java.util.Map;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.apache.commons.text.StringEscapeUtils;

import org.glassfish.tyrus.client.ClientManager;

import org.json.simple.JSONObject;
import org.json.simple.parser.*; 


@ClientEndpoint
public class BriarBot {

    private static CountDownLatch latch;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private String bearerToken;
    private final int allowedContactId = 1;
    private final String botKeyword = "bb";

    @OnOpen
    public void onOpen(Session session) throws IOException {
        logger.info("Connected ... " + session.getId());
        try {
        	String tokenFilePath = System.getProperty("user.home") + "/.briar/auth_token";
        	File file = new File(tokenFilePath); 
        	BufferedReader br = new BufferedReader(new FileReader(file));
        	String authToken = br.readLine();
        	br.close();
        	if (authToken == null || authToken.equals("")) {
        		logger.warning("Briar authentication token missing.  You should have a file " + tokenFilePath + " containing your Briar authentication token.");
        		latch.countDown();
        		return;
        	}
        	bearerToken = "Bearer " + authToken;
        	session.getBasicRemote().sendText(authToken);
        } catch (IOException e) {
        	logger.warning(e.toString());
            throw new RuntimeException(e);
        }
    }

    @OnMessage
    public void onMessage(String json, Session session) {
        try {
            logger.info("message: " + json);

    		JSONObject jo = (JSONObject) (new JSONParser().parse(json));
    		Map data = ((Map)jo.get("data"));
    		
    		// Restrict by contacts allowed
            Long contactId = (Long) data.get("contactId");
            if (contactId != allowedContactId) { // TODO make this configurable
            	logger.info("Bot use not allowed for contact ID " + contactId);
            	return;
            }
            
            // Only execute bot messages
            String messageText = (String) data.get("text");
            String[] splitMessage = messageText.split(" ", 3);
            if (splitMessage.length == 0 || !splitMessage[0].equalsIgnoreCase(botKeyword)) {
            //	logger.info("'" + splitMessage[0] + "'");
            	return;
            }
            
            String replyText = "";
            if(splitMessage.length == 1 || splitMessage[1].equals("")) {
            	replyText = "I'm here.  If you need help type '" + botKeyword + " help'.";
            }
            else {
            	String messageRemainder = "";
            	if(splitMessage.length == 3) {
            		messageRemainder = splitMessage[2];
            	}
	            switch(splitMessage[1].toLowerCase()) {
	            	case "echo":
	            		replyText = "You said... " + messageRemainder;
	            		break;
	            	case "help":
	            		replyText = "To do";
	            		break;
	            	case "run":
	            		boolean isWindows = System.getProperty("os.name")
	            		  .toLowerCase().startsWith("windows");
	            		ProcessBuilder builder = new ProcessBuilder();
	            		if (isWindows) {
	            		    builder.command("cmd.exe", "/c", "dir");
	            		} else {
	            		    builder.command("sh", "-c", messageRemainder);
	            		}
	            		builder.directory(new File(System.getProperty("user.home") + "/.briar/bot_scripts"));
	            		Process process = builder.start();
	            		BufferedReader br=new BufferedReader(new InputStreamReader(process.getInputStream()));
	            	    String line;
	            	    while((line=br.readLine())!=null){
	            	    	replyText += line + "\n";
	            	    }
	            	    int exitValue = process.waitFor();
	            	    logger.info("\n\nExit Value is " + exitValue);
	            	    if (exitValue != 0) {
	            	    	replyText = "Warning, exit value=" + exitValue + " " + replyText;
	            	    }
	            		break;
	            	default:
	            		return;
	            }
            
            }
            
            String replyJson = "{\"text\": \"" + StringEscapeUtils.escapeJava(replyText) + "\"}";
            
    		URL url = new URL("http://127.0.0.1:7000/v1/messages/1");
    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    		conn.setDoOutput(true);
    		conn.setRequestMethod("POST");
    		conn.setRequestProperty("Content-Type", "application/json");
    		conn.setRequestProperty("Authorization", bearerToken);
    		OutputStream os = conn.getOutputStream();
    		os.write(replyJson.getBytes());
    		os.flush();
    		os.close();

    		if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
    			throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode() + ", reply: "+replyJson);
    		}
    		
    		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    		String output;
    		logger.info("Output from Server ....");
    		while ((output = br.readLine()) != null) {
    			logger.info(output);
    		}
    		br.close();

    		conn.disconnect();

        } catch (Exception e) {
        	logger.warning(e.toString());
            throw new RuntimeException(e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info(String.format("Session %s close because of %s", session.getId(), closeReason));
        latch.countDown();
    }

    public static void main(String[] args) {
        latch = new CountDownLatch(1);

        ClientManager client = ClientManager.createClient();
        try {
            client.connectToServer(BriarBot.class, new URI("ws://localhost:7000/v1/ws"));
            latch.await();

        } catch ( Exception e) {
            throw new RuntimeException(e);
        }
    }
}
