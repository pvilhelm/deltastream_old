/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author fisksoppa
 */

public class ReadInputStream implements Runnable{
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