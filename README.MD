A distributed service that allows multiple clients to view images from a central webcam. 

To compile:
1. clone repo
2. type 'make'

To run client:
$java -cp bin/ com/github/montycheese/client/Client

To run webcam server:
$java -cp bin/:dependencies/* com/github/montycheese/server/Server

To shutdown client: 
type 'quit'

To shutdown server: 
hit ctrl + c
