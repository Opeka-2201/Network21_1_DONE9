import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;

/**
 * Class communicating with server using Monster Hunting Protocol.
 * Also communicates with MHG class to computes data sent by server.
 * 
 * @author Arthur LOUIS
 * @see MHG.java
 */

public class Subscriber{
    // Only version understandable by Subscriber.java
    private static final int protocolVersion = 1;

    public static void main(String[] args) throws IOException{
        
        //Initiating the game;
        MHG game = new MHG();

        // Scanner to compute guesses
        Scanner sc = new Scanner(System.in);
        
        // Data to create the socket
        String host = "localhost";
        int port = Integer.parseInt(args[0]);

        // Creation of socket
        Socket s = new Socket(host, port);
        InputStream response = s.getInputStream();
        OutputStream query = s.getOutputStream();
        s.setSoTimeout(2000);

        // Initiating variables useful to understand stream
        Object[] MHPMessage = new Object[4];
        int nbResponseBytes = 0;
        int nbQueryBytes = 0;
        byte[] streamByte = new byte[512];

        // Trying to establish a connection with server
        try{
            query.write(sendStream(0, "s191230", "victory"));
            query.flush();

            response.read(streamByte);
            Object[] decodedStream = readStream(streamByte,nbResponseBytes); 

            // Checking if connection went well
            if(decodedStream[1].equals("OK"))
                System.out.println("Victory stream connected");
            else
                System.out.println("Error while connecting victory");

        }catch(SocketTimeoutException e){
            // In the case of a problem during connection to server, we close the streams, scanner and socket
            System.out.println("Timeout");
            query.close();
            response.close();
            s.close();
            sc.close();
            throw new SocketTimeoutException("Socket Timeout");
        }catch(IOException e){
            System.out.println(e);
        }

        // Now that connection is established for victory we create the channel for position

        query.write(sendStream(0, "s191230", "position"));
        System.out.println("Position stream connected");
        query.flush();

        // Getting first packets sent by server to compute the first grid
        try{
            for(;;){
                nbQueryBytes = response.read(streamByte);
                
                while(nbResponseBytes < nbQueryBytes){
                    MHPMessage = readStream(streamByte, nbResponseBytes);
                    //streamToString(MHPMessage);

                    nbResponseBytes += (int) MHPMessage[3];

                    // Update of the grid's sensors if the message indicates position
                    int messageType = (int) MHPMessage[0];
                    String messageTopic = (String) MHPMessage[1];

                    // In this loop the only type of message interesting us is position data
                    if(messageTopic.equals("position") && messageType == 1){
                        String sensorData = (String) MHPMessage[2];
                        game.computeSensor(sensorData);
                        query.write(sendStream(2, "OK", null));
                        query.flush();
                    }
                }
                nbResponseBytes = 0;
            }
        }catch(SocketTimeoutException e){
            // We simply wait for the server to stop sending packets to catch the exception
        }

        // We loop on the same code while the game isn't won by the player
        boolean victory = false;
        
        do{
            // Getting guess by the player (after showing grid)
            System.out.println("The Monster is on one of the squares with an X\n");
            game.printMatrix();
            System.out.print("Guess Monster's position (row/column) : ");

            String scannedCommand = sc.nextLine();

            query.write(sendStream(1, "guess", scannedCommand));
            query.flush();

            // After each guess we reinitiate the grid to compute new data from server
            game.gameInit();

            try{
                while(!victory){
                    nbQueryBytes = response.read(streamByte);

                    while(nbResponseBytes < nbQueryBytes){
                        MHPMessage = readStream(streamByte, nbResponseBytes);
                        nbResponseBytes += (int) MHPMessage[3];

                        int messageType = (int) MHPMessage[0];
                        String messageTopic = (String) MHPMessage[1];

                        // Checking if data sent by server is expected
                        if(messageType == 1 && messageTopic.equals("position")){
                            String sensorData = (String) MHPMessage[2];
                            game.computeSensor(sensorData);
                            query.write(sendStream(2, "OK", null));
                            query.flush();
                        }
                        else if(messageType == 1 && messageTopic.equals("victory")){
                            victory = true;
                            System.out.println("You found the Monster, well done");
                            // After victory we close streams, scanner and socket
                            query.close();
                            response.close();
                            s.close();
                            sc.close();
                            
                            // Immidiately exiting loop
                            break;
                        }
                    }
                    nbResponseBytes = 0;
                }
            }catch(SocketTimeoutException e){
                // We simply wait for the server to stop sending packets to catch the exception
            }
        }while(!victory);      
    }

    /**
     * Method to represent a MHPMessage into a string
     * 
     * @param MHPMessage message to translate into string
     */
    private static void streamToString(Object[] MHPMessage){
        if(MHPMessage.length != 4){
            System.out.println("Error in message to translate");
            System.exit(-1);
        }
        else{
            System.out.println("Message type : " + (int) MHPMessage[0]
                               + "\nPayload 1 : " + (String) MHPMessage[1]
                               + "\nPayload 2 : " + (String) MHPMessage[2]
                               + "\nLength : " + (int) MHPMessage[3]);
        }
    }

    /**
     * Method to read the stream sent by server decoding it into an array of Objects 
     * (the message being represented by different type of Objects).
     * 
     * @param streamByte byte stream received from server
     * 
     * @param nbOffsetBytes offset representing the location of the first byte we do care about
     * 
     * @return decodedStream 
     */
    private static Object[] readStream(byte[] streamByte, int nbOffsetBytes){
        Object[] decodedStream = new Object[4];

        String payload1, payload2;
        int payload1Length, payload2Length;

        int messageType = (int) streamByte[1 + nbOffsetBytes];

        if(messageType == 1){ // Publish message.
            // Computation of payloads using offset
            payload1Length = streamByte[3 + nbOffsetBytes];
            payload2Length = streamByte[3 + nbOffsetBytes + 1 + payload1Length];
            payload1 = new String(streamByte, 3 + nbOffsetBytes + 1, payload1Length);
            payload2 = new String(streamByte, 3 + nbOffsetBytes + 1 + payload1Length + 1, payload2Length);
        }
        else if(messageType == 2){ // Ack message.
            if(streamByte[2 + nbOffsetBytes] == 3){ // Tells that it is ackOK message (shorter).
                payload1 = "OK";
                payload2 = null;
            }
            else{ // ackError message.
                // Computation of payloads using the offest.
                payload1Length = streamByte[3 + nbOffsetBytes];
                payload2Length = streamByte[3 + nbOffsetBytes + 1 + payload1Length];
                payload1 = new String(streamByte, 3 + nbOffsetBytes + 1, payload1Length);
                payload2 = new String(streamByte, 3 + nbOffsetBytes + 1 + payload1Length + 1, payload2Length);
            }
        }
        else{
            // If the message type is unknown, we simply ignore those bytes.
            payload1 = null;
            payload2 = null;
            streamByte[2 + nbOffsetBytes] = (byte) 256;
        }

        // Depending on the message type, this computes total length of the message.
        int totalLength = (int) streamByte[2 + nbOffsetBytes] + 3;
        
        // Placing everything in the expected position and returning decodedStream.
        decodedStream[0] = messageType;
        decodedStream[1] = payload1;
        decodedStream[2] = payload2;
        decodedStream[3] = totalLength;
        return decodedStream;
    }

    /**
     * Method to send MHP message to server using apropriate syntax
     * \Version\Type\TotalPayloadLength + 2\Payload1Length\Payload1\Payload2Length\Payload2.
     * 
     * @param messageType int representing message type : 0 - Subscribe, 1 - Publish, 2 - ack.
     * 
     * @param payload1 first string to send (transform in bytes first).
     * 
     * @param payload2 second string to senf (transform in bytes first).
     * 
     * @return a byte array representing the MHP message correctly.
     * 
     * @throws IOException
     */
    private static byte[] sendStream(int messageType, String payload1, String payload2) throws IOException{
        
        // Using provided types to more easely implement the method.
        ByteArrayOutputStream sendedStream = new ByteArrayOutputStream();
        byte[] byteStream;

        try {
            // Firstly transforming payload1 in UTF-8 bytes (as there will always be a payload irrespective of the type).
            byte[] payload1Bytes = payload1.getBytes(StandardCharsets.UTF_8);
            int payload1Length = payload1Bytes.length;

            // Writing data provided in any message.
            sendedStream.write(protocolVersion);
            sendedStream.write(messageType);

            if(messageType == 0 || messageType == 1){
                // In this case there is 2 payload thus need to translate it un UTF-8 bytes.
                byte[] payload2Bytes = payload2.getBytes(StandardCharsets.UTF_8);
                int payload2Length = payload2Bytes.length;

                // Writing everything in the good order.
                sendedStream.write(1 + payload1Length + 1 + payload2Length);
                sendedStream.write(payload1Length);
                sendedStream.write(payload1Bytes);
                sendedStream.write(payload2Length);
                sendedStream.write(payload2Bytes);
            }
            else if(messageType == 2){
                // In this case there is only 1 payload, we just write in the correct order previously computed values.
                sendedStream.write(1 + payload1Length);
                sendedStream.write(payload1Length);
                sendedStream.write(payload1Bytes);
            }
            else{
                System.out.println("Unknown message type");
            }
            
            // Finnaly transforming sendedStream into the array of bytes expected by protocol
            byteStream = sendedStream.toByteArray();
        }catch(IOException e){
            System.out.println("Error while sending packet");
            throw new IOException("Sending packet error");
        }

        return byteStream;
    }
}