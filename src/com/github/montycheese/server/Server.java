package com.github.montycheese.server;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;

public class Server {

	private ExecutorService threadPool = Executors.newCachedThreadPool();
	private final int BACKLOG = 20;
	private int port = -1;
	private String address = null;
	private ServerSocket serverSocket = null;
	private final String CONFIG_FILE = "config.txt";
	//file should be of the format:
	//address
	//port #
	
	/**
	 * Server initialization
	 *@params address, port
	*/
	public Server(String address, int port){
		this.address = address;
		this.port = port;
	}
	/**
	 * Overloaded Constructor for server initialization
	 * @params nPort, tPort
	*/
	public Server(){
		File file = new File(this.CONFIG_FILE);
		//try with resources
		try(Scanner s = new Scanner(file)){
			while(s.hasNext()){
				this.address = s.nextLine();
				this.port = s.nextInt();
			}
		} catch (FileNotFoundException e) {
			System.out.println("File " + this.CONFIG_FILE + " could not be found.");
			System.exit(0);
		}
		
		if(this.address == null || this.port < 0 || this.port > 65335){
			System.out.println("Config file is improperly formatted");
			System.exit(0);
		}
	}
	//Automatically called when server is initialized and the connection is successful.
	public void run(){
		System.out.println("Server is running");
		
		//create a socket
		try {
			//create two sockets listening at terminate and normal ports
			this.serverSocket = new ServerSocket(
					this.port,
					this.BACKLOG
			);
			while(true){
				//wait for incoming message from client
					try {
						//accept message
						Socket clientSocket = this.serverSocket.accept();
						//create new server thread to handle
						this.threadPool.execute(new ServerThread(
								clientSocket, 
								this.serverSocket
						));
						
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
		} 
		
		catch (IOException e) {
 			System.out.println("Error reading socket");
		}
		catch(IllegalArgumentException iae){
			iae.printStackTrace();
		}
		finally{
			//close sockets
			if (this.serverSocket != null){
				try {
					this.serverSocket.close();
				} 
				catch (IOException e) {
					System.out.println("Error closing server socket.");
				}
			}
		}
		
	}

	
	public static void main(String[] args){
		
		boolean DEVELOPMENT = true;
		if (DEVELOPMENT){
			System.out.println("Starting Server");
			Server server = new Server("localhost", 60000);
			server.run();
		}
		else{
			System.out.println("Starting Server");
			Server server = new Server();
		}
		
	}

}