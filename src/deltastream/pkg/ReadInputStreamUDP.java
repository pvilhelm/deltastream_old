/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Reads the TCP-FORWARDED UDP input stream on the server.
 * 
 * Then sends the stream through a pipe {@link MakePartsUDP}, which makes parts of the
 * stream at a specified time interval. 
 * 
 * @author Petter Tomner 
 */

public class ReadInputStreamUDP implements Runnable{
    //här ska instreamen samplas ... 
    
    Broadcast broadcast;
    /**
     * 
     * @param broadcast the broadcast this stream belongs to
     */
    public ReadInputStreamUDP(Broadcast broadcast){
        
        this.broadcast = broadcast;
    }
    
    @Override 
    public void run(){
        for(;;){
            broadcast.oldDatagramLength = 0;
            try{
                broadcast.inputSSS = new ServerSocket(1081); //creates a server socked that accepts i stream of some sort
                broadcast.inputSSS.setReceiveBufferSize(100000);
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
                s.setReceiveBufferSize(100000);
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
                    internalOutputStreamBuffered.flush();
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
