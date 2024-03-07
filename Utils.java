import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Utils {
    
    public static int getIntArray(byte[] bA) {
        return ByteBuffer.wrap(bA).getInt();
    }

    public static byte[] intToByteArray(int iTC, int type) {
        
        return ByteBuffer.allocate(4).putInt(iTC).array();
    }

    public static void addIntToByteArray(byte[] bA, int iTC, int sI) {

        byte[] lA = Utils.intToByteArray(iTC,0);

        int i=0;
        while(i<4)
        {
            bA[sI + i] = lA[i];
            i++;
        }

    }

    public static String getStringFromBytes(byte[] bA, int first, int last) {
        int size = last - first + 1;
        //tbd - spl
        if (isaBoolean(bA, last, size))
        {return "";}

        //Part of byte array need to be returned as a string
        byte[] oS = new byte[size];
        System.arraycopy(bA, first, oS, 0, size);
        return new String(oS, StandardCharsets.UTF_8);

    }

    private static boolean isaBoolean(byte[] bA, int last, int size) {
        return size <= 0 || last >= bA.length;
    }


    public static ArrayList<String> readFromFile(String fN) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader bR = new BufferedReader(new FileReader(fN));
        String line = bR.readLine();
        iterate(line, lines, bR);
        bR.close();
        return lines;
    }

    private static void iterate(String line, ArrayList<String> lines, BufferedReader bR) throws IOException {
        while (line != null) {
            lines.add(line);
            line = bR.readLine();
        }
    }

}
