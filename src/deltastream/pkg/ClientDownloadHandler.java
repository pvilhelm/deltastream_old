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

class ClientDownloadHandler implements Runnable{

    DataInputStream ISData;
    int type;
    long ID;
    String clientIp;
    ListOfClients.Client client;
    Broadcast broadcast;
    ClientDownloadHandler(ListOfClients.Client client, Broadcast broadcast){
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
            try{
                client.socket.close();
            }
            catch(Exception ee2){
                System.out.println("Problem closing socket after exception: " +ee2);
            }
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
                        ISData.readLong();
                        ISData.readByte();
                        partN = ISData.readInt();
                        ISData.readLong();
                        int dataLength = ISData.readShort();
                        data = new byte[dataLength];
                        int nReadBytes = 0;
                        while(nReadBytes<dataLength)
                            nReadBytes += ISData.read(data,nReadBytes,dataLength-nReadBytes);        
                        if(nReadBytes!=dataLength)
                            System.out.println("Couldnt read all bytes of part");//TODO error management
                        System.out.println("Acuired part "+partN);
                        Part part = new Part(partN, data); //TODO check signature
                        broadcast.parts.PutPart(part);
                        client.downloadedParts++;
                    }
                    catch(Exception ee){
                        System.out.println("couldnt read part"+ee);
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
                        ISData.readLong();
                        ISData.readByte();
                        partN = ISData.readInt(); /// TODO <------------------ kom ihåg lägga in check här!!
                        System.out.println("Read part offer "+partN);
                    }
                    catch(Exception ee){
                        System.out.println("Couldnt read what part client wants"+ee);
                        return;
                    }
                    if(!broadcast.parts.allParts.contains(partN)){ //TODO or is downloading that part
                            client.PutUlQue('c', partN);
                            System.out.println("Dont have that part accepted");
                    }
                    else{
                            client.PutUlQue('d',partN);
                            System.out.println("Have that part declined");
                    }
                    break;
                case 'd'://the client didnt want the part
                    try{
                        ISData.readLong();
                        ISData.readByte();
                        partN = ISData.readInt();
                        System.out.println("The client didnt want part: "+partN);
                    }
                    catch(Exception ee){
                        System.out.println("Couldnt read part id");
                    }
                    break;
                case 'c'://the client wants the part
                    partN = -1;
                    try{
                        ISData.readLong();
                        ISData.readByte();
                        partN = ISData.readInt();
                        System.out.println("The client wants part: "+partN);
                    }
                    catch(Exception ee){
                        System.out.println("Couldnt read part id");
                    }
                    if(partN==-1)
                        break;
                    client.PutUlQue('p',partN);
                    break;
                default:
                    System.out.println("Error: Message wrong format, no type");
                    //TODO flush input
                    try{
                        client.OS.close();
                    }
                    catch(Exception ee){
                        System.out.println("Problem closing socket: "+ee);
                    }
                    return;//TODO REMOVE CLIENT CONNECNTIOn
                    //break;
            } 
            
            
            //TODO add check for closing thread 
        }
    } 
}
