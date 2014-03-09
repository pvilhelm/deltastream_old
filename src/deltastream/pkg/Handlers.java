/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.util.*;
import java.io.DataInputStream;
import java.io.*;
import java.net.*;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

class UploadHandler implements Runnable{
    //this class handles the uploads to the clients
    
    
    Broadcast broadcast;
    UploadHandler(Broadcast broadcast){
        this.broadcast = broadcast;
    }
    
    @Override
    public void run(){
    
    
    //this one uses port 82 (another port) then the client handlar
        for(;;){   
            try{
                Thread.sleep(5); //varför protesterar den ...
            }
            catch(Exception ee){
                System.out.println("Couldnt sleep in uploadhandler");
            }
            //pick random client according to some smart algoritm
            
            ListOfClients.Client clientToUploadTo;
            try{
                clientToUploadTo = broadcast.listOfClients.PickClientForUpload();
            }
            catch(Exception ee){
                continue;
            }
            if(clientToUploadTo==null)
                continue;
            
            if(!clientToUploadTo.connected && (clientToUploadTo.lastTriedToConnetcTo.getTime()+2000<(new Date()).getTime())){
                if(clientToUploadTo.ulThread==null || !clientToUploadTo.ulThread.isAlive()){ //check if a upload thread is alive or make on
                    synchronized(clientToUploadTo){
                        if(clientToUploadTo.ulThread==null || !clientToUploadTo.ulThread.isAlive()){
                            Thread thread = new Thread(new ConnectToClient(clientToUploadTo,broadcast),"Make connection thread");
                            
                            thread.start();
                        }
                    }
                }
            }
            
            if(clientToUploadTo.uploadQue.size()>12) // TODO <------------- make this changeable from config
                continue;

            
            long time = new Date().getTime();
            if(clientToUploadTo.lastBitSet.getTime()+5000< time){
                if(!clientToUploadTo.uploadQue.contains(113) && (clientToUploadTo.lastPartListRequested.getTime() + 2000 < time))
                    clientToUploadTo.PutUlQue('q'); //TODO if it doenst put in queue, dont do rest!!!
                
                continue;
            }
            //pick random part it needs

            int partToUploadNr= clientToUploadTo.PickPartItNeeds();
            if(partToUploadNr==-1)
                continue;
            
            
                
            clientToUploadTo.PutUlQue('x',partToUploadNr); 
        }
    }
}   


class ConnectToClient implements Runnable{
    
    ListOfClients.Client client;
    Broadcast broadcast;
    
    ConnectToClient(ListOfClients.Client client, Broadcast broadcast){
        this.client = client;
        this.broadcast = broadcast;
    }
    
    @Override
    public void run(){
        
        synchronized(client){ //TODO should be "thread making lock" instead
            if(client.dlThread.isAlive())
                return;
            if(client.ulThread.isAlive())
                return;
            
            if(client.socket == null || !client.socket.isConnected() || client.socket.isClosed()){
                try{
                    Socket socket =  new Socket(client.IP,Config.clientServerSocketPort);
                    client.socket = socket;
                    client.OS = new BufferedOutputStream(socket.getOutputStream());
                    client.IS = new BufferedInputStream(socket.getInputStream());
                    client.socket.setSoTimeout(0);//TODO remeber to remove/set lower
                }
                catch(Exception ee){
                    System.out.println("Couldnt create streams in ConnectedToClient");
                    client.unsucessfullRcAttempts++;
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


class MakeParts extends TimerTask{
    Broadcast broadcast;
    BufferedInputStream internalInputStream;
    byte[] buffer;
    
    MakeParts(BufferedInputStream internalInputStream, Broadcast broadcast, byte[] buffer){
        this.broadcast = broadcast;
        this.internalInputStream = internalInputStream;
        this.buffer = buffer;
    }
    
    @Override
    public void run(){
        int nrOfBytes;
        try{
            nrOfBytes = internalInputStream.available();
        }
        catch(Exception ee)
        {
            System.out.println("Coulnt read how many bytes avaible in make parts task");
            return;
        }
        
        try{
            internalInputStream.read(buffer, 0, nrOfBytes);
        }
        catch(Exception ee){
            System.out.println("Couldnt read buffer in making parts");
        }
        broadcast.parts.Put(Arrays.copyOfRange(buffer,0, nrOfBytes));
        
        /*try{
            System.out.println(buffer[0] +" "+ nrOfBytes);
        }
        catch(Exception ee){
            ;
        }*/
    }
}

class MakePartsUDP extends TimerTask{
    Broadcast broadcast;
    BufferedInputStream internalInputStream;
    byte[] buffer;
    
    MakePartsUDP(BufferedInputStream internalInputStream, Broadcast broadcast, byte[] buffer){
        this.broadcast = broadcast;
        this.internalInputStream = internalInputStream;
        this.buffer = buffer;
        
    }
    
    @Override
    public void run(){
        
        DataInputStream dataIn = new DataInputStream(internalInputStream);
        
        int nrOfBytes;
        try{
            nrOfBytes = internalInputStream.available();
        }
        catch(Exception ee)
        {
            System.out.println("Coulnt read how many bytes avaible in make parts task");
            return;
        }
        int readBytes = 0;
        int datagramL;
        try{
            if(broadcast.oldDatagramLength!=0){
                datagramL = broadcast.oldDatagramLength;
                broadcast.oldDatagramLength = 0;
                if(datagramL<=nrOfBytes)
                        readBytes += internalInputStream.read(buffer, 0, nrOfBytes);

                }

            for(;readBytes+2<nrOfBytes;){
                datagramL = dataIn.readShort();
                readBytes+=2;
                
                if(readBytes+datagramL<=nrOfBytes)
                    readBytes += internalInputStream.read(buffer, readBytes, nrOfBytes);
                else{//not enough data in input stream
                    broadcast.oldDatagramLength = datagramL;
                    break;
                }
            }
        }
        catch(Exception ee){
            System.out.println("Couldnt read buffer in making parts");
        }
        broadcast.parts.Put(Arrays.copyOfRange(buffer,0, nrOfBytes));
        
        /*try{
            System.out.println(buffer[0] +" "+ nrOfBytes);
        }
        catch(Exception ee){
            ;
        }*/
    }
}

class ReadInputStream implements Runnable{
    //här ska instreamen samplas ... 
    InputStream BufferedInDataStream;
    Broadcast broadcast;
    
    ReadInputStream(InputStream BufferedInDataStream, Broadcast broadcast){
        this.BufferedInDataStream = BufferedInDataStream;
        this.broadcast = broadcast;
    }
    
    @Override 
    public void run(){
        System.out.println("Creating part");
        byte[] buffer = new byte[10000000];
        byte[] buffer2 = new byte[10000000];
        
        int nBytesInBuffer = 0; 
        PipedOutputStream internalOutputStream;
        PipedInputStream internalInputStream;
        BufferedOutputStream internalOutputStreamBuffered;
        BufferedInputStream internalInputStreamBuffered;
        try{
            internalOutputStream = new PipedOutputStream();
            internalOutputStreamBuffered = new BufferedOutputStream(internalOutputStream,2000);
            
            internalInputStream = new PipedInputStream(internalOutputStream,10000000);
            internalInputStreamBuffered = new BufferedInputStream(internalInputStream,1000000);    
             
        }
        catch(Exception ee){
            System.out.println("Couldnt connect piped streams");
            return;
        }

        TimerTask makeParts = new MakeParts(internalInputStreamBuffered, broadcast, buffer2);
        Timer SampleTimer = new Timer(); //sets timer
        SampleTimer.schedule(makeParts,broadcast.samplingPeriod,broadcast.samplingPeriod); //starts timer event
        
        for(;;){
            try{
                nBytesInBuffer = BufferedInDataStream.available();
                 
            }
            catch(Exception ee){
                System.out.println("Couldn't read how many bytes on buffer in instream: "+ee);
                return;
            }

            try{
                BufferedInDataStream.read(buffer,0,nBytesInBuffer); //maybe it doesnt read that many bytes??
            }
            catch(Exception ee){
                System.out.println("Couln't read instreambuffer: "+ee);
                return;
            }
            try{
                internalOutputStreamBuffered.write(buffer,0,nBytesInBuffer);
            }
            catch(Exception ee){
                System.out.println("Couldnt write to internal piped stream");
            }
            //saves the data in a part
            

            
        }
    }
}

class ReadInputStreamUDP implements Runnable{
    //här ska instreamen samplas ... 
    
    Broadcast broadcast;
    
    ReadInputStreamUDP(Broadcast broadcast){
        
        this.broadcast = broadcast;
    }
    
    @Override 
    public void run(){
        for(;;){
            try{
                broadcast.inputSSS = new ServerSocket(81); //creates a server socked that accepts i stream of some sort
                broadcast.inputSSS.setReceiveBufferSize(10000);
            }
            catch(Exception ee){
                System.out.println("Couln't create Server Socket: " + ee);
                return; 
            }

            Socket s;           //the stream connection
            InputStream InDataStream;   //the internal stream
            InputStream bufferedInDataStream; //d:o buffered

            try{       
                s = broadcast.inputSSS.accept(); //accepts a connection on that socket
                s.setSendBufferSize(2000);
                s.setReceiveBufferSize(1000000);
                System.out.println("Stream source connected on port: " + s.getPort()+" from IP: "+s.getInetAddress());
                s.setSoTimeout(0); ///<--remeber to remove
            }
            catch(Exception ee){
                System.out.println("Couln't accept stream connection: " + ee);
                return; 
            }

            try{
                InDataStream = s.getInputStream();
            }
            catch(Exception ee){
                System.out.println("Couln't get input stream from stream connection: " + ee);
                return; 

            }
            bufferedInDataStream = new BufferedInputStream(InDataStream);


            System.out.println("Creating part");
            byte[] buffer = new byte[10000000];
            byte[] buffer2 = new byte[10000000];


            PipedOutputStream internalOutputStream;
            PipedInputStream internalInputStream;
            BufferedOutputStream internalOutputStreamBuffered;
            BufferedInputStream internalInputStreamBuffered;
            try{
                internalOutputStream = new PipedOutputStream();
                internalOutputStreamBuffered = new BufferedOutputStream(internalOutputStream,2000);

                internalInputStream = new PipedInputStream(internalOutputStream,10000000);
                internalInputStreamBuffered = new BufferedInputStream(internalInputStream,1000000);    

            }
            catch(Exception ee){
                System.out.println("Couldnt connect piped streams");
                break;
            }

            TimerTask makePartsUDP = new MakePartsUDP(internalInputStreamBuffered, broadcast, buffer2);
            Timer SampleTimer = new Timer(); //sets timer
            SampleTimer.schedule(makePartsUDP,broadcast.samplingPeriod,broadcast.samplingPeriod); //starts timer event
            DataInputStream dataIn = new DataInputStream(bufferedInDataStream);
            DataOutputStream dataOutInternal = new DataOutputStream(internalOutputStreamBuffered);
            
            int datagramL;

            for(;;){

                try{

                    datagramL = dataIn.readShort();
                    bufferedInDataStream.read(buffer,0,datagramL);
                }
                catch(Exception ee){
                    System.out.println("Couln't read instreambuffer: "+ee);
                    break;
                }
                try{ 
                    dataOutInternal.writeShort(datagramL);
                    internalOutputStreamBuffered.write(buffer,0,datagramL);
                }
                catch(Exception ee){
                    System.out.println("Couldnt write to internal piped stream");
                    break;
                }
                //saves the data in a part



            }
            SampleTimer.cancel();
            try{
                broadcast.inputSSS.close();
                s.close();
            }
            catch(Exception ee){
                ;
            }
        }
    }
}