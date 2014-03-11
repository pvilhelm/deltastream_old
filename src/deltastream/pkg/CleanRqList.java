/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.util.*;
import java.io.DataInputStream;
import java.io.*;
import java.net.*;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author fisksoppa
 */
public class CleanRqList extends TimerTask{
    Broadcast broadcast; 
    CleanRqList(Broadcast broadcast){
        this.broadcast = broadcast;
    }
    
    @Override
    public void run(){
        try{
            int size = broadcast.requestedParts.size();
            for(int i=0;i<size/2;i++){
                broadcast.requestedParts.remove();
            }
            while(broadcast.requestedParts.size() > Config.maxRqPartsQueSize)
                broadcast.requestedParts.remove();
        }
        catch(Exception ee){
            System.out.println("Error: Que requestedParts empty, tried to remove head anyways");
        }
    }    
}
