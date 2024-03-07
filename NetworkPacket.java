import java.util.*;

public class NetworkPacket implements FixedValues {

    public int typeID;
    public int pIx;
    public byte[] pL;

    
    public static byte[] build_message(int type, byte[] pL, int pIx){
        byte[] msgg;


    if (type == NetworkPacket.CHOKE || type == NetworkPacket.UNCHOKE || type == NetworkPacket.INTERESTED || type == NetworkPacket.NOT_INTERESTED) {
        msgg = new byte[5];
        Utils.addIntToByteArray(msgg, 1, 0);
        msgg[4] = (byte) type;
        return msgg;
    } else if (type == NetworkPacket.HAVE || type == NetworkPacket.REQUEST) {
        msgg = new byte[9];
        Utils.addIntToByteArray(msgg, 5, 0);
        msgg[4] = (byte) type;
        Utils.addIntToByteArray(msgg, pIx, 5);
        return msgg;
    } else if (type == NetworkPacket.BITFIELD) {
        msgg = new byte[(5 + pL.length)];
        Utils.addIntToByteArray(msgg, 1 + pL.length, 0);
        msgg[4] = (byte) type;
 
        int i = 0;
        while (i < pL.length) {
            msgg[5 + i] = pL[i];
            i++;
        }

        return msgg;
    } else if (type == NetworkPacket.PIECE) {
        msgg = new byte[(9 + pL.length)];
        Utils.addIntToByteArray(msgg, 1 + pL.length, 0);
        msgg[4] = (byte) type;
        Utils.addIntToByteArray(msgg, pIx, 5);

        int i = 0;
        while (i < pL.length) {
            msgg[9 + i] = pL[i];
            i++;
        }

        return msgg;
    } else {
        return new byte[0];
    }
    


}

    public void infer(byte[] nws){ 
        int len =  nws.length;
        if(nws.length  == 0) {
            this.typeID = -1;
            return;
        }
        int type = nws[0];
        if (type>=0&&type<=7){
            this.typeID = type; 
            if (len == 5&&(type==HAVE||type==REQUEST)){  
                pIx = Utils.getIntArray(Arrays.copyOfRange(nws, 1, 5));
            } else if (type==PIECE){
                pIx = Utils.getIntArray(Arrays.copyOfRange(nws, 1, 5));
                this.pL = new byte[len - 5];
               
                int i = 5;
                while (i < len) {
                    this.pL[i - 5] = nws[i];
                    i++;
                }


            } else if (type==BITFIELD){
                this.pL = new byte[len - 1];
                
                int i = 1;
                while (i < len) {
                    this.pL[i - 1] = nws[i];
                    i++;
                }

            }
        } 
    }
}