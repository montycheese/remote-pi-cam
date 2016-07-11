package com.github.montycheese.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.FileSystemException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;




/**
 * Creates ClientThread inherited from Java.lang.Thread 
 * Represents one connection
 * Contains (new) socket for termination of processes get and put
 */
public class ClientThread extends Thread {
	
	private Socket socket;
	private String cmd;
    private InputStream in;
    private BufferedReader br;
    private PrintWriter out;
    private final String IMAGE_PATH = "media/photo/";
    private final String VIDEO_PATH = "media/video/";
	private final int BUF_SIZE = 16*1024;
    private final String[] IMAGE_EXTS = new String[]{ "PNG", "JPG", "JPEG", "GIF" };

    
	/**
	 * Creates new ClientThread with param socket and cmd 
	 * Sets socket and connection
	 * Constructor
	 * @param socket
	 * @param cmd
	 */
	public ClientThread(Socket socket, String cmd){
		super();
		this.socket = socket;
		this.cmd = cmd;
		try{
		    this.in = socket.getInputStream();
		    this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		    this.out = new PrintWriter(this.socket.getOutputStream());
		}
		catch(Exception e){
		    e.printStackTrace();   
		}
	}
	
	/**
	 * overridden method run to send and receive signals and catches exceptions with reading files and 
	 * handling sockets
	 */
	@Override
	public void run(){
		
		try {
			this.parse();
		}
		catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			System.out.println("File not found.");
		}
		catch(FileSystemException fse){
			System.out.println("File size too large.");
		}
		catch(IOException ioe){
			System.out.println("Error reading file or socket");
		}


	}
		/**
		 * Parses command given by client and tokenizes the command then
		 * calls the correct method accordingly
		 * 
	 * @throws IOException
	 */
	private void parse() throws IOException{
		String[] tokens = this.cmd.split(" ");	
		if (tokens[0].equalsIgnoreCase("picture")){
			this.sendPictureRequest();
			this.receivePicture();
		}
		else if (tokens[0].equalsIgnoreCase("video")){
			this.sendVideoRequest();
			//TODO this.receiveVideo();
		}
		else{
			this.sendElse();
		}
	}	
	
	private void sendVideoRequest(){
		//TODO
	}
		/**
		 * 
		 * sends command to the socket then sends the command to the server, afterwards making
		 * a call to receiveGet()
	 * @throws IOException
	 */
	private void sendPictureRequest() throws IOException{
		//send command on socket
	    //PrintWriter out = null;
	    //out = new PrintWriter(this.socketN.getOutputStream());
	    
	    //Send the command to the server
	    this.sendMessage(this.cmd.toLowerCase());
		
	}
	
	private void sendMessage(String msg){
		this.out.println(msg);
		this.out.flush();
	}
	
	/**
	 * 
	 * Receives command for get and handles the request
	 * @throws IOException
	*/
	private void receivePicture() throws IOException{
		String sendStatus = this.br.readLine();
		String imgType = null;
		System.out.println("Message from Webcam server: " + sendStatus);
		
		if(sendStatus.equalsIgnoreCase("PNG") || sendStatus.equalsIgnoreCase("JPG")){
			imgType = sendStatus;
			String fileName = String.format(
					"%simg%s.%s", 
					this.IMAGE_PATH, 
					this.generateId(), 
					imgType.toLowerCase()
				);
			this.sendMessage("ready");
		    //receive image from server and write it to a file
			byte[] buffer = new byte[BUF_SIZE];	
			FileOutputStream fos = new FileOutputStream(fileName);
			int count = 0;
			
			while((count = this.in.read(buffer)) > 0){				
				fos.write(buffer, 0, count); 
			}
			fos.flush();
			fos.close();
			//notify user of successful transfer
			DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			//get current date time with Date()
			Date date = new Date();
			System.out.println("Image successfully received at " + dateFormat.format(date) + "\nSaved as " + fileName);
		}
		else if(sendStatus.equalsIgnoreCase("error")){
			System.out.println("Server can not send photos at this time.");
		}
		else{
			this.sendMessage("error");
			System.out.println("Unrecognized status code received from server.");
		}
	}
	
	
	/**
	 * 
	 * Sends command for anything else (any commands that aren't get and put)
	 * @throws IOException
	*/
	private void sendElse() throws IOException{
		//send command
		PrintWriter out = null;
	    out = new PrintWriter(this.socket.getOutputStream());
	    //Send the command to the server
	    out.println(this.cmd);
	    out.flush();
	    this.receiveElse();
	}
	/**
	 * 
	 * Receives command for anything else, other than get and put
	 * @throws IOException
	*/
	private void receiveElse() throws IOException{
			this.printResponse();
		
	}
		
	/**
	 * Checks the server response and throws IOException
	 * 
	 * @return prompt to user that file is accepted or not accepted
	 * @throws IOException
	 */
    private boolean checkServerResponse() throws IOException{
		StringBuffer response = new StringBuffer();
		String input = null;
		
		input = this.br.readLine();
		response.append(input);
		return (response.toString().equals("Accept")) ? true : false;  
    }

	/**
	 * Prints response by user
	 * @throws IOException
	 */
	public void printResponse() throws IOException{
		//Print the response
		String input = null;
		while (((input = this.br.readLine()) != null) && !input.equals("")){
				System.out.println(input);
		}
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
