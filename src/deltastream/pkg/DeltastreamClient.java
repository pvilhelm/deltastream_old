/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.util.Random;
import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.security.*;
/**
 *
 * @author servos
 */
public class DeltastreamClient {
    //this is a client that receives part from the swarm and puts them together to a generic byte stream
    public static Config config;
    
    public static void main(String args[]){
        config = new Config(args);
        Broadcast broadcast = config.CreateBroadcast();
        broadcast.config = config;

        ServerSocket clientServerS = null;
        try{
            clientServerS = new ServerSocket(config.clientServerSocketPort);
        }
        catch(Exception ee){
            System.out.println("Couldn't set up Client Server Socket"+ee);
        }
        
        //init upload handler
        UploadHandler uploadHandler = new UploadHandler(broadcast);
        Thread uploadHandlerThread = new Thread(uploadHandler,"Upload Handler");
        uploadHandlerThread.start();
        
        /*OutputStreamServer outputStreamServer = new OutputStreamServer(broadcast);
        Thread outputStreamServerThread = new Thread(outputStreamServer,"OutputStreamServer");
        outputStreamServerThread.start();*/
        
        //w8 for new connections 
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
            }
            client.socket = clientSocket; 
            
            //creates thread for handling the client
            if(client.ulThread==null || !client.ulThread.isAlive()){
                synchronized(client){
                    if(client.ulThread==null || !client.ulThread.isAlive()){
                        Thread dlThread = new Thread( new ClientDownloadHandler(client, broadcast), "Handle Client Download ID="+client.clientSessionId);
                        Thread ulThread = new Thread( new ClientUploadHandler(client, broadcast), "Handle Client Upload ID="+client.clientSessionId);
                        client.dlThread = dlThread;
                        client.ulThread = ulThread;
                        dlThread.start();
                        ulThread.start();
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
