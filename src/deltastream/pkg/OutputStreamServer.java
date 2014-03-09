 

package deltastream.pkg;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.security.*;
 
public class OutputStreamServer implements Runnable{
    // This class provides a server for putting the parts together and outputting the input stream
    
    Parts parts;
    Broadcast broadcast; 
    ServerSocket serverSocket;
    
    OutputStreamServer(Broadcast thisBroadcast){
        parts = broadcast.parts; 
        broadcast = thisBroadcast;      
    }
    
    @Override
    public void run(){
        try{
            serverSocket = new ServerSocket(Config.clientOutputServerSocketPort,0, InetAddress.getByName(null));
        }
        catch(Exception ee){
            System.out.println("Coulnt create output server socket"+ee);
        }
 
        for(;;){//listen for incoming output socket request    
            Socket outputSocket;
            DataOutputStream dataOutputStream;
            try{
                outputSocket = serverSocket.accept();
                dataOutputStream = new DataOutputStream(outputSocket.getOutputStream());
            }
            catch(Exception ee){
                System.out.println("Couldnt accept connection for output");
                break;
            }   
            
            
            int partToGet = broadcast.parts.oldestPartId+10;
            Date errorN = new Date();
            for(;;){//TODO add support for moar outputs and not only to 127.0.0.1
                
                if(partToGet != broadcast.parts.oldestPartId+10){
                    if(partToGet+1 != broadcast.parts.oldestPartId+10)
                        break;
                    errorN = new Date();
                    partToGet = broadcast.parts.oldestPartId+10;
                    Part part = broadcast.parts.allParts.get(partToGet);
                    try{
                        dataOutputStream.write(part.data);
                        dataOutputStream.flush();

                    }
                    catch(Exception ee){
                        System.out.println("Couldnt write to output. ");
                        break;
                        
                    }
                }
                else if(errorN.getTime()+5000<(new Date()).getTime()){
                    partToGet = broadcast.parts.oldestPartId+10;//nollställer
                    System.out.println("Resetting output stream part number to:"+partToGet);
                }
                
            }
        }     
    }      
}
