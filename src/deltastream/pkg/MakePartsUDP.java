/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.TimerTask;

/**
 * Makes parts out of a UDP stream buffer.
 * 
 * The piped stream from @see ReadInputStreamUDP is read by this class instance and then made into a part, 
 * ready to be transmitted to the other clients. 
 * <p>
 * Instances of this class is spawned by a timer at intervals corresponding to 
 * sample time provided by @see Config. 
 * <p>
 * The parts are made in such a way that a UDP datagram is not split up between 
 * two parts.
 * 
 * @see ReadInputStream
 * @author Petter Tomner
 */

public class MakePartsUDP extends TimerTask{
    Broadcast broadcast;
    BufferedInputStream internalInputStream;
    byte[] buffer;
     /**
     * 
     * @param internalInputStream the piped stream from ReadInputStream
     * @param broadcast the broadcast this parts belongs to
     * @param buffer a buffer provided (to help the poor garbage garbage collector)
     */
    public MakePartsUDP(BufferedInputStream internalInputStream, Broadcast broadcast, byte[] buffer){
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
        
        ByteArrayOutputStream bAOutputStream = new ByteArrayOutputStream(1000000);
        DataOutputStream dataOutStream = new DataOutputStream(bAOutputStream);
        
        int readBytes = 0;
        int datagramL;
        try{
            if(broadcast.oldDatagramLength!=0){
                datagramL = broadcast.oldDatagramLength;
                broadcast.oldDatagramLength = 0;
                if(datagramL<=nrOfBytes){
                    dataOutStream.writeShort(datagramL);
                    //readBytes+=2;
                    readBytes += internalInputStream.read(buffer, 0, datagramL);
                    bAOutputStream.write(buffer, 0, datagramL);
                    
                }
                else{//för lite data igen
                    broadcast.oldDatagramLength = datagramL;
                    return;
                }
            }
            for(;readBytes+2<nrOfBytes;){
                datagramL = dataIn.readShort();
                readBytes+=2;
                if(readBytes+datagramL<=nrOfBytes){
                    dataOutStream.writeShort(datagramL);
                    
                    readBytes += internalInputStream.read(buffer, 0, datagramL);
                    bAOutputStream.write(buffer, 0, datagramL);
                }
                else{//not enough data in input stream
                    broadcast.oldDatagramLength = datagramL;
                    break;
                }
            }
        }
        catch(Exception ee){
            System.out.println("Couldnt read buffer in making parts");
        }
        broadcast.parts.Put(bAOutputStream.toByteArray());
        
        /*try{
            System.out.println(buffer[0] +" "+ nrOfBytes);
        }
        catch(Exception ee){
            ;
        }*/
    }
}

