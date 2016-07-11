all: src/com/github/montycheese/server/Server.java src/com/github/montycheese/client/Client.java src/com/github/montycheese/server/ServerThread.java src/com/github/montycheese/client/ClientThread.java
	javac -cp ".:dependencies/webcam-capture-0.3.10.jar" -d bin/ src/com/github/montycheese/server/ServerThread.java src/com/github/montycheese/server/Server.java
	javac -cp ".:dependencies/webcam-capture-0.3.10.jar" -d bin/ src/com/github/montycheese/client/ClientThread.java  src/com/github/montycheese/client/Client.java 

clean:
	rm -f bin/com/github/montycheese/server/*.class
	rm -f bin/com/github/montycheese/client/*.class