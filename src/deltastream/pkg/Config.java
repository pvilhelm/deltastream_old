
package deltastream.pkg;

 
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
 
/**
 * This class contains a default config for a broadcast.
 * 
 * Call method CreateBroadcast to create a broadcast. 
 * 
 * @author Petter Tomner
 */
public class Config {
    //this is a global conig class

    /**
     *
     */
        public static int inputStreamPort = 1081;  //the port listening the extrenal stream to be broadcasted

    /**
     *
     */
    public static int SamplingPeriod = 100;  //time in ms for sampling the input stream

    /**
     *
     */
    public static int timeLimit = 60*1000;   //how old a part is allowed to be in the broadcast

    /**
     *
     */
    public static int nOfParts = timeLimit/SamplingPeriod+10; //how many parts to save (internal use)

    /**
     *
     */
    public static int clientServerSocketPort = 1082;   //the port for generic com with clients

    /**
     *
     */
    public static int genericStreamBufferSize = 1000000; //the buffer size in bytes for some streams

    /**
     *
     */
    public static int clientOutputServerSocketPort = 1084 ;

    /**
     *
     */
    public static int maxRqPartsQueSize = 2000;

    /**
     *
     */
    public static int localUDPOutputStreamPort = 4444;

    /**
     *
     */
    public static int remoteUDPOutputStreamPort = 4444;

    /**
     *
     */
    public static String remoteUDPOutputStreamIP = "127.0.0.1";
    
    /**
     * 
     */
    
    public static double UDPBroadcastWaitCoeff = 0.8;
    
    Config(String[] args){
        //arguments go here TODO
    }
    
    Broadcast CreateBroadcast(){
        Broadcast broadcast = new Broadcast();
        broadcast.samplingPeriod = SamplingPeriod; /// <-------------------------------------------- hårdkodat
        broadcast.timeLimit = timeLimit; //hur gamla parts får bli
        Random rand = new Random();
        broadcast.broadcastId = rand.nextLong();
        broadcast.listOfClients = new ListOfClients(broadcast);
        broadcast.parts = new Parts(); //init the array of parts (i.e. the parts of a stream)
        broadcast.requestedParts = new LinkedBlockingQueue();
        
        //setup DSA signatures
        
        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
            
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(1024, random);
            KeyPair pair = keyGen.generateKeyPair();
            broadcast.priv = pair.getPrivate();
            broadcast.pub = pair.getPublic();
            broadcast.dsa = Signature.getInstance("SHA1withDSA"); 
            broadcast.dsa.initSign(broadcast.priv);
        }
        catch(Exception ee){
            System.out.println("Couldnt setup  signature: "+ee);
        }
        
        
        return broadcast;        
    }
}
