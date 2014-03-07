/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;
import java.util.*;
import java.util.BitSet;
import java.util.Date;
import java.util.Hashtable;

/**
 *
 * @author servos
 */
//object with all the parts
class Parts{
    long timeOut;//ms before a part should be killed
    //static Part[] allParts;//array of all parts
    Hashtable<Integer,Part> allParts;
    int newestPartId = 0; //the id of the last created part
    int[] arrayOfPartN;
    
    BitSet allPartsBitSet;
    //byte[] allPartsBitSetAsByteArray;
    int oldestPartId = 1;
    int nOfParts = 0;
    int maxNOfParts = 200;
    
    Parts(int nOfParts){
        
        allParts = new Hashtable(500);  
        arrayOfPartN = new int[nOfParts];
        
        //allPartsBitSet = new BitSet(nOfParts);
        //allPartsBitSet.set(0, nOfParts);//create bit array with all 11111111s
        //allPartsBitSetAsByteArray = allPartsBitSet.toByteArray();
    }   
    synchronized BitSet GetPartsAsBitSet(){ //extremly ugly D:
        //TODO only do this when a part is updated ... get allOartsBitSet instead
        BitSet bitSet = new BitSet(allParts.size());
        int i = 0;
        for(Map.Entry<Integer, Part> entry : allParts.entrySet()){
            int tmp = entry.getKey()-oldestPartId;
            if ( tmp >= 0) //to avoid bitset of -x
                bitSet.set(tmp);   
        }       
        return bitSet;  
    }
    
    synchronized byte[] GetPartsAsBitSetByteArray(){ //extremly ugly D:
        
        BitSet bitSet = new BitSet(allParts.size());
        int i = 0;
        for(Map.Entry<Integer, Part> entry : allParts.entrySet()){
            int tmp = entry.getKey()-oldestPartId;
            if ( tmp >= 0) //to avoid bitset of -x
                bitSet.set(tmp);   
        }       
        byte[] answer =  bitSet.toByteArray();
        return answer;  
    }
    
    synchronized void PutPart(Part part){
        //put a specific part in the allParts
         if(part.partN<oldestPartId){//check wether the acuired part is too old
             System.out.println("Part to old");
             return;}
         
        //remeber to add check for timelimit here!!
         if(allParts.containsKey(part.partN)){
             System.out.println("Has that part");
             return;}
         
         allParts.put(part.partN, part);
         nOfParts++;
         System.out.println("Saved part"+part.partN);
         if(nOfParts>maxNOfParts){ //if too many parts
             synchronized(allParts.get(oldestPartId)){
                allParts.remove(oldestPartId); //remove one
             }
             oldestPartId++;    //new oldest part
             nOfParts--;        //remove one from count
         }      
    }
    
    synchronized void Put(byte data[]){
        //put part in the oldest part's place
        Part part = new Part(newestPartId++,data);  //
        allParts.put(part.partN, part);             //put the part in the mapping table
        if(++nOfParts>maxNOfParts)                  //remove the oldest part if buffer is full
            synchronized(allParts.get(oldestPartId)){
                allParts.remove(oldestPartId++);
            }
          
    }    
    
    
}

//single part 
class Part{
    int partN;  //de part number of the part
    byte[] data; //generic data container
    long timeCreated;//when the part was created
    
    Part(int thisPartN, byte thisData[]){
        timeCreated = new Date().getTime();
        partN = thisPartN;
        data = thisData; 
    }
    
 
}