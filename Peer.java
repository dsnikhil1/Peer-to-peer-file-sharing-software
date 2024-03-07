import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Peer {

    
    static int numFiles = 0;
    static HashSet<Integer> interestPeers = new HashSet<>();
    static HashSet<Integer> peerN = new HashSet<>();
    static int oUPeer;
    static HashMap<Integer, Double> speeedID = new HashMap<>();
    static HashMap<Integer, BitSet> bMaps = new HashMap<>();
    static HashMap<Integer, Boolean> cMaps = new HashMap<>();
    static HashMap<Integer, Boolean> fMaps = new HashMap<>();
    static BitSet rPiece;
   

    int peerS;
    String hName;
    int po;
    int hFile;
    Socket sock;
    boolean hChoke;
    static HashMap<Integer, byte[]> fileI;

    public Peer(int peerS, String hName, int po, int hFile) throws Exception {
        this.peerS = peerS;
        this.hName = hName;
        this.po = po;
        this.hFile = hFile;
        bMaps.put(peerS, new BitSet(ParseSharedConfig.tPs));
        if (peerS != SupervisorThread.ps) {
            cMaps.put(peerS, true);
            speeedID.put(peerS, 0.0);
        }

        if (this.hFile == 1 && (peerS == SupervisorThread.ps)) {
            bMaps.get(peerS).set(0, ParseSharedConfig.tPs);
            numFiles = 1;
            fileI = FileProcessingUtility.returnChunks(ParseSharedConfig.fileSize, ParseSharedConfig.pSz, ParseSharedConfig.fN, 1);
            fMaps.put(SupervisorThread.ps, true);
        } else if (this.hFile != 1 && (peerS == SupervisorThread.ps)) {
            fileI = new HashMap<Integer, byte[]>();
            fMaps.put(SupervisorThread.ps, false);

        }

    }

    public void stExMes(Socket portal) {
        this.sock = portal;

        new Thread(new MessageExchange(portal)).start();
    }

    public void setBitFields(int ps, byte[] bytes) {
        bMaps.put(ps, BitSet.valueOf(bytes));
    }

    public void setBitField(int ps, int pIx) {
        bMaps.get(ps).set(pIx);
    }

    class MessageExchange implements Runnable {
        Socket portal;

        public MessageExchange(Socket portal) {
            this.portal = portal;
        }

        @Override
        public void run() {
            synchronized (this) {
                try {

                    DataInputStream inFl = new DataInputStream(portal.getInputStream());
                    DataOutputStream oSM = new DataOutputStream(portal.getOutputStream());

                    if (!bMaps.get(SupervisorThread.ps).isEmpty()) {
                        printPaCo(oSM);
                    }

                    while (numFiles < SupervisorThread.peers.size()) {

                           int mS = 0;
                           try
                           {
                                mS = inFl.readInt();

                           }
                           catch (EOFException e)
                           {
                              
                                continue;
                           }

                        byte[] mA = new byte[mS];
                        double start = System.currentTimeMillis();
                        inFl.read(mA);
                        double time = System.currentTimeMillis() - start;

                        NetworkPacket nws = PacketCoordinator.incomingMessage(mA);
                        speeedID.put(peerS, mS / time);
                        speeedID = sortDownloadSpeeds(speeedID);
                        switch (nws.typeID) {
                            case NetworkPacket.BITFIELD:
                                bMaps.put(peerS, BitSet.valueOf(nws.pL));


                                

                                BitSet pR = (BitSet) bMaps.get(SupervisorThread.ps).clone();
                                pR.xor(bMaps.get(peerS));
                                pR.andNot(bMaps.get(SupervisorThread.ps));

                                checkInterest(pR, oSM, nws);

                                break;
                            case NetworkPacket.INTERESTED:
                                Interest();
                                break;
                            case NetworkPacket.NOT_INTERESTED:

                                NotInterested();
                                break;
                            case NetworkPacket.CHOKE:
                                ch(" is choked by ", true);
                                break;
                            case NetworkPacket.UNCHOKE:
                                ch(" is unchoked by ", false);
                                // request piece required

                                pR = (BitSet) bMaps.get(SupervisorThread.ps).clone();
                                pR.xor(bMaps.get(peerS));
                                pR.andNot(bMaps.get(SupervisorThread.ps));

                                checkrPiece(pR, oSM);

                                break;
                            case NetworkPacket.REQUEST:
                                req(nws, oSM,1);
                                break;
                            case NetworkPacket.PIECE:
                                piece(nws, oSM);

                                break;

                            case NetworkPacket.HAVE:
                                have(nws, oSM);

                                break;
                            default:
                                break;

                        }

                    }

                    Thread.sleep(5000);
                    System.exit(0);

                }
                catch (SocketException s)
                {
                    System.out.println("Socket connection closed with " + peerS);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }


            }
        }

        private void have(NetworkPacket nws, DataOutputStream oSM) throws IOException {
            BitSet pR;
            if(nws.pIx == ParseSharedConfig.tPs) {
                numFiles += 1;
                fMaps.put(peerS, true);
                return;
            }
            pR = getBitSet(nws);

            prLength(!(pR.length() == 0) && !(fileI.size() == ParseSharedConfig.tPs), oSM);
        }

 
        private BitSet getBitSet(NetworkPacket nws) {
            BitSet pR;
            SupervisorThread.log.logInfo("Peer "+ SupervisorThread.ps + " received have nws from "+ peerS);


            setBitField(peerS, nws.pIx);
            pR = (BitSet) bMaps.get(SupervisorThread.ps).clone();
            pR.xor(bMaps.get(peerS));
            pR.andNot(bMaps.get(SupervisorThread.ps));
            return pR;
        }

        private void piece(NetworkPacket nws, DataOutputStream oSM) throws Exception {
            BitSet pR;
            fileI.put(nws.pIx, nws.pL);
            setBitField(SupervisorThread.ps, nws.pIx);

            for (Peer clg : SupervisorThread.peers.values()) {
                if (clg.sock != null) {
                    DataOutputStream oStream = new DataOutputStream(clg.sock.getOutputStream());
                    if(fileI.size() == ParseSharedConfig.tPs) {
                        oStream.write(PacketCoordinator.buildPacket(NetworkPacket.HAVE, null, ParseSharedConfig.tPs));
                        oStream.write(PacketCoordinator.buildPacket(NetworkPacket.HAVE, null, nws.pIx));
                    } else {
                        oStream.write(PacketCoordinator.buildPacket(NetworkPacket.HAVE, null, nws.pIx));
                    }
                    oStream.flush();
                }
            }

            SupervisorThread.log.logInfo("Peer "+ SupervisorThread.ps + " has downloaded the piece from "+ peerS +". Now the number of pieces it has is " + fileI.size());

            
            pR = (BitSet) bMaps.get(SupervisorThread.ps).clone();
            pR.xor(bMaps.get(peerS));
            pR.andNot(bMaps.get(SupervisorThread.ps));

            rPiece = new BitSet(ParseSharedConfig.tPs);

            if (!(pR.length() == 0) && !hChoke) {
                intialise(oSM, pR);
            }
            if (fileI.size() == ParseSharedConfig.tPs) {

                initialiseTps();
            }
        }

        private void req(NetworkPacket nws, DataOutputStream oSM, int type) throws IOException {
            if (peerS == oUPeer || peerN.contains(peerS)) {
                int pIx = nws.pIx;
                byte[] info = new byte[ParseSharedConfig.pSz];

                info = getChunkData(pIx);

           
                if(info != null) {
                    oSM.write(PacketCoordinator.buildPacket(NetworkPacket.PIECE, info, pIx));
                    oSM.flush();
                }

            }
        }

     

        private void ch(String x, boolean hChoke) {
            SupervisorThread.log.logInfo("Peer " + SupervisorThread.ps + x + peerS);

            
            hChoke = hChoke;
        }

        private void NotInterested() {
            SupervisorThread.log.logInfo("Peer "+ SupervisorThread.ps + " received not interested nws from "+ peerS);
            interestPeers.remove(peerS);
        }

        private void Interest() {
            SupervisorThread.log.logInfo("Peer "+ SupervisorThread.ps + " received interested nws from "+ peerS);
            interestPeers.add(peerS);
        }

        private void checkInterest(BitSet pR, DataOutputStream oSM, NetworkPacket nws) throws IOException {
            prLength(!(pR.length() == 0), oSM);

            if (nws.pL.length * 8 >= ParseSharedConfig.tPs) {
                numFiles += 1;
                fMaps.put(peerS, true);
            }
        }

  

    }

    private static void checkrPiece(BitSet pR, DataOutputStream oSM) throws IOException {
            if (rPiece == null) {
                rPiece = new BitSet(ParseSharedConfig.tPs);
            }


            if (!(pR.size() == 0)) {

                int pIx = pR.nextSetBit(new Random().nextInt(pR.size()));
                if (pIx < 0) {
                    pIx = pR.nextSetBit(0);
                }
                if (pIx >= 0) {
                    rPiece.set(pIx);
                    oSM.write(PacketCoordinator.buildPacket(NetworkPacket.REQUEST, null, pIx));
                    oSM.flush();
                }
            }
        }

        private static void prLength(boolean pR, DataOutputStream oSM) throws IOException {
            if (pR) {
                oSM.write(PacketCoordinator.buildPacket(NetworkPacket.INTERESTED, null, -1));
                oSM.flush();
            } else {
                oSM.write(PacketCoordinator.buildPacket(NetworkPacket.NOT_INTERESTED, null, -1));
                oSM.flush();
            }
        }

        private static void printPaCo(DataOutputStream oSM) throws IOException {
            oSM.write(PacketCoordinator.buildPacket(NetworkPacket.BITFIELD,
                    bMaps.get(SupervisorThread.ps).toByteArray(), -1));
            oSM.flush();
        }

    private void initialiseTps() throws Exception {
        SupervisorThread.log.logInfo("Peer " + SupervisorThread.ps + " has downloaded the complete file.");
        if(!fMaps.get(SupervisorThread.ps)) {
            numFiles += 1;
            writeFile();
            fMaps.put(SupervisorThread.ps, true);
            }
    }

    private static void intialise(DataOutputStream oSM, BitSet pR) throws IOException {
        int pIx = pR.nextSetBit(0);
        rPiece.set(pIx);
        oSM.write(PacketCoordinator.buildPacket(NetworkPacket.REQUEST, null, pIx));
        oSM.flush();
        pR.andNot(rPiece);
    }

    public static byte[] getChunkData(int cI) {

        return fileI.get(cI);
    }


    public void writeFile() throws Exception {
       
            fileI = FileProcessingUtility.sortData(fileI,1);
            File file = new File("./" + SupervisorThread.ps + "/thefile");
        chkNewFile(file);


    }

    private static void chkNewFile(File fileObj) throws IOException {
        if (fileObj.createNewFile()) {
            FileWriter fW = new FileWriter("./" + SupervisorThread.ps + "/" + ParseSharedConfig.fN, true);
            BufferedWriter buffer = new BufferedWriter(fW);

            for (HashMap.Entry<Integer, byte[]> entry : fileI.entrySet()) {
                buffer.write(new String(entry.getValue(), StandardCharsets.UTF_8));
            }
            buffer.close();
            fW.close();
        }
    }

    public static HashMap<Integer, Double> sortDownloadSpeeds(HashMap<Integer, Double> map) throws Exception {
        List<Map.Entry<Integer, Double>> items = new LinkedList<Map.Entry<Integer, Double>>(map.entrySet());

        srtCollect(items);

       
        HashMap<Integer, Double> tpm = new LinkedHashMap<Integer, Double>();
        for (Map.Entry<Integer, Double> sorted : items) {
            tpm.put(sorted.getKey(), sorted.getValue());
        }
        return tpm;

    }

    private static void srtCollect(List<Map.Entry<Integer, Double>> list) {
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
            public int compare(Map.Entry<Integer, Double> object1, Map.Entry<Integer, Double> obj2) {
                return -1*(object1.getValue()).compareTo(obj2.getValue());
            }
        });
    }

}

