/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.*;
import java.util.concurrent.*;

class ListOfClients{
    Broadcast broadcast;
    Random rand;
    int nClients; //number of clients in list
    Hashtable<String,Client> clientHashtable;
    ListOfClients(Broadcast broadcast){
        //allClients = new Client[1000];//waste of space ...
        clientHashtable = new Hashtable(1400);
        this.broadcast = broadcast; 
        rand = new Random();
    } 
    
    Client AddClient(String key){
        Client client = new Client(broadcast);
        client.IP = key;
        clientHashtable.put(key, client);
        return client;
    }
    void RemoveClient(Client client){
        clientHashtable.remove(client.IP);
        client.Drop();
        //client = null; //är detta nödvändigt? ... D:
    }
    
    Client PickClientForUpload(){
        //picks a client for a upload of a part
        //as this is a server it will pick kinda random
        //but clients should pick clients that deserves uploads
        Object[] values = clientHashtable.values().toArray();
        if(values.length==0)
            return null;
        return (Client) values[rand.nextInt(values.length)];
         
    }
            
    class Client{
        Object lockUPDL; //all things that wanna use this client for uploading/dling MUST use this
        Boolean willBeDroped;
        BufferedInputStream IS;
        BufferedOutputStream OS;
        InputStream IS2;
        OutputStream OS2;
        String IP;
        BlockingQueue<Integer> uploadQue;
        BlockingQueue<Integer> downloadQue;
        //BlockingQueue<Integer> partWishList;
        Date firstContact;
        Date lastContact;
        Date lastBitSet;
        Date lastPartListRequested;
        Date lastPartSentToClient;
        Date lastPartAquiredFromClient;
        Date lastKeyRqSentToClient;
        Date lastTriedToConnetcTo;
        BitSet bitFieldParts;
        int partIdOffset;
        Broadcast broadcast; //the broadcast the client belongs to
        int downloadedParts; //how many part I downloaded from him
        int uploadedParts; //how many parts he dowloaded from me
        Part partToUpload;
        Socket socket;
        int clientSessionId;
        Thread ulThread;
        Thread dlThread;
        Boolean connected = false; 
        int unsucessfullRcAttempts = 0;
        
        Client(Broadcast broadcast){
            lastContact = new Date(0); //when the client connected
            firstContact = new Date(0);
            lastPartSentToClient = new Date(0);
            lastPartListRequested = new Date(0);
            lastTriedToConnetcTo = new Date(0);
            this.broadcast = broadcast;
            bitFieldParts = new BitSet(100);
            lockUPDL = new Object();
            downloadQue = new LinkedBlockingQueue(100);
            uploadQue = new LinkedBlockingQueue(100);
            lastBitSet = new Date(0);
            clientSessionId = rand.nextInt();
            
        }
        
        synchronized boolean PutDlQue(int type){
            try{
                uploadQue.put(type);
            }
            catch(Exception ee){
                System.out.println("couldnt put in que");
            }
            return true; //TODO error echeck
        }
        
        int GetDlQue(){
            try{
                return downloadQue.take();
            }
            catch(Exception ee){
                System.out.println("Coulnt get post from DL que");
                return -1;//TODO check returning error
            }
             
        }
        
        /* IsUlQueFull(int n){//checks if u can add n posts 
            return uploadQue.remainingCapacity()>=n;
        }*/
        
        synchronized boolean PutUlQue(int type){
            try{
                uploadQue.put(type);
                return true;
            }
            catch(Exception ee){
                //System.out.println("Que ul full");
                return false;
            }
        }
        
        synchronized boolean PutUlQue(int first, int sec){
            try{
                uploadQue.put(first);
                uploadQue.put(sec);
                return true;
            }
            catch(Exception ee){
                //System.out.println("Que ul full");
                return false;
            }
        }
        
        int TakeUlQue(){
            try{
                return uploadQue.take();
            }
            catch(Exception ee){
                System.out.println("Couldnt wait for upload que"+ee);
                return -1;
            }
        }
        
        void ConfirmPartRq(int partNr){
            DataOutputStream outData = new DataOutputStream(OS);
            try{
                outData.writeLong(broadcast.broadcastId); //writes broadcast ID
                outData.writeByte('c');//type of message
                outData.writeInt(partNr);
                outData.flush();
                //System.out.println("confirmed part rq");
            }
            catch(Exception ee){
                System.out.println("Couldnt send send rq to client"+ee);   
                this.Drop();
            }  
        }
        
        void DeclinePartRq(int partNr){
            DataOutputStream outData = new DataOutputStream(OS);
            try{
                outData.writeLong(broadcast.broadcastId); //writes broadcast ID
                outData.writeByte('d');//type of message
                outData.writeInt(partNr);
                outData.flush();
                //System.out.println("Declined part rq");
            }
            catch(Exception ee){
                System.out.println("Couldnt send decline rq to client"+ee);    
                this.Drop();
            }     
        }
        
        void AskSendPart(int PartN){
            //check if the client wants the part        
            DataOutputStream outData = new DataOutputStream(OS);
            try{
                outData.writeLong(broadcast.broadcastId); //writes broadcast ID
                outData.writeByte('x');//type of message
                outData.writeInt(PartN);
                outData.flush();
                //System.out.println("Ask if he wants part: "+PartN);
                lastPartListRequested = new Date();
            }
            
            catch(Exception ee){
                System.out.println("Couldnt send send rq to client"+ee);
                this.Drop();
            }
        }
        
        int PickPartItNeeds(){
            //pick a part this client need weighted after how much it needs it
            // TODO - the wighting ... later parts should be wieghted
            // TODO - this should be redone ... so that prior to each part exchange
            //there always be a trading of partsliost
            int nBits = bitFieldParts.length();
            int myOldestPartId;
            int myNewestPartId;
            BitSet myPartsBitSet;
            synchronized(broadcast.parts){
                myOldestPartId = broadcast.parts.oldestPartId;
                myNewestPartId = broadcast.parts.newestPartId;
                myPartsBitSet = broadcast.parts.GetPartsAsBitSet();
            }
            if(myPartsBitSet.isEmpty()){
                System.out.println("MyBitset empty");
                return -1;
            }
            int diff;
            synchronized(bitFieldParts){
                diff = myOldestPartId-partIdOffset;
                if(myNewestPartId<partIdOffset)
                    return -1;//i have no parts that he would want

                
                if(diff>=0){
                    for(int i=0;i<bitFieldParts.length()-diff;i++){
                        if(!((myPartsBitSet.get(i) ^ bitFieldParts.get(i+diff)) & myPartsBitSet.get(i)))
                            myPartsBitSet.clear(i); 
                    } 

                }
                else{
                   myPartsBitSet.clear(0, diff);

                   for(int i=0;i<myPartsBitSet.length()-diff;i++){
                        if(!((myPartsBitSet.get(i+diff) ^ bitFieldParts.get(i)) & myPartsBitSet.get(i+diff)))
                            myPartsBitSet.clear(i+diff);  
                    }    
                }
            }
            int offset=0;
            if(diff<0)
                offset = diff;
            
            int indexRand = rand.nextInt(myNewestPartId-myOldestPartId-offset)+offset;
            
            int partNrRnd = myPartsBitSet.previousSetBit(indexRand);
            if(partNrRnd==-1)
                partNrRnd = myPartsBitSet.nextSetBit(indexRand);
            return partNrRnd+myOldestPartId;
        }
        
        void UpdateLastContact(){
            lastContact = new Date(); 
        }
        
        void SendPart(int partNr){
            //send part to client
            DataOutputStream outData = new DataOutputStream(OS);
             
            Part partUl = broadcast.parts.allParts.get(partNr);
            if(partUl==null){
                System.out.println("Part doesnt exist:" +partNr);
                return;
            }
            int partN; long timeCreated;
            byte[] dataCopy;
            
            synchronized(partUl){//reads the data so it wont block remove
                partN = partUl.partN;
                dataCopy = Arrays.copyOf(partUl.data, partUl.data.length);
                timeCreated = partUl.timeCreated;
            }
            
            try{
                outData.writeLong(broadcast.broadcastId);
                outData.writeByte('p');
                outData.writeInt(partN);
                outData.writeLong(timeCreated);
                //outData.writeLong(0);
                outData.writeInt(dataCopy.length);
                //sign here
                OS.write(dataCopy);
                outData.flush();
                //System.out.println("Sent part: " + partN);
            }
            catch(Exception ee){
                System.out.println("Couldnt send part"+ee);
                //Close connection to client
                this.Drop();
                return;
            }
            this.uploadedParts++;
             
        } 

        synchronized void SendKey(){
            DataOutputStream OSData = new DataOutputStream(OS);
            try{
                OSData.writeLong(broadcast.broadcastId);
                OSData.writeByte('l'); //public key rq answer
                OSData.writeShort(1028);
                OSData.writeByte(1); //DSA == 1
                OSData.write(broadcast.pub.getEncoded());
                OSData.flush();
                System.out.println("Sent key");
            }
            catch(Exception ee){
                System.out.println("Couldnt send public key: "+ee);//
                this.Drop();
                return;
            }
            UpdateLastContact();
        }

        void SendListOfParts(char type){
            DataOutputStream OSData = new DataOutputStream(OS);
            try{
                //cache of the parts to be sent so they dont change during transmission
                int oldestPart; byte[] allPartsBitSetAsByteArray;
                
                synchronized(broadcast.parts){ 
                     oldestPart = broadcast.parts.oldestPartId; 
                     allPartsBitSetAsByteArray = broadcast.parts.GetPartsAsBitSetByteArray();
                }
                
                //måste clona sakerna som sänds så att de inte ändrar sig under tiden de sänds ... 
                OSData.writeLong(broadcast.broadcastId);
                OSData.writeByte(type);//'q' or 'a'
                OSData.writeInt(oldestPart);//sends offset
                OSData.writeShort(allPartsBitSetAsByteArray.length);//sends length of bitbytearray as short
                //OS.write(allPartsBitSetAsByteArray,0,allPartsBitSetAsByteArray.length);
                OSData.write(allPartsBitSetAsByteArray);
                OSData.flush();
                lastPartListRequested = new Date(); //update last time we req part list
                //System.out.println("Sent list of parts");
            }
            catch(Exception ee){
                System.out.println("coulndt provide answer to client about existing parts: "+ee);
                this.Drop();
                return;
            }
            UpdateLastContact();
        }
        
        /*void CreateGenConnTCP(){ //creates a tcp connection on general port with clietn
            try{
                Socket socket = new Socket(IP,broadcast.config.clientServerSocketPort);
                IS = new BufferedInputStream(socket.getInputStream());
                OS = new BufferedOutputStream(socket.getOutputStream());
                System.out.println("Connected to "+socket.getInetAddress().getHostAddress()+" at local port: "+socket.getLocalPort()+"to remote port:"+socket.getPort());
                socket.setSoTimeout(0); ///<--remeber to remove
            }
            catch(Exception ee){
                System.out.println("Couldnt create TCP connection with client"+ee.toString());
            }               
        }*/
        
        /*void CloseGenConnTCP(){
            try{
            IS.close();
            //OS.close();
            }
            catch(Exception ee){
                System.out.println("Coudlnt close socekts"+ee);
            }
        }*/
     
        
        void GetListOfParts(){
            //read incoming data save as parts for this client
            short lengthData = 0;


            DataInputStream ISData = new DataInputStream(IS);
            try{
                ISData.readLong();
                ISData.readByte();
                partIdOffset = ISData.readInt();
            }
            catch(Exception ee){
                System.out.println("Coulnt read length of array of bits of parts, connection error: "+ee);
                this.Drop();
                return;
            }
            try{
                
                lengthData = ISData.readShort();
                
            }
            catch(Exception ee){
                System.out.println("Coulnt read length of array of bits of parts, connection error: "+ee);
                this.Drop();
                return;
            }

            if(lengthData<0)
                return;
                
            byte[] arrByteBitParts = new byte[lengthData]; //read a little endian bit array of parts
            
            int readBytes = 0;
            try{
                while(readBytes<lengthData){
                    readBytes += IS.read(arrByteBitParts,readBytes,lengthData-readBytes);}
                if(readBytes!=lengthData)
                    throw new IOException();
            }
            catch(Exception ee){
                System.out.println("Couldnt read all the reqeusted parts"+ee+"Lengthdata:"+lengthData+" Readbytes: "+readBytes);
                this.Drop();
                return;
            }
            bitFieldParts = BitSet.valueOf(arrByteBitParts);
            UpdateLastContact();
            lastBitSet = new Date(); //new time for last bitset
            //System.out.println("Got list of parts");
             
        }
        void Drop(){
            //drop the client connection
            try{
                socket.close();
                connected = false; 
                System.out.println("Error: Closed connection with client with IP: "+this.IP);
                this.uploadQue.clear();
                this.downloadQue.clear();
                this.socket = null;
                this.OS = null;
                this.IS = null;
                 
                
            }
            catch(Exception ee){
                System.out.println("Problem closing socket for client in Thread:"+Thread.currentThread());
            }
            
            //broadcast.listOfClients.clientHashtable.remove(IP);
        }
    }
}
