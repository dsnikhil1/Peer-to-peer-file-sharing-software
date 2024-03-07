import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class FileProcessingUtility {
    public static boolean check(String peerId) throws IOException {
        String pathname = "./" + peerId + "/thefile";
        File file = new File(pathname);
        return file.exists();
    }

    public static byte[] getChunks(byte[] original, int low, int high) {
        byte[] result = new byte[high - low];
        int min = Math.min(original.length - low, high - low);
        System.arraycopy(original, low, result, 0, min);
        return result;
    }

    public static HashMap<Integer, byte[]> returnChunks(int sizeOfFile, int chunkSize, String fN, int type) throws Exception {

        HashMap<Integer, byte[]> fInfo= new HashMap<Integer, byte[]>();
        BufferedInputStream file = new BufferedInputStream(new FileInputStream("./" + SupervisorThread.ps + "/" + fN));
        byte[] bA = new byte[sizeOfFile];

        file.read(bA);
        file.close();
        int cI = 0, cnt = 0;

        while (cI < sizeOfFile) {

            if (cI + chunkSize <= sizeOfFile) {
                cnt = getCnt(fInfo, cnt, bA, cI, cI + chunkSize);
            } else {
                cnt = getCnt(fInfo, cnt, bA, cI, sizeOfFile);
            }
            cI += chunkSize;

        }

        return fInfo;

    }

    private static int getCnt(HashMap<Integer, byte[]> fInfo, int cnt, byte[] bA, int cI, int cI1) {
        fInfo.put(cnt, getChunks(bA, cI, cI1));
        cnt++;
        return cnt;
    }


    public static void initialiseDirectories(int ps, String file) throws IOException {

        Path p = Paths.get("./" + String.valueOf(ps));
        System.out.println(p.toString());
        boolean exists = Files.exists(p);
        if (exists) {
            restore(p, file);
        } else {
            Files.createDirectory(p);

        }
        String pathname = "./" + String.valueOf(ps) + "/logs_" + String.valueOf(ps) + ".log";
        new File(pathname);
    }

    public static void restore(Path path, String file) throws IOException {

        Stream<Path> filesList = Files.list(path);

        for (Object fileObject : filesList.toArray()) {

            Path cf = (Path) fileObject;
            boolean equals = cf.getFileName().toString().equals(file);
            if (!equals) {
                Files.delete(cf);
            }

        }
        filesList.close();


    }

    public static HashMap<Integer, byte[]> sortData(HashMap<Integer, byte[]> map, int type) throws Exception
    {
        List<Map.Entry<Integer, byte[]> > infoItems =
                new LinkedList<Map.Entry<Integer, byte[]> >(map.entrySet());

        Collections.sort(infoItems, new Comparator<Map.Entry<Integer, byte[]> >() {
            public int compare(Map.Entry<Integer, byte[]> o1,
                               Map.Entry<Integer, byte[]> o2)
            {
                return (o1.getKey()).compareTo(o2.getKey());
            }
        });

        HashMap<Integer, byte[]> tpm = new LinkedHashMap<Integer, byte[]>();
        for (Map.Entry<Integer, byte[]> sorted : infoItems) {
            tpm.put(sorted.getKey(), sorted.getValue());
        }
        return tpm;

    }



}

