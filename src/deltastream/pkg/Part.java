/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.util.Date;

/**
 *
 * @author fisksoppa
 */

public class Part{
    int partN;  //de part number of the part
    byte[] data; //generic data container
    long timeCreated;//when the part was created
    
    Part(int thisPartN, byte thisData[]){
        timeCreated = new Date().getTime();
        partN = thisPartN;
        data = thisData; 
    }
    
 
}
