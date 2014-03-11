/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.io.BufferedInputStream;
import java.util.Arrays;
import java.util.TimerTask;

/**
 *
 * @author fisksoppa
 */

public class MakeParts extends TimerTask{
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
