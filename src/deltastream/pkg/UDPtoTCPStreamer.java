/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package deltastream.pkg;
import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.security.*;
/**
 * Repackages a UDP stream to a TCP-stream.
 * 
 * The UDP-stream source connects to this input. Each UDP datagram is then 
 * stripped of its header and sent together with it's
 * data length as a TCP stream to {@link ReadInputStreamUDP}.
 * <p>
 * (Ugly workaround solution should be directly implemented in {@link ReadInputStream})
 * 
 * 
 * @author Petter Tomner 
 */
public class UDPtoTCPStreamer {
        static DatagramSocket serverSocket;
        static Socket socketOut;
    
    /**
     *
     * @param args
     */
    public void UDPtoTCPStreamer(){
        
        for(;;){
            try{

                serverSocket = new DatagramSocket(5555);
                //DatagramSocket outSocket = new DatagramSocket(4321);
                socketOut = new Socket("127.0.0.1",1081); //open socket to deltastream server

                //SocketAddress remoteAddr =new InetSocketAddress("127.0.0.1",4321);
                BufferedOutputStream outputStream = new BufferedOutputStream(socketOut.getOutputStream());
                DataOutputStream dataOutputStream = new DataOutputStream ( outputStream);
                byte[] buffer = new byte[65508];
                //byte[] Buffer = new byte[65508];
                while(true){
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    serverSocket.receive(packet);

                    System.out.println("Got datagram with length: "+packet.getLength());
                    //packet.setSocketAddress(remoteA ddr);
                    //serverSocket.send(packet);
                    dataOutputStream.writeShort(packet.getLength());//wirte packet length
                    outputStream.write(packet.getData(), 0, packet.getLength());  //write data length
                    
                }
                
            } 
            catch(Exception ee){
                System.out.println(ee);
                try{
                    serverSocket.close();
                    if(socketOut!=null)
                        socketOut.close();
                    Thread.sleep(1000);
                }
                catch(Exception ee2){
                    System.out.println("Couldnt catch exceptions"+ee2);
                }
                
            }
        
        }
        
    }
    
}
