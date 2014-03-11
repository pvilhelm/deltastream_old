/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.util.Date;

/**
 * Handles uploads for ANOTHER client.
 * 
 * A thread instance of this class is spawned to handle the uploads that is sent
 * to the remote client from the local client/server.
 *
 * @author Petter Tomner
 */

public class UploadHandler implements Runnable{
    //this class handles the uploads to the clients
    
    
    Broadcast broadcast;
    /**
     * 
     * @param broadcast the broadcast the handler belongs to
     */
    public UploadHandler(Broadcast broadcast){
        this.broadcast = broadcast;
    }
    
    @Override
    public void run(){
    
    
    //this one uses port 82 (another port) then the client handlar
        for(;;){   
            try{
                Thread.sleep(5); //varför protesterar den ...
            }
            catch(Exception ee){
                System.out.println("Couldnt sleep in uploadhandler");
            }
            //pick random client according to some smart algoritm
            
            ListOfClients.Client clientToUploadTo;
            try{
                clientToUploadTo = broadcast.listOfClients.PickClientForUpload();
            }
            catch(Exception ee){
                continue;
            }
            if(clientToUploadTo==null)
                continue;
            
            if(!clientToUploadTo.connected && (clientToUploadTo.lastTriedToConnetcTo.getTime()+2000<(new Date()).getTime())){
                if(clientToUploadTo.ulThread==null || !clientToUploadTo.ulThread.isAlive()){ //check if a upload thread is alive or make on
                    synchronized(clientToUploadTo){
                        if(clientToUploadTo.ulThread==null || !clientToUploadTo.ulThread.isAlive()){
                            Thread thread = new Thread(new ConnectToClient(clientToUploadTo,broadcast),"Make connection thread");
                            
                            thread.start();
                        }
                    }
                }
            }
            
            if(clientToUploadTo.uploadQue.size()>12) // TODO <------------- make this changeable from config
                continue;

            
            long time = new Date().getTime();
            if(clientToUploadTo.lastBitSet.getTime()+5000< time){
                if(!clientToUploadTo.uploadQue.contains(113) && (clientToUploadTo.lastPartListRequested.getTime() + 2000 < time))
                    clientToUploadTo.PutUlQue('q'); //TODO if it doenst put in queue, dont do rest!!!
                
                continue;
            }
            //pick random part it needs

            int partToUploadNr= clientToUploadTo.PickPartItNeeds();
            if(partToUploadNr==-1)
                continue;
            
            
                
            clientToUploadTo.PutUlQue('x',partToUploadNr); 
        }
    }
}   


