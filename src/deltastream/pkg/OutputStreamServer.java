
package deltastream.pkg;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.security.*;

class OutputStreamServerUDP implements Runnable{
    // This class provides a server for putting the parts together and outputting the input stream
    
    Parts parts;
    Broadcast broadcast; 
    DatagramSocket serverSocket;
    
    OutputStreamServerUDP(Broadcast thisBroadcast){
        
        broadcast = thisBroadcast;     
        parts = broadcast.parts; 
    }
    
    @Override
    public void run(){
        
 
        for(;;){//listen for incoming output socket request    
            try{
            serverSocket = new DatagramSocket(Config.localUDPOutputStreamPort);
            System.out.println("Will sen output UDP stream from port:"+Config.localUDPOutputStreamPort);
            }
            catch(Exception ee){
                System.out.println("Coulnt create output server socket"+ee);
            }
            SocketAddress remoteAddr = new InetSocketAddress(Config.remoteUDPOutputStreamIP,Config.remoteUDPOutputStreamPort);
            System.out.println("Sending Output UDP data to port:"+Config.remoteUDPOutputStreamPort+" at IP:"+Config.remoteUDPOutputStreamIP);
            
            
            byte[] buffer = new byte[65535];
            int partToGet = broadcast.parts.oldestPartId+50;
            Date errorN = new Date();
            for(;;){//TODO add support for moar outputs and not only to 127.0.0.1
                
                if(partToGet <= broadcast.parts.oldestPartId+50){
                    if(broadcast.parts.allParts.containsKey(partToGet)){                                        
                        errorN = new Date();
                        
                        Part part = broadcast.parts.allParts.get(partToGet);
                        ByteArrayInputStream bAInputStream = new ByteArrayInputStream(part.data);
                        DataInputStream dataInStream = new DataInputStream(bAInputStream);
                        
                        try{
                            while(bAInputStream.available()!=0){
                                int datagramL = dataInStream.readUnsignedShort();
                                bAInputStream.read(buffer,0,datagramL);
                                DatagramPacket packet = new DatagramPacket(buffer,0,datagramL); 
                                packet.setSocketAddress(remoteAddr);
                                serverSocket.send(packet);
                            }
                        }
                        catch(Exception ee){
                            System.out.println("Couldnt write to output. "+ee.toString());
                            break;
                        }
                        partToGet++;
                    }
                }
                if(errorN.getTime()+5000<(new Date()).getTime()){
                    errorN = new Date();
                    partToGet = broadcast.parts.oldestPartId+50;//nollställer
                    System.out.println("Resetting output stream part number to:"+partToGet);
                }                            
            }
            try{
                Thread.sleep(4000);
            }
            catch(Exception ee){;}
            serverSocket.close();
        }     
    }      
}
 
class OutputStreamServer implements Runnable{
    // This class provides a server for putting the parts together and outputting the input stream
    
    Parts parts;
    Broadcast broadcast; 
    ServerSocket serverSocket;
    
    OutputStreamServer(Broadcast thisBroadcast){
        
        broadcast = thisBroadcast;     
        parts = broadcast.parts; 
    }
    
    @Override
    public void run(){
        try{
            serverSocket = new ServerSocket(Config.clientOutputServerSocketPort,0, InetAddress.getByName(null));
            System.out.println("Listening for output connections on port:"+Config.clientOutputServerSocketPort);
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
                System.out.println("Connection on Outputstream from:"+outputSocket.toString());
                //dataOutputStream.writeUTF("Connected to Outputstream: Test \r\n");//TEST remeber to remove TODO TODO TODO
            }
            catch(Exception ee){
                System.out.println("Couldnt accept connection for output");
                break;
            }   
            
            
            int partToGet = broadcast.parts.oldestPartId+50;
            Date errorN = new Date();
            for(;;){//TODO add support for moar outputs and not only to 127.0.0.1
                
                if(partToGet <= broadcast.parts.oldestPartId+50){
                    if(broadcast.parts.allParts.containsKey(partToGet)){                                        
                        errorN = new Date();
                        
                        Part part = broadcast.parts.allParts.get(partToGet);
                        try{
                            dataOutputStream.write(part.data);
                            dataOutputStream.flush();

                        }
                        catch(Exception ee){
                            System.out.println("Couldnt write to output. ");
                            break;

                        }
                        partToGet++;
                    }
                }
                if(errorN.getTime()+5000<(new Date()).getTime()){
                    errorN = new Date();
                    partToGet = broadcast.parts.oldestPartId+50;//nollställer
                    System.out.println("Resetting output stream part number to:"+partToGet);
                    try{
                        dataOutputStream.writeUTF("Debugg debugg\r\n");
                    }
                    catch(Exception ee){
                        System.out.println("Coulnt write to terminal");    
                    }
                }                            
            }
        }     
    }      
}
