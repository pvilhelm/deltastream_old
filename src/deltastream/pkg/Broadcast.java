/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;

import java.security.PublicKey;
import java.security.Signature;
import java.net.*;
import java.security.PrivateKey;

/**
 *
 * @author servos
 */

public class Broadcast{
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