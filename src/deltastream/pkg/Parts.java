/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;
import java.util.*;
import java.util.BitSet;
import java.util.Date;
import java.util.*;

/**
 * This is a object list of all the parts.
 * 
 * Each part is put into this list. 
 * 
 * @author Petter Tomner 
 */
//object with all the parts
public class Parts{
    
    class NewPartLock{
        boolean existNew;
        NewPartLock(){
            this.existNew = true; 
        }
    }
    
    long timeOut;//ms before a part should be killed
    //static Part[] allParts;//array of all parts
    Hashtable<Integer,Part> allParts;
    int newestPartId = 0; //the id of the last created part
    //int[] arrayOfPartN;
    
    BitSet allPartsBitSet;
    //byte[] allPartsBitSetAsByteArray;
    int oldestPartId = 1;
    int nOfParts = 0;
    int maxNOfParts = 200;
    NewPartLock newPartLock;
    /**
     *
     * a hashtable of default length is created
     */
    public Parts(){
        
        allParts = new Hashtable(500);  
        newPartLock = new NewPartLock();   
    }   

    /**
     *
     * @return
     */
    public synchronized BitSet GetPartsAsBitSet(){ //extremly ugly D:
        //TODO only do this when a part is updated ... get allOartsBitSet instead
        BitSet bitSet = new BitSet(allParts.size());//borde ju va största id - minsta ...
        int i = 0;
        for(Map.Entry<Integer, Part> entry : allParts.entrySet()){
            int tmp = entry.getKey()-oldestPartId;
            if ( tmp >= 0) //to avoid bitset of -x
                bitSet.set(tmp);   
        }       
        return bitSet;  
    }
    
    /**
     *
     * @return
     */
    
    
    public synchronized byte[] GetPartsAsBitSetByteArray(){ //extremly ugly D:
        
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
    
    /**
     *
     * @param part
     */
    synchronized public void PutPart(Part part){
        //put a specific part in the allParts
        try{
            if(part.partN<oldestPartId){//check wether the acuired part is too old
                System.out.println("Part to old");
                return;}

           //remeber to add check for timelimit here!!
            if(allParts.containsKey(part.partN)){
                //System.out.println("Has that part");
                return;}

            allParts.put(part.partN, part);
            nOfParts++;
            //System.out.println("Saved part"+part.partN);
            if(nOfParts>maxNOfParts){ //if too many parts
                while(allParts.remove(oldestPartId++)==null)
                    ; //remove one TODO sync so only one oldest path etc 
                //oldestPartId++;    //new oldest part
                nOfParts--;        //remove one from count  
            }
            synchronized(this.newPartLock){
                this.newPartLock.existNew = true;
                this.newPartLock.notify();
            }
            
        }
        catch(Exception ee){
                System.out.println("Error in putpart "+ee);
        }
              
    }

    /**
     *
     * @param data
     */
    synchronized public void Put(byte data[]){
        //put part in the oldest part's place
        Part part = new Part(newestPartId++,data);  //
        allParts.put(part.partN, part);             //put the part in the mapping table
        if(++nOfParts>maxNOfParts){                  //remove the oldest part if buffer is full
            synchronized(allParts.get(oldestPartId)){
                allParts.remove(oldestPartId++);
            }}
        synchronized(this.newPartLock){
            this.newPartLock.existNew = true;
            this.newPartLock.notify();
        } 
    }    
    
    
}

