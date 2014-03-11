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
 * Makes parts out of a stream buffer.
 * 
 * The piped stream from @see ReadInputStream is read by this class instance and then made into a part, 
 * ready to be transmitted to the other clients. 
 * <p>
 * Instances of this class is spawned by a timer at intervals corresponding to 
 * sample time provided by @see Config. 
 * 
 * @author Petter Tomner
 * 
 */

public class MakeParts extends TimerTask{
    Broadcast broadcast;
    BufferedInputStream internalInputStream;
    byte[] buffer;
    
    /**
     * 
     * @param internalInputStream the piped stream from ReadInputStream
     * @param broadcast the broadcast this parts belongs to
     * @param buffer a buffer provided (to help the poor garbage garbage collector)
     */
    
    
    public MakeParts(BufferedInputStream internalInputStream, Broadcast broadcast, byte[] buffer){
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
