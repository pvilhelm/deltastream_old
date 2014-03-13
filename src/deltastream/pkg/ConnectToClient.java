/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.util.Date;

/**
 * A instance of this class is spawned to connect to a client. 
 * 
 * To not block the thread that spawned it while trying to connect. 
 *
 * @author Petter Tomner
 */

public class ConnectToClient implements Runnable{
    
    ListOfClients.Client client;
    Broadcast broadcast;
    
    /**
     * 
     * @param client the client to try to connect to
     * @param broadcast the broadcast to which the client belongs
     */
    public ConnectToClient(ListOfClients.Client client, Broadcast broadcast){
        this.client = client;
        this.broadcast = broadcast;
    }
    
    @Override
    public void run(){
        
        synchronized(client){ //TODO should be "thread making lock" instead
            if(client.dlThread != null && client.dlThread.isAlive())
                return;
            if(client.ulThread != null &&client.ulThread.isAlive())
                return;
            
            if(client.socket == null || !client.socket.isConnected() || client.socket.isClosed()){
                try{
                    Socket socket =  new Socket(client.IP,Config.clientServerSocketPort);
                    client.socket = socket;
                    socket.setReceiveBufferSize(1000000);
                    client.OS = new BufferedOutputStream(socket.getOutputStream());
                    client.IS = new BufferedInputStream(socket.getInputStream());
                    client.socket.setSoTimeout(0);//TODO remeber to remove/set lower
                }
                catch(Exception ee){
                    System.out.println("Couldnt create streams in ConnectedToClient");
                    client.unsucessfullRcAttempts++;
                    if(client.unsucessfullRcAttempts>15){
                        client.Drop();
                        broadcast.listOfClients.RemoveClient(client);
                    }
                    //TODO add reomve client thingy here when to many failed rc attemps
                    return;
                }
            }
            client.lastTriedToConnetcTo =   new Date();
            client.connected = true;
            //creates thread for handling the client
            Thread dlThread = new Thread( new ClientDownloadHandler(client, broadcast), "Handle Client Download ID="+client.clientSessionId);
            Thread ulThread = new Thread( new ClientUploadHandler(client, broadcast), "Handle Client Upload ID="+client.clientSessionId);
            client.dlThread = dlThread;
            client.ulThread = ulThread;
            dlThread.start();
            ulThread.start();
        }
    }   
}


