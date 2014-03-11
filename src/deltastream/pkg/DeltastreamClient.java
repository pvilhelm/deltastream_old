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
        
        //Clean up the list of requested parts every 5 second. 
        Timer timer2 = new Timer("Clean Rq list");
        timer2.schedule(new CleanRqList(broadcast), 5000, 5000);//
        
        //init upload handler
        /*UploadHandler uploadHandler = new UploadHandler(broadcast);
        Thread uploadHandlerThread = new Thread(uploadHandler,"Upload Handler");
        uploadHandlerThread.start();*/
        
        ListOfClients.Client client2 = broadcast.listOfClients.AddClient("85.24.167.217");
        Thread thread = new Thread(new ConnectToClient(client2,broadcast),"Make connection thread");
        thread.start();
        
        /*OutputStreamServer outputStreamServer = new OutputStreamServer(broadcast);
        Thread outputStreamServerThread = new Thread(outputStreamServer,"OutputStreamServer");
        outputStreamServerThread.start();*/
        
        OutputStreamServerUDP outputStreamServerUDP = new OutputStreamServerUDP(broadcast);
        Thread outputStreamServerUDPThread = new Thread(outputStreamServerUDP,"OutputStreamServerUDP");
        outputStreamServerUDPThread.start();
        
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
