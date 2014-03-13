
package deltastream.pkg;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.security.*;

/**
 * This is a UDP stream output from the application.
 * 
 * Connect the UDP stream to w/e and it will be the same stream that was put into
 * the server. 
 * 
 * @author Petter Tomner
 */

class OutputStreamServerUDP implements Runnable{
    // This class provides a server for putting the parts together and outputting the input stream
    
    Parts parts;
    Broadcast broadcast; 
    DatagramSocket serverSocket;
    /**
     * 
     * 
     * @param thisBroadcast the broadcast this output stream belongs to
     */
    public OutputStreamServerUDP(Broadcast thisBroadcast){
        
        broadcast = thisBroadcast;     
        parts = broadcast.parts; 
    }
    
    @Override
    public void run(){
        
 
        for(;;){//listen for incoming output socket request    
            try{
                serverSocket = new DatagramSocket();
                
                System.out.println("Will sen output UDP stream from local port:"+serverSocket.getLocalPort());
            }
            catch(Exception ee){
                System.out.println("Coulnt create output server socket"+ee);
            }
            SocketAddress remoteAddr = new InetSocketAddress(Config.remoteUDPOutputStreamIP,Config.remoteUDPOutputStreamPort);
            System.out.println("Sending Output UDP data to remote port:"+Config.remoteUDPOutputStreamPort+" at IP:"+Config.remoteUDPOutputStreamIP);
            
            
            byte[] buffer = new byte[15000000];
            int[] datagramL = new int[10000];
            int partToGet = broadcast.parts.oldestPartId+90;
            Date errorN = new Date();
            Date old = new Date();
            Date neew = new Date();
            long lastSentPart = System.currentTimeMillis(); // old current time
            long currentTimeMs = System.currentTimeMillis(); //current time
            Part oldPart = new Part(-1,null); //pointer to the prior sent part
            Part part = new Part(-1,null);
            for(;;){ 
                
                long tmp = oldPart.timeCreated+10000-System.currentTimeMillis();
                try{
                    if(tmp>0)
                        Thread.sleep(tmp);
                }
                catch(Exception ee){
                    System.out.println("couldnt pause"+ee+"67");
                }
                
                if(oldPart.timeCreated+10000<System.currentTimeMillis()){
                //if(partToGet <= broadcast.parts.oldestPartId+90){
               
                    
                    
                    if(broadcast.parts.allParts.containsKey(partToGet)){                                        
                        errorN = new Date();
                        
                        oldPart = part; 
                        part = broadcast.parts.allParts.get(partToGet);
                        ByteArrayInputStream bAInputStream = new ByteArrayInputStream(part.data);
                        DataInputStream dataInStream = new DataInputStream(bAInputStream);
                        
                        try{ //TODO make the protocoll make the udp stream perfect identical timing
                            int datagramCount = 0;
                            int readBytes = 0; 
                            while(bAInputStream.available()!=0){
                                datagramL[datagramCount] = dataInStream.readUnsignedShort();
                                readBytes += bAInputStream.read(buffer,readBytes,datagramL[datagramCount]);
                                datagramCount++;
                            }
                            
                            int waitMs = (int) (Config.UDPBroadcastWaitCoeff*(double)Config.SamplingPeriod/(double)datagramCount);
                            
                            readBytes = 0;
                            for(int i = 0; i<datagramCount;i++){
                                DatagramPacket packet = new DatagramPacket(buffer,readBytes,datagramL[i]); 
                                readBytes += datagramL[i];
                                packet.setSocketAddress(remoteAddr);
                                serverSocket.send(packet);
                                
                                try{
                                    //Thread.sleep(waitMs);
                                }
                                catch(Exception ee){
                                    System.out.println("Coudlnt wait the specified amount of ms in UDP stream output"+ee);
                                }
                            }
                            partToGet++;
                            if(old.getTime()-neew.getTime()<-800)
                                    old=old;//dummy instruction 
                            
                            
                            currentTimeMs = System.currentTimeMillis();
                            long sendTime = currentTimeMs - lastSentPart; 
                            lastSentPart = currentTimeMs; 
                            long originalCreationTimeSpan = part.timeCreated -oldPart.timeCreated;
                            long diff = originalCreationTimeSpan - sendTime;
                            
                            
                            old = neew;
                            neew = new Date();
                                                        
                            if(diff>0 && diff < broadcast.samplingPeriod*3){
                                Thread.sleep(diff); //sleeping to make sure we dont "catch up" with the parts
                            }
                            System.out.println((old.getTime()-(new Date()).getTime()) + " " +(old.getTime()-neew.getTime())+" : "+originalCreationTimeSpan+" read bytes: "+readBytes+" partNr"+part.partN+" diff part to newest"+(broadcast.parts.oldestPartId-part.partN));
  
                            
                        }
                        catch(Exception ee){
                            System.out.println("Couldnt write to output. "+ee.toString());
                            break;
                        }
                        
                    }
                    else{//if we dont have a new enough part, wait for another one
                        synchronized(broadcast.parts.newPartLock){ //this makes sure we only test conditions while we have a new part
                            while(!broadcast.parts.newPartLock.existNew){
                                try{
                                    broadcast.parts.newPartLock.wait();    
                                }
                                catch(Exception ee){
                                    System.out.println("Exception: Couldnt wait for new part lock"+ee);
                                }
                            }
                            broadcast.parts.newPartLock.existNew = false;
                        }
                    }
                }
                if(errorN.getTime()+5000<(new Date()).getTime()){
                    errorN = new Date();
                    partToGet = broadcast.parts.oldestPartId+90;//nollställer
                    System.out.println("Resetting output stream part number to:"+partToGet);
                }                            
            }
            try{
                Thread.sleep(4000);
            }
            catch(Exception ee){;}
            if(serverSocket!=null)
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
