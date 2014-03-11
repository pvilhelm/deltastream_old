package deltastream.pkg;

import java.security.PublicKey;
import java.security.Signature;
import java.net.*;
import java.security.PrivateKey;
import java.util.concurrent.BlockingQueue;

/**
 * A Broadcast class.
 * 
 * For each broadcast that is sent or received, there need to be one instance 
 * of this class to keep track on same variables. 
 * <p>
 * Init. broadcast with the Config class.
 * 
 * @author Petter Tomner
 * 
 */

public class Broadcast{
    BlockingQueue<Integer> requestedParts;
    PrivateKey priv;
    int timeLimit;
    long broadcastId; //the broadcast Id
    Signature dsa; //the signature
    PublicKey pub; //the public key
    ListOfClients listOfClients;
    Parts parts;
    int portInputStream; //the port that the source stream goes in to on the server
    int nOfParts; //number of parts that exist in the swarm at the same time
    ServerSocket inputSSS; //inputStreamServerSocket
    int samplingPeriod;
    Config config;
    int oldDatagramLength;//length of a datagram that wasnt included in a part
}