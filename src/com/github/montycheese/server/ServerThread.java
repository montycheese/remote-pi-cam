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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;

/**
 * 
 */
public class ServerThread implements Runnable {
	
	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader br;
	private InputStream in;
	private byte INVALID_CMD = 0;
	private byte SEND_PICTURE = 1;
	private byte STREAM_VIDEO = 2;
	private final String IMAGE_TYPE = "PNG";
	private final String IMAGE_PATH = "media/photo/";
	private final int BUF_SIZE = 16*1024;
	
	/**
	 * 
	 * @param clientSocket
	 * @param serverSocket
	 */
	public ServerThread(Socket clientSocket, ServerSocket serverSocket){
		this.clientSocket = clientSocket;
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
	 * client and server until the client enters the quit command. The thread dies soon there after. 
	 */
	@Override
	public void run(){
		//do tasks until no more, then let thread die
		System.out.println("Processing client request!");
		
		try {
			//checks commands inputed by user and parses it
			String command = "";
			while((command = this.br.readLine()) != null){
				if(command.equalsIgnoreCase("quit")) {
					this.out.println("Goodbye, Exiting\n");
					break;
				}
				System.out.println("debug");
				//parse client's request
				byte commandCode = this.parse(command);
				
				switch(commandCode){
				case 1:
					this.takeAndSendPhoto();
					//response = Byte.toString(commandCode);
					break;
				case 2:
					//todo video stream
					break;
				case 0:
					//send client that there was an error
				default:
					this.sendMessage("Error");
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
			if(this.clientSocket != null)
				try {
					this.clientSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			
			
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
	
	/**
	 * Takes a photo from the webcam and sends it to the requesting client.
	 */
	private void takeAndSendPhoto(){
		String fileName = null;
		try {
			fileName = this.takePicture();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error taking image.");
		}
		//if image success / video success notify client to prepare for transfer
		if(fileName != null){
			//notify client to prepare to receive image by sending file type
			this.sendMessage(this.IMAGE_TYPE);
		}
		else{
			this.sendMessage("error");
		}
		if(this.checkClientReady() == true){
			try {
				//send picture
				this.sendPicture(fileName);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error sending image");
			}
		}
		else{
			System.out.println("Client not able to start receiving image.\n" + 
								"Process terminating.");
			
		}
		
	}
    
	/**
	 * Calls the sarxos webcam library to take a photo with the webcam and saves it to file.
	 * @return String imgPath the path to the image that is stored on the server.
	 * @throws IOException
	 */
    private String takePicture() throws IOException{
    	// get default webcam and open it
		Webcam webcam = Webcam.getDefault();
		webcam.open();

		// get image
		BufferedImage img = webcam.getImage();
		String imgPath = null;

		// save image to PNG file
		//e.g. media/photos/img000000.png
		imgPath = String.format("%simg%s.%s",
				this.IMAGE_PATH,
				this.generateId(),
				this.IMAGE_TYPE.toLowerCase()
				);
		//imgPath = this.IMAGE_PATH +  "img" + this.generateId() +   this.IMAGE_TYPE.toLowerCase();
		ImageIO.write(img, this.IMAGE_TYPE, new File(imgPath));
	
		return imgPath;
    			
    }
    
    /**
     * Sends the image that was just taken on the webcam to the client whom requested it.
     * @param fileName
     * @throws IOException
     */
    private void sendPicture(String fileName) throws IOException{
    	File img = null;
	
		img = new File(fileName);
		if (!img.exists()){
			throw new FileNotFoundException();
		}
		
		//send file
		byte[] buffer = new byte[this.BUF_SIZE];
		int count;
		FileInputStream fileInputStream = new FileInputStream(img);
		OutputStream fileOut = this.clientSocket.getOutputStream();
		//send file
		while((count = fileInputStream.read(buffer)) > 0){
			fileOut.write(buffer, 0, count);
		}
		fileInputStream.close();
		fileOut.flush();
		this.clientSocket.shutdownOutput();

		System.out.println("Sending of file: " + fileName + " at " + this.getCurrentDateTime() + " complete.");
		//TODO possibly remove image from server once sent to save space.
		//this.delete(fileName);
    }
    
    private String getCurrentDateTime(){
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		//get current date time with Date()
		Date date = new Date();
		return dateFormat.format(date);
    }
    
    @SuppressWarnings("unused")
	private String streamVideo(){
    	//TODO
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
			if (response.equalsIgnoreCase("ready")){
				return true;
			}
    	}
    	catch(IOException e){
    		System.out.println(response);
    	}
    	return false;
    	
    }
  
    /**
	 * 
	 * Flag to show that a file is deleted. Checks to see if file is in existence
	 * and if so, deletes files. Returns true is file is successfully gone.
	*/
	private boolean delete(String filename){
		File file = new File(filename);
		if (file.exists()){
			file.delete();
		}
		return !file.exists();
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