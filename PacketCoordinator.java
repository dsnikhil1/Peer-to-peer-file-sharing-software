

import java.nio.*;
import java.nio.charset.StandardCharsets;
public class PacketCoordinator implements FixedValues{

    public static byte[] handshakeBuilder(int ps)
    {
        byte[] greetPckt = new byte[32];
        byte[] paylod = ByteBuffer.allocate(4).put(String.valueOf(ps).getBytes()).array();

       
        byte[] greetHdr = FixedValues.HEADER_VALUE.getBytes();
        byte[] middlPad = FixedValues.HEADER_MDDL_VALUE.getBytes();


        
        int index = 0;

        int i = 0;
        while (i < greetHdr.length) {
            greetPckt[index] = greetHdr[i];
            index += 1;
            i += 1;
        }

        i = 0;
        while (i < middlPad.length) {
            greetPckt[index] = middlPad[i];
            index += 1;
            i += 1;
        }

        i = 0;
        while (i < paylod.length) {
            greetPckt[index] = paylod[i];
            index += 1;
            i += 1;
        }


        System.out.println("Hand Shake Packet --- "+new String(greetPckt, StandardCharsets.UTF_8));
        return greetPckt;

    }
    
    public static NetworkPacket incomingMessage(byte[] mA){
        
        NetworkPacket nws = new NetworkPacket();
        nws.infer(mA);
        return nws;
    }

     public static byte[] buildPacket(int type, byte[] pL, int pIx){
        
        return NetworkPacket.build_message(type, pL, pIx);
    }
    

}