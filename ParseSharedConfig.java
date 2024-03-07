
import java.io.*;
import java.util.*;

public class ParseSharedConfig {

    static int nPN;
    static int uI;
    static int oUI;
    static String fN;
    static int fileSize;
    static int pSz;
    static int tPs;

    public static void infer(String fileLocation) throws IOException {
        ArrayList<String> fileLines = Utils.readFromFile(fileLocation);
        nPN = Integer.valueOf(fileLines.get(0).split(" ")[1]);
        uI = Integer.valueOf(fileLines.get(1).split(" ")[1]);
        oUI = Integer.valueOf(fileLines.get(2).split(" ")[1]);
        fN = fileLines.get(3).split(" ")[1];
        fileSize = Integer.valueOf(fileLines.get(4).split(" ")[1]);
        pSz = Integer.valueOf(fileLines.get(5).split(" ")[1]);
        tPs = (int) Math.ceil((double) fileSize / pSz);
    }
    



}


