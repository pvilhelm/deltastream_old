package deltastream.pkg;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.security.*;



public class DeltastreamServer{

    public static Config config;

    public static void main(String[] args) {
        // TODO code application logic here
        config = new Config(args);
        Broadcast broadcast = config.CreateBroadcast(); //creates a broadcast object from the config
        broadcast.config = config;

        /*
        ReadInputStream readInputStream = new ReadInputStream(BufferedInDataStream, broadcast);
        Thread readInputStreamThread = new Thread(readInputStream);
        readInputStreamThread.start();
        */
        
        //Sample the input stream every SamplingPeriod ms
        ReadInputStreamUDP readInputStreamUDP = new ReadInputStreamUDP(broadcast);
        Thread readInputStreamThread = new Thread(readInputStreamUDP, "Read input UDP");
        readInputStreamThread.start();
        
        //Listen for clients
        ServerSocket clientServerS = null;
        try{
            clientServerS = new ServerSocket(config.clientServerSocketPort);
            clientServerS.setSoTimeout(0); ///<--remeber to remove
        }
        catch(Exception ee){
            System.out.println("Couldn't set up Client Server Socket"+ee);
        }

        //init upload handler
        //will upload to known clients at "random"==smart algortim

        ListOfClients.Client client2 = broadcast.listOfClients.AddClient("81.170.216.105");
        Thread thread = new Thread(new ConnectToClient(client2,broadcast),"Make connection thread");
        thread.start();
        
        try{
            Thread.sleep(2000);
        }
        catch(Exception ee){}
        
        UploadHandler uploadHandler = new UploadHandler(broadcast);
        Thread uploadHandlerThread = new Thread(uploadHandler,"Handle uploads");
        uploadHandlerThread.start();
        
         //w8 for new incoming connections 
        for(;;){
            Socket clientSocket;
            try{
                clientSocket = clientServerS.accept();
                System.out.println("Client accepted from IP: " + clientSocket.getInetAddress() 
                        +" at port: " +clientSocket.getLocalPort()+" to port: "+clientSocket.getPort());
                clientSocket.setSendBufferSize(1000);
                clientSocket.setReceiveBufferSize(1000000);
                clientSocket.setSoTimeout(0); ///<--remeber to remove
            }
            catch(Exception ee){
                System.out.println("Couldn't accept incoming client socket request");
                continue;
            }
            String clientIp = clientSocket.getInetAddress().getHostAddress();
            ListOfClients.Client client;
            synchronized(broadcast.listOfClients){
                if(!broadcast.listOfClients.clientHashtable.containsKey(clientIp)){//true == new client
                    client = broadcast.listOfClients.AddClient(clientIp); //adds client to client list
                }
                else{
                    client = broadcast.listOfClients.clientHashtable.get(clientIp);
                }
            client.socket = clientSocket; 
            }
            
            
            //creates thread for handling the client
            if(client.ulThread==null || !client.ulThread.isAlive()){
                synchronized(client){
                    if(client.ulThread==null || !client.ulThread.isAlive()){
                        Thread thread2 = new Thread(new ConnectToClient(client,broadcast),"Make connection thread");
                        thread2.start();
                    }
                    else
                        System.out.println("Error: Thread and/or connection allready exists ... cought sync error");
                }
            }
            else
                System.out.println("Error: Thread and/or connection allready exists");
        }
    }   
}
