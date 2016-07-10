package com.github.montycheese.server;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;

/**
 * 
 */
public class ServerThread implements Runnable {
	
	private Socket clientSocket;
	private File currentWorkingDir;
	private PrintWriter out;
	private BufferedReader br;
	private InputStream in;
	private byte INVALID_CMD = 0;
	private byte SEND_PICTURE = 1;
	private byte STREAM_VIDEO = 2;
	private final String IMAGE_TYPE = "PNG";
	private final String IMAGE_PATH = "";
	private final int TERMINATE_INTERVAL = 1000;
	private final int BUF_SIZE = 16*1024;
	private final int HEADER_OFFSET = 3;
	private final byte[] VALID = new byte[]{1,1,1}, INVALID = new byte[]{0,0,0};
	
	/**
     * @param client socket, server socket, and hashmap that contains the IDs to terminate processes get and put
     * @return error connecting if server is already running or initializes client socket, hashmap for IDs, and current directoryy
     * */
	public ServerThread(Socket clientSocket, ServerSocket serverSocket){
		this.clientSocket = clientSocket;
		this.currentWorkingDir = new File(System.getProperty("user.dir"));
		//out is the message buffer to return to the client
		try {
			this.out = new PrintWriter(clientSocket.getOutputStream(), true);
			//br is the incoming message buffer from the client to be read by the server
			this.br = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			this.in = this.clientSocket.getInputStream();
		} catch (IOException e) {
		    e.printStackTrace();
			System.out.println("Error connecting to socket.");
		}

		
		
	}
	
	/**
	 * Automatically called when the thread is created. This method handles the communication between
	 * client and server until the client enters the quit command. The thread dies soonthereafter. 
	 */
	@Override
	public void run(){
		//do tasks until no more, then let thread die
		System.out.println("Running thread!");
		
		try {
			//checks commands inputted by user and parses it
			String command = "", response = null;
			while((command = this.br.readLine()) != null){
				if(command.equalsIgnoreCase("quit")) {
					this.out.println("Goodbye, Exiting\n");
					break;
				}
				
				//parse client's request
				byte commandCode = this.parse(command);
				String msg = null;
				switch(commandCode){
				case 1:
					this.takeAndSendPhoto();
					//response = Byte.toString(commandCode);
					break;
				case 2:
					//todo
					break;
				case 0:
					//send client that there was an error
				default:
					//response = Byte.toString(commandCode);
					break;
				}
				
				//return server's response
				//if(response != null)
					//this.out.println(response + "\n");
			}
			//close reader and writer
			this.out.close();
			this.br.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
			    //close client conn.
			    this.clientSocket.close();
			} catch (IOException e) {
				System.out.println("Error closing client socket");
			}
			
		}
		
	}
	
	
	private void takeAndSendPhoto(){
		String fileName = this.takePicture();
		//if image success / video success notify client to prepare for transfer
		if(fileName != null){
			//notify client to prepare to receive image
			this.sendMessage("Ready");
		}
		if(this.checkClientReady() == true){
			this.sendPicture(fileName);
		}
		else{
			System.out.println("Client not able to start receiving image.\n" + 
								"Process terminating.");
			
		}
		
	}
	
	
	/**
	 * Parses the client's command and returns the response string
	 * @param cmd String the command to be parsed.
	 * @return response String the response to return to the client.
	 */
    private byte parse(String cmd){
		//break command into an array of each word
		// e.g. mkdir files -> {"mkdir", "files"}
		String[] tokens = cmd.split(" ");
		
		if (tokens.length == 1){
			switch(cmd){
				case "picture":
					return this.SEND_PICTURE;
				case "video":
					return this.STREAM_VIDEO;
			}
		}
		return this.INVALID_CMD;
	}
    
    private String takePicture(){
    	// get default webcam and open it
		Webcam webcam = Webcam.getDefault();
		webcam.open();

		// get image
		BufferedImage img = webcam.getImage();
		String imgPath = null;

		// save image to PNG file
		try {
			//e.g. media/photos/img000000.png
			imgPath = String.format("%simg%s%s",
					this.IMAGE_PATH,
					this.generateId(),
					this.IMAGE_TYPE.toLowerCase()
					);
			//imgPath = this.IMAGE_PATH +  "img" + this.generateId() +   this.IMAGE_TYPE.toLowerCase();
			ImageIO.write(img, this.IMAGE_TYPE, new File(imgPath));
		} catch (IOException e) {
			e.printStackTrace();
			imgPath = null;
		}
		return imgPath;
    			
    }
    private String sendPicture(String fileName){
    	return null;
    }
    
    //todo determine return type
    private String streamVideo(){
    	return null;
    }
    
    private void sendMessage(String msg){
    	this.out.println(msg);
    	this.out.flush();
    }
    private boolean checkClientReady() {
    	String response = null;
    	try{
    		response = this.br.readLine();
			if (response.equalsIgnoreCase("Ready")){
				return true;
			}
    	}
    	catch(IOException e){
    		System.out.println(response);
    	}
    	return false;
    	
    }
  

	/**
     * method to generate a 6-digit ID
     * @param none
     * @return 6-digit ID between 100000 and 999999
     * */
	private String generateId(){
		//max 6 digit number
		int max = 999999;
		//min 6 digit number
		int min = 100000;
		//adds min to random generated number to ensure 6 digits
		String id = Integer.toString( (int) Math.round(Math.random() * (max - min + 1) + min));
		
		return id;
	}
	
	
} 