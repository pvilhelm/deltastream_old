/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

/**
 * A part of data sampled from the input stream at the source.
 * 
 * Each of this parts are transmitted to the other clients and then reforged
 * into the original UDP- or TCP-stream.
 * 
 * @author Petter Tomner
 */

public class Part{
    int partN;  //de part number of the part
    byte[] data; //generic data container
    long timeCreated;//when the part was created
    /**
     * 
     * @param thisPartN the number this part has, with the first part BY THE SOURCE created at index 0
     * @param thisData the data the part carries
     */
    public Part(int thisPartN, byte thisData[]){
        timeCreated = System.currentTimeMillis();
        partN = thisPartN;
        data = thisData; 
    }
    
 
}
