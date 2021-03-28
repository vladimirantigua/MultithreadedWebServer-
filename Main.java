package vladimirAntigua.msd;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.ArrayList;


// Assignment: Multithreading our HTTP server
//Modify your HTTP server so that everything that you do with the socket returned from accept() is performed in a
// separate thread. This likely will only require minor modifications to your code.
//
//Check to make sure your server is "thread safe" and fix any issues you find. Any data that might be shared between
// threads is a potential threading error! Make sure you're not doing unnecessary sharing! Any static variables are
// shared... you shouldn't have any of them!
//
//To test that you're actually serving multiple clients at the same time, try loading a large image from multiple
// browsers or browser tabs. In your loop to copy the bytes of the file, after each write call, call flush() on your
// output stream, then Thread.sleep(millisecondsToWait) to artificially slow your response so that you can verify both
// clients are loading simultaneously. Try having a few classmates hit your server at the same time.
//
//In other words, replaces fileInputStream.transferTo(socket.getOutputStream()) to:
//
//while(fileInputStream.available() > 0){
//	socketOut.write(fileInputStream.read());
//	socketOut.flush()
//	//maybe Thread.sleep(10); if this are still loading too fast
//}
//If you weren't sharing data before, this assignment might only require adding new Thread( () -> { and }).start() to
// your code.








// to access other classmate basic http project type their IP address like this for example: http://192.168.50.240:8080/
//  to test if the 404  message is working type the following after /8080/
//  enter anything like a single caracter to check if simulater an empty file and provide the 404 file not found
//  http://192.168.50.240:8080/b

//Assignment: Server Refactoring
//Refactor your HTTP Server by doing the following:
//
//First, handle the exceptions that may be thrown by I/O in a reasonable way. For example, some exceptions should cause the program to exit.
// Some should result in a warning or log message being printed to the screen. Should a client be able to crash the server program?
//
//Second, split up your long main() method into appropriate classes and methods. Spend a few minutes thinking about your design before writing any code.
// You may want to create classes for HTTPRequest and HTTPResponse. Think about how exceptions may be part of your design.

// Homework Server Refactoring:


class RoomDetails {
    ArrayList<String> users;
    ArrayList<String> messages;
    ArrayList<OutputStream> outputStreams;

    RoomDetails() {
        users = new ArrayList<>();
        messages = new ArrayList<>();
        outputStreams = new ArrayList<>();
    }

    public void broadcastMessage(String message) throws IOException {
        for (int i = 0; i < outputStreams.size(); i++) {
            WebSocketMessage.send(message, outputStreams.get(i));
            outputStreams.get(i).flush();

        }
    }

    public void addUser(String user) {
        users.add(user);
    }

    public void addMessage(String message) {
        messages.add(message);
    }
}

// WebSocketMessage class for sending and receiving messages:
class WebSocketMessage {

//
//    InputStream in = new ByteArrayInputStream(testByte);
//    DataInputStream ins = new DataInputStream(in);

    DataInputStream in;
    DataOutputStream out;
    // Test Bytes:
//    byte[] testByte = {(byte) 0x81, (byte) 0x85, (byte) 0x37, (byte) 0xfa, (byte) 0x21, (byte) 0x3d, (byte) 0x7f, (byte) 0x9f, (byte) 0x4d, (byte) 0x51, (byte) 0x58};
//    //0x81 0x05 0x48 0x65 0x6c 0x6c 0x6f (contains "Hello")
//    byte[] unmaskedTestByte = {(byte) 0x81, (byte) 0x05, (byte) 0x48, (byte) 0x65, (byte) 0x6c, (byte) 0x6f};

    long length;  // this is the length of the input that is coming in.
    byte[] maskingKey; // this is the masking key for decoding messages
    String message = "";


    //constructor: The socket being pass here (Socket s)
    WebSocketMessage(Socket s) throws IOException {

        // we use input and output to read and write from the socket
//        InputStream input = new ByteArrayInputStream(testByte);
        InputStream input = s.getInputStream(); // to read to the sock
//        DataInputStream ins = new DataInputStream(in); // to write to the sock
        OutputStream output = s.getOutputStream();
// DataInputStream in;
        this.in = new DataInputStream(input);
// DataOutputStream out;
        this.out = new DataOutputStream(output);
    }

    // method for sending a message we set it according 5.2.  Base Framing Protocol website:  https://tools.ietf.org/html/rfc6455#section-5.2
    static void send(String data, OutputStream out) throws IOException {
//        out.write(data);
        byte[] output = null;
// if the first or second byte is less than 126 character long it will go through 
        if (data.length() < 126) {
            output = new byte[data.length() + 2];
            // controlling the output byte this is the first byte :
            output[0] = (byte) 0x81;
            int a = data.length();
            // controlling the output byte this is the second byte :
            output[1] = (byte) a;

            for (int i = 0; i < data.length(); i++) {
                output[i+2] = (byte) data.charAt(i);
            }
        } else if (data.length() < 32768) {
            output = new byte[data.length() + 4];
            output[0] = (byte) 0x81;
            output[1] = (byte) 0x7E;
            int a = (byte) data.length();

            output[2] = (byte) (a >>> 8 & 0xFF);
            output[3] = (byte) 32768 & 0xFF;

            for (int i = 0; i < data.length(); i++) {
                output[i+4] = (byte) data.charAt(i);
            }
        }

        assert output != null;
        out.write(output);
    }

// Receive constructor:
    String receive() throws IOException {
        byte byte0 = in.readByte();
        byte opCode = (byte) (byte0 & 0x0F);
        byte byte1 = in.readByte();
        byte mask = (byte) (byte1 >>> 7);
        maskingKey = in.readNBytes(4);
        length = (byte) (byte1 & 0x7F);
        if (length == 126) {
            maskingKey = in.readNBytes(4);
            length = in.readShort();
        } else if (length > 126) {
            maskingKey = in.readNBytes(4);
            length = in.readLong();
        }

        System.out.println("The Length " + length);
        for (int i = 0; i < length; i ++) {
            message += (char) (in.readByte() ^ maskingKey[i & 0x3]);

        }

        return message;
    }

}



class  HTTPRequest {

//    public boolean containsWords(String inputString, String[] items) {
//        boolean found = true;
//        for (String item: items ) {
//            if (!inputString.contains(item)) {
//                found = false;
//                break;
//            }
//        }
//
//        return found;
//    }




    public Socket clientSocket;
    public HashMap<String, RoomDetails> rooms;

    HTTPRequest(Socket clientSocket, HashMap<String, RoomDetails> rooms) {
        this.clientSocket = clientSocket;
        this.rooms = rooms;
    }



// get the file
    public String getFileName() throws IOException, NoSuchAlgorithmException {


         //for storing the fileName:
        String fileName = "";
        // this will get the input from the clientSocket
        InputStreamReader isr =  new InputStreamReader(clientSocket.getInputStream());
        // BufferedReader reader will wrap the input from the clientSocket
        // make a scanner instead of the BufferReader
        BufferedReader reader = new BufferedReader(isr);
        // will get the next line:
        String line = reader.readLine();


//        BufferedReader tempReader = new BufferedReader(isr);
//        String newLine = tempReader.readLine();
//        for (int i = 0; i < 3; i++) {
//            if (line.contains("keep")) {
//                System.out.println("Contains keep ");
//            } else {
//                System.out.println("Contains keep ");
//            }
//            line = reader.readLine();
//        }

//        reader.reset();
//        line = reader.readLine();

        // loop and find fileName:
        boolean appendFileName = false;

        // find the fileName part of the first Line:
        for  (int i = 0; i < line.length(); i++){
            if (line.charAt(i) == '/'){
                appendFileName = true;
            }
            // stop loop at the space after filename
            if (line.charAt(i) == ' ' && appendFileName) {
                break;
            }
            // this adds character to fileName:
            if (appendFileName){
                fileName+= line.charAt(i);

            }
        }
        System.out.println("FileName" + fileName);
        //
        if (fileName.equals("/")) {
            fileName = "/index.html";
        }
        System.out.println(line);

        HashMap<String, String> httpRequestHeaders = new HashMap<>();

        while (!line.equals("")){

            if (line.contains(": ")) {
                String[] split = line.split(": ");
                String key = split[0];
                String value = split[1];
                httpRequestHeaders.put(key, value);
                System.out.println(line);

            }

            if (line.isBlank()) {
                break;
            }
            line = reader.readLine();
        }
        System.out.println("HEADERS " + httpRequestHeaders);

//            while (true) {
//                reader.readLine();
//                if (line.contains(": ")) {
//                    String[] split = line.split(":");
//                    String key = split[0];
//                    String value = split[1];
//                    httpRequestHeaders.put(key, value);
//                    System.out.println();

//                }
//                if (line.isBlank()) {
//                    break;
//                }
//                line = reader.readLine();
//            }
        if (httpRequestHeaders.containsKey("Sec-WebSocket-Key")) {
            String key = httpRequestHeaders.get("Sec-WebSocket-Key");

             System.out.println(" Hi this is the handshake! ");

            OutputStream out = clientSocket.getOutputStream();

// Handshake : sending the handshake =
            byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
                    + "\r\n\r\n").getBytes("UTF-8");
            out.write(response, 0, response.length);
            out.flush();

             System.out.println(response);

            System.out.println(response.length);


//            webSocketMessage.send(webSocketMessage.receive());
//            System.out.println("message " + webSocketMessage.in);

            String currentRoom = null;

            while (true) {

                WebSocketMessage webSocketMessage = new WebSocketMessage(clientSocket);
//                clientSocket.setKeepAlive(true);
                if (clientSocket.getInputStream().available() > 0) {
                    String newMessage = webSocketMessage.receive();
                    System.out.println("the message " + newMessage);
                    if (newMessage.contains("join")) {
                        String[] sp = newMessage.split( " ");
                        if (rooms.containsKey(sp[1])) {
                            currentRoom = sp[1];
                            rooms.get(currentRoom).outputStreams.add(out);
                            ArrayList<String> messages = rooms.get(currentRoom).messages;
                            if (messages != null) {
                                for (String message : messages) {
                                    WebSocketMessage.send(message, out);

                                }

                            }
                        } else {
                            // to create new room
                            RoomDetails roomDetails = new RoomDetails();
                            rooms.put(sp[1], roomDetails);
                            currentRoom = sp[1];
                            rooms.get(currentRoom).outputStreams.add(out);

                        }

                    } else if (newMessage.contains("get rooms")) {
                        for (String keys : rooms.keySet() ) {
                            System.out.println("HERE is a room " + keys);
                            WebSocketMessage.send(keys, out);

                        }

                    } else {
                        assert(rooms.containsKey(currentRoom));
                        rooms.get(currentRoom).addMessage(newMessage);
                        rooms.get(currentRoom).broadcastMessage(newMessage);

                    }
//                    webSocketMessage.send(newMessage);

                }
            }
        }
        System.out.println("Sending File");
        return fileName;

    }
}
//            for (int i = 0; i < httpRequestHeaders.size(); i++) {
//                if(httpRequestHeaders.containsKey("Sec-WebSocket-Key")) {
//                    String key = httpRequestHeaders.get("Sec-WebSocket-Key");

//                    OutputStream out = clientSocket.getOutputStream();

//                    byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
//                            + "Connection: Upgrade\r\n"
//                            + "Upgrade: websocket\r\n"
//                            + "Sec-WebSocket-Accept: "
//                            + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
//                            + "\r\n\r\n").getBytes("UTF-8");
//                    out.write(response, 0, response.length);

//                    String append = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
//                    String appendedKey = key + append;
//
//                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
//                    digest.reset();
//                    digest.update(appendedKey.getBytes("utf8"));
                    //  sha1 = String.format("%040x", new BigInteger(1, digest.digest()));

//                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream()); // out put for the header
//                    BufferedOutputStream dataOut = BufferedOutputStreamfferedOutputStream(clientSocket.getOutputStream()); // Output files

//                    out.println("HTTP/1.1 101 Switching Protocols");
//                    out.println("Upgrade: websocket");
//                    out.println("Connection: upgrade");
//                    out.println("Sec-WebSocket-Accept: " + digest);
//                    out.println("Sec-WebSocket-Accept: chat");
//
//                    out.println();
//                    WebSocketMessage webSocketMessage = new WebSocketMessage(clientSocket);
//                    System.out.println("message " + webSocketMessage.in);
//                }

//            }
//       return fileName;
//
//               }
//
//               }
//        String nextLine = reader.readLine();
////
//        while(!nextLine.equals("")) {
//           nextLine = reader.readLine();
//           if (nextLine.contains("Sec-Websocket-Key")) {
//               return "/websocket_Echoes.html";
////               System.out.println("Upgrade Request");
//
//           }
//            System.out.println(nextLine);
//
//        }



//class WebSocketResponse {
//    // passing socket
//    public Socket clientSocket;
//
//    //constructor:
//    WebSocketResponse(HashMap<String>, httpRequestHeaders, Socket ) throws IOException {
//        this.clientSocket = clientSocket;
//    }
//
//    //for storing the fileName:
//    String fileName = "";
//    // this will get the input from the clientSocket
//    InputStreamReader isr =  new InputStreamReader(clientSocket.getInputStream());
//    // BufferedReader reader will wrap the input from the clientSocket
//    // make a scanner instead of the BufferReader
//    BufferedReader reader = new BufferedReader(isr);
//    // will get the next line:
//    String line = reader.readLine();
//
//
//
//
//
//}



class HTTPResponse {
//    private final Socket ClientSocket;
    public Socket clientSocket;
    public File file;
    public int  fileLength;

    HTTPResponse(Socket newClientSocket, File newFile, int newFileLength) {
        clientSocket = newClientSocket;
        file = newFile;
        fileLength = newFileLength;
    }

    public byte[] readFileData() throws IOException, InterruptedException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        //Thread.sleep(millisecondsToWait) to artificially
        // slow your response so that you can verify both clients are loading simultaneously.
        //add the exception InterruptedException

//        Thread.sleep(4000);

        return fileData;
    }

    public void respond() throws  IOException {
        // PrinterWriter output wrapper for the header:
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream()); // to output for header
        // this will output wrapper for files:
        BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream());

        try{
            // read the content to return to the client:
            byte[] fileData = readFileData();

            System.out.println(fileData[1]);

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Length: " + fileLength);
            //Blank line between headers and content:
            out.println();
            // flush character output stream buffer:
            out.flush();
            //
            dataOut.write(fileData, 0, fileLength);
            dataOut.flush();
        } catch (Exception e){
            System.out.println("Inside catch block");
            out.println("HTTP/1.1 404 FILE NOT FOUND");
            //out.println("HTTP/1.1 405 FILE NOT FOUND");
            // When the file length is 0 this simulate not file present in directory this will create a page not found 404:
            out.println("Content-Length: " + 0);
            //Blank line between headers and content:
            out.println();
            // flush character output stream buffer:
            out.flush();
//                e.printStackTrace();
//                System.out.println(e);
        }
    }
}

//class MultithreadingTesting extends Thread {
//
//    Socket clientSocket;
//
//    MultithreadingTesting(Socket clientSocket) throws  IOException {
//        this.clientSocket = clientSocket;
//    }
////
//    public void run() {
//        HTTPRequest input = new HTTPRequest(clientSocket);
//
//        // To displaying the thread that is running:
//        System.out.println(Thread.currentThread().getId() + " is running");
//
//        try {
//            File file = new File("/Users/mj/vladimirantigua/CS6011/homework/Basic_HTTP_Server/src/vladimirAntigua/msd" + input.getFileName());
//            int fileLength = (int) file.length();
//            HTTPResponse output = new HTTPResponse(clientSocket, file, fileLength);
//            output.respond();
//
//            clientSocket.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//}

class Main {

//    private static HashMap<String, RoomDetails> ;

    public static void main(String[] args) throws Exception {

        try {
	// write your code here
//        final ServerSocket server = new ServerSocket(8080);
                ServerSocket server = new ServerSocket(8080);
                System.out.println(" Oh yeah !!! Listening to the connection on port 8080 :) ");
                HashMap<String, RoomDetails> rooms = new HashMap<>();

        // create an infinite loop to continue listening indefinitely:
            while (true) {

            //Try catch:
//            Socket clientSocket = null;


            //accept incoming connection by blocking call to accept() method:
            //final Socket client = server.accept();
            //see what is coming from browser in form of HTTP request. When you connect to http://localhost:8080,your browser
            // will send a GET HTTP request to the server. I can read the content of request using InputStream opened from the client socket.
            // It's better to use BufferedReader because browser will send multiple line:

// will return the socket once have establish connection:
                    Socket clientSocket = server.accept();

// when is get accepted by the server we are creating a new thread:
//            MultithreadingTesting multithreadingTesting = new MultithreadingTesting(clientSocket);
                Thread multithreadingTesting = new Thread( () -> {

                try {

                    HTTPRequest input = new HTTPRequest(clientSocket, rooms);

                    // To displaying the thread that is running:
                    System.out.println(Thread.currentThread().getId() + " is running");

                    File file = new File("/Users/mj/vladimirantigua/CS6011/homework/Basic_HTTP_Server/src/vladimirAntigua/msd" + input.getFileName());
                    int fileLength = (int) file.length();
                    HTTPResponse output = new HTTPResponse(clientSocket, file, fileLength);
                    output.respond();

                    clientSocket.close();

                } catch (IOException | NoSuchAlgorithmException e) {


                    try {
                        rooms.clear();
                        clientSocket.close();

                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    e.printStackTrace();
                }
            });
            multithreadingTesting.start();
        }

//        }

//                HTTPRequest input = new HTTPRequest(clientSocket);
       
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Connection Failed " + e );
      }
   }
}
//                File file = new File("/Users/mj/vladimirantigua/CS6011/homework/Basic_HTTP_Server/src/vladimirAntigua/msd" + input.getFileName());
                // (int) to change  double to int:
//                int fileLength = (int) file.length();

//                HTTPResponse output = new HTTPResponse(clientSocket, file, fileLength);
//                output.respond();
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.out.println("Connection Failed " + e );
//            }
//        }
//    }

//Thread.sleep(millisecondsToWait)

// PrinterWriter output wrapper for the header:
//            PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
//            // this will output wrapper for files:
//            BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream());
// this will get the input from the clientSocket
//            InputStreamReader isr =  new InputStreamReader(clientSocket.getInputStream());
// BufferedReader reader will wrap the input from the clientSocket
// make a scanner instead of the BufferReader
//            BufferedReader reader = new BufferedReader(isr);
// will get the next line:
//            String line = reader.readLine();
////////////////// for storing the fileName:
//            String fileName = "";
//            // loop and find fileName:
//            boolean appendFileName = false;
//            // find the fileName part of the first Line:
//            for  (int i = 0; i < line.length(); i++){
//                if (line.charAt(i) == '/'){
//                    appendFileName = true;
//                }
//                // stop loop at the space after filename
//                if (line.charAt(i) == ' ' && appendFileName) {
//                    break;
//                }
//                // adds char to fileName:
//                if (appendFileName){
//                    fileName+= line.charAt(i);
//
//                }
//            }
//            System.out.println("FileName" + fileName);
//            //
//            if (fileName.equals("/")) {
//                fileName = "/index.html";
//            }
/////////

// check if the file exits and will read the content to return to client:
//            if (file.exists()){
//                byte[] fileData = readFileData(file, fileLength);
//
//                out.println("HTTP/1.1 200 OK");
//                out.println("Content-Length: " + fileLength);
//                //Blank line between headers and content:
//                out.println();
//                // flush character output stream buffer:
//                out.flush();
//
//                //
//                dataOut.write(fileData, 0, fileLength);
//                dataOut.flush();
//            } else {
//                out.println("HTTP/1.1 404 FILE NOT FOUND");
//                //out.println("HTTP/1.1 405 FILE NOT FOUND");
//                // When the file length is 0 this simulate not file present in directory this will create a page not found 404:
//                out.println("Content-Length: " + 0);
//                //Blank line between headers and content:
//                out.println();
//                // flush character output stream buffer:
//                out.flush();
//            }

//            try{
//                byte[] fileData = readFileData(file, fileLength);
//                out.println("HTTP/1.1 200 OK");
//                out.println("Content-Length: " + fileLength);
//                //Blank line between headers and content:
//                out.println();
//                // flush character output stream buffer:
//                out.flush();
//                //
//                dataOut.write(fileData, 0, fileLength);
//                dataOut.flush();
//            } catch (Exception e){
//                System.out.println("Inside catch block");
//                out.println("HTTP/1.1 404 FILE NOT FOUND");
//                //out.println("HTTP/1.1 405 FILE NOT FOUND");
//                // When the file lenght is 0 this simulate not file present in directory this will create a page not found 404:
//                out.println("Content-Length: " + 0);
//                //Blank line between headers and content:
//                out.println();
//                // flush character output stream buffer:
//                out.flush();
////                e.printStackTrace();
////                System.out.println(e);
//            }



//    private static byte[] readFileData(File file, int fileLength) throws IOException {
//        FileInputStream fileIn = null;
//        byte[] fileData = new byte[fileLength];
//
//        try {
//            fileIn = new FileInputStream(file);
//            fileIn.read(fileData);
//        } finally {
//            if (fileIn != null)
//                fileIn.close();
//        }
//        return fileData;
//    }

//try {
//        //  Block of code to try
//    }
//catch(Exception e) {
//        //  Block of code to handle errors
//    }


//            while (!line.isEmpty()) {
//                System.out.println(line);
//                line = reader.readLine();

//send today's date to the client:

//            try (Socket clientSocket = server.accept()) {
//                Date today = new Date();
//                String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + today;
//                clientSocket.getOutputStream().write(httpResponse.getBytes("UTF-8"));

// if receive date and time Thu Oct 01 19:04:35 MDT 2020 in the browser when refresh http://localhost:8080/
// it means that the HTTP server is working correctly and it is activate listening to port 8080.