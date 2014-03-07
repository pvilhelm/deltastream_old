/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;
import java.io.*;
 
import java.net.*;
/**
 *
 * @author servos
 */
public class ClientUploadHandler implements Runnable{
    
    DataOutputStream OSData;
    int type;
    long ID;
    String clientIp;
    ListOfClients.Client client;
    Broadcast broadcast;
    ClientUploadHandler(ListOfClients.Client client, Broadcast broadcast){
            ID = 0;
            type = 0;
            this.broadcast = broadcast; 
            this.client = client;
    }
    
    
    @Override
    public void run(){
         try{
            client.OS = new BufferedOutputStream(client.socket.getOutputStream());
            OSData = new DataOutputStream(client.OS);
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
        
        int partNr;
        for(;;){
             
             //w8 for upload job
             byte typeToSend = (byte) client.TakeUlQue();
             if(typeToSend==0)//some sort of error
                 continue;
             
             switch(typeToSend){
                 
                 case 'a'://send key list to client
                     client.SendListOfParts('a');
                     break;
                 case 'l'://l = answer to key request
                     client.SendKey();
                     break;
                 case 'x'://x = ask wether the client want a certein part
                     partNr = client.TakeUlQue();
                     client.AskSendPart(partNr);
                     break;
                 case 'q'://q = ask which parts a node has
                     client.SendListOfParts('q');
                     break;
                 case 's':
                     //TODO send stats
                     break;
                 case 'g'://send goodbye notice
                     //TODO
                     break;
                 case 'f'://send corruption complaint
                     //TODO
                     break;
                 case 'b'://send ban notice to client
                     //TODO
                     break;
                 case 'k'://request public key from some one
                     //TODO
                     break;
                 case 'd'://tell the client i dont want that part
                     partNr = client.TakeUlQue();
                     client.DeclinePartRq(partNr);
                     break;
                 case 'c'://confirms i want the part
                     partNr = client.TakeUlQue();
                     if(partNr==-1)
                         break;//TODO some error managment
                     client.ConfirmPartRq(partNr);
                     break;
                 case 'p'://p = send part
                     partNr = client.TakeUlQue();
                     if(partNr==-1)
                         break; //TODO some error managment
                     Part part = broadcast.parts.allParts.get(partNr);
                     client.SendPart(part);
                     break;
                 default:
                     System.out.println("Wrong type in ClientUploadHandler"+typeToSend);
                     break;
                     
             }
             
         }
        
    }
}
