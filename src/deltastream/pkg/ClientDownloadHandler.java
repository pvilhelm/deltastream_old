/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.*;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This class handles downloads from ANOTHER client.
 * 
 * Each TCP connection to the local instance of this program spawn one 
 * ClientDownLoadHandler thread. The thread might be closed with out 
 * removing the client from the client list.  
 * 
 * @author Petter Tomner
 */

public class ClientDownloadHandler implements Runnable{

    DataInputStream ISData;
    int type;
    long ID;
    String clientIp;
    ListOfClients.Client client;
    Broadcast broadcast;
    
    /**
     * 
     * @param client the client instance that is handled by this instance.
     * @param broadcast the broadcast instance the client belongs to
     */
    
    public ClientDownloadHandler(ListOfClients.Client client, Broadcast broadcast){
            ID = 0;
            type = 0;
            this.broadcast = broadcast; 
            this.client = client; 
    }
    
    
    @Override
    public void run(){
        
        try{
            client.IS = new BufferedInputStream(client.socket.getInputStream());
            ISData = new DataInputStream(client.IS);
            //OSData = new DataOutputStream(OS);
        }
        catch(Exception ee){
            System.out.println("Couldn't get streams from client socket"+ee);
            client.Drop();
            return;
        }
        
        int partN;
        byte [] data;
        
        while(client.connected){
         
            try{
                ISData.mark(100);
                ID = ISData.readLong();
                type = ISData.readByte();
                ISData.reset();
            }
            catch(Exception ee){
                System.out.println("Couldn't read first char from socket"+ee);
                client.Drop();
                return;
            }
            switch(type){
                case 'p': //incoming part
                    try{
                        long broadcastID = ISData.readLong();
                        ISData.readByte();
                        partN = ISData.readInt();
                        long creationTime = ISData.readLong();
                        int dataLength = ISData.readInt();
                        data = new byte[dataLength];
                        int nReadBytes = 0;
                        while(nReadBytes<dataLength)
                            nReadBytes += ISData.read(data,nReadBytes,dataLength-nReadBytes);        
                        if(nReadBytes!=dataLength)
                            System.out.println("Couldnt read all bytes of part");//TODO error management
                        //System.out.println("Acuired part "+partN);
                        Part part = new Part(partN, data); //TODO check signature
                        part.timeCreated = creationTime;
                        
                        //TODO add check for broadcast id and signature
                        broadcast.parts.PutPart(part);
                        client.downloadedParts++;
                        broadcast.requestedParts.remove(partN);
                    }
                    catch(Exception ee){
                        System.out.println("couldnt read part"+ee);
                        client.Drop();
                    }
                    break;
                case 'q': // q = ask which parts a node has
                    client.PutUlQue('a');
                    client.GetListOfParts();//read the parts the client sent
                    break;
                case 'k'://k = public key request
                    client.PutUlQue('l');
                    break;
                case 'a'://a = answer on part question
                    client.GetListOfParts();
                    break;
                case 'g': //"goodbye"
                    broadcast.listOfClients.RemoveClient(client);
                    return;
                case 'b': //i got banned by client :S
                    broadcast.listOfClients.RemoveClient(client);
                    return;
                case 's': //answer to stats request
                    ;//todo
                    break;
                case 'l': //anser to a key request
                    ;///todo
                    break;
                case 'f'://the data the server sent the clietn was corrupt
                    ;//todo
                    break;
                case 'n'://request master list of streams
                    ;//todo
                    break;
                case 'x'://do I want this part?
                    try{
                        client.GetListOfParts();
                        
                    }
                    catch(Exception ee){
                        System.out.println("Couldnt read what part client wants"+ee);
                        client.Drop();
                        return;
                    }
                    partN = client.PickPartINeed();// TODO this shouldnt pick allready requested parts
                    
                    if(!broadcast.parts.allParts.containsKey(partN) && !broadcast.requestedParts.contains(partN)){ //TODO or is downloading that part
                            client.PutUlQue('c', partN);
                            broadcast.requestedParts.offer(partN);
                            //System.out.println("Dont have that part accepted");
                            
                    }
                    else{
                            client.PutUlQue('d',partN);
                            //System.out.println("Have that part declined");
                    }
                    break;
                case 'd'://the client didnt want the part
                    try{
                        ISData.readLong();
                        ISData.readByte();
                        partN = ISData.readInt();
                        //System.out.println("The client didnt want part: "+partN);
                    }
                    catch(Exception ee){
                        System.out.println("Couldnt read part id");
                        client.Drop();
                    }
                    break;
                case 'c'://the client wants the part
                    partN = -1;
                    try{
                        ISData.readLong();
                        ISData.readByte();
                        partN = ISData.readInt();
                        //System.out.println("The client wants part: "+partN);
                    }
                    catch(Exception ee){
                        System.out.println("Couldnt read part id");
                        client.Drop();
                    }
                    if(partN==-1)
                        break;
                    client.PutUlQue('p',partN);
                    break;
                default:
                    System.out.println("Error: Message wrong format, no type");
                    client.Drop();
                    return;//TODO REMOVE CLIENT CONNECNTIOn
                    //break;
            } 
        }
    } 
}
