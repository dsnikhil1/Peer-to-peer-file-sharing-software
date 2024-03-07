
import java.io.*;

import java.net.*;


import java.util.*;


public class PeerProcess {

    static final String GPATH = "Common.cfg";

    public static void main(String[] args) throws Exception {

        int ps = Integer.parseInt(args[0]);
        ParseSharedConfig.infer(GPATH);

        new Thread(new SupervisorThread(ps)).start();
    }

}

class SupervisorThread implements Runnable {

    static final String PIPATH = "PeerInfo.cfg";

    static int ps;
    static TreeMap<Integer, Peer> peers;
    static LoggingRecords log;
    static HashMap<Integer, byte[]> fInfo;



    public SupervisorThread(int ps) throws Exception {
        SupervisorThread.ps = ps;
        peers = fetchPeerDetails();


        try {

            FileProcessingUtility.initialiseDirectories(ps, ParseSharedConfig.fN);
            log = new LoggingRecords(String.valueOf(SupervisorThread.ps));


        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    @Override
    public void run() {

        new Thread(new ActivateReciever()).start();
        new Thread(new ActivateSender()).start();
        new Thread(new ChooseBestNeighbors()).start();
        new Thread(new SelectivelyEnablePeer()).start();



    }

    private class ActivateSender implements Runnable {

        byte[] greeingInfo = new byte[32];
        @Override
        public void run() {
            try {

                int port = peers.get(ps).po;
                ServerSocket sS = new ServerSocket(port);
                log.logInfo("Server: " + SupervisorThread.ps + " initiated at Port :" + port);
                boolean nP = false;
            
                Iterator<Map.Entry<Integer, Peer>> iterator = peers.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Integer, Peer> adj = iterator.next();

                    if (nP) {
                        Socket portal = sS.accept();
                        ObjectInputStream sIS = new ObjectInputStream(portal.getInputStream());
                        ObjectOutputStream sOS = new ObjectOutputStream(portal.getOutputStream());

                        sIS.read(greeingInfo);

                        sOS.write(PacketCoordinator.handshakeBuilder(ps));
                        sOS.flush();

                        log.logInfo("Peer :" + ps + " makes a connection to" + adj.getKey());
                        adj.getValue().stExMes(portal);
                    }
                    if (ps == adj.getKey()) {
                        nP = true;
                    }
                }


                sS.close();

            } catch (Exception spl) {
               spl.printStackTrace();
            }
        }
    }

    private class ActivateReciever implements Runnable {

        @Override
        public void run() {
            try {
                
                Iterator<Map.Entry<Integer, Peer>> iterator = peers.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Integer, Peer> clg = iterator.next();

                    if (clg.getKey() == ps)
                        break;

                    Peer adj = clg.getValue();
                    Socket portal = new Socket(adj.hName, adj.po);

                    ObjectOutputStream cOS = new ObjectOutputStream(portal.getOutputStream());
                    ObjectInputStream cIS = new ObjectInputStream(portal.getInputStream());

                    byte[] greeingInfo = PacketCoordinator.handshakeBuilder(ps);
                    cOS.write(greeingInfo);
                    cOS.flush();

                    cIS.readFully(greeingInfo);
                    String mH = Utils.getStringFromBytes(greeingInfo, 0, 17);
                    String mPID = Utils.getStringFromBytes(greeingInfo, 28, 31);

                    if (mH.equals("P2PFILESHARINGPROJ") && Integer.parseInt(mPID) == clg.getKey()) {
                        adj.stExMes(portal);
                    } else {
                        portal.close();
                    }
                }


            } catch (IOException spl) {

                spl.printStackTrace();

            }
        }
    }

    public static TreeMap<Integer, Peer> fetchPeerDetails() throws Exception {
        ArrayList<String> items = Utils.readFromFile(PIPATH);
        TreeMap<Integer, Peer> pDta = new TreeMap<>();
        for (String item : items) {
            String[] pices = item.split(" ");
            pDta.put(Integer.valueOf(pices[0]), new Peer(Integer.parseInt(pices[0]), pices[1],
                    Integer.valueOf(pices[2]), Integer.parseInt(pices[3])));
        }
        return pDta;
    }

}

class ChooseBestNeighbors implements Runnable {

    public ChooseBestNeighbors() {
    }

    @Override
    public void run() {
        synchronized (this) {

            try {
                while (Peer.numFiles < SupervisorThread.peers.size()) {

                    int KNs = ParseSharedConfig.nPN;
                    Peer.peerN.clear();


                    if (Peer.interestPeers.size() > KNs) {
                        int i = 0;
                        for (HashMap.Entry<Integer, Double> e : Peer.speeedID.entrySet()) {
                            Peer.peerN.add(e.getKey());
                            i++;
                            if (i >= KNs) {
                                break;
                            }
                        }
                    } else {
                        for (Integer ps : Peer.interestPeers) {
                            Peer.peerN.add(ps);
                        }
                    }

                    Peer.speeedID.replaceAll((key, value) -> 0.0);

                    Iterator<HashMap.Entry<Integer, Boolean>> iterator = Peer.cMaps.entrySet().iterator();

                    while (iterator.hasNext()) {
                        HashMap.Entry<Integer, Boolean> pair = iterator.next();

                        Socket portal = SupervisorThread.peers.get(pair.getKey()).sock;
                        if (portal == null) {
                            continue;
                        }

                        DataOutputStream dOS = new DataOutputStream(portal.getOutputStream());
                        if (Peer.peerN.contains(pair.getKey())) {
                            dOS.write(PacketCoordinator.buildPacket(NetworkPacket.UNCHOKE, null, -1));
                            dOS.flush();
                            Peer.cMaps.put(pair.getKey(), false);
                        } else {
                            dOS.write(PacketCoordinator.buildPacket(NetworkPacket.CHOKE, null, -1));
                            dOS.flush();
                            Peer.cMaps.put(pair.getKey(), true);
                        }
                    }

                    SupervisorThread.log.logInfo("Peer " + SupervisorThread.ps +" has the preferred neighbors " + Peer.peerN.toString());

                    Thread.sleep(ParseSharedConfig.uI * 1000);

                }

            }
            catch (SocketException e)
            {}
            catch (Exception e) {

                e.printStackTrace();
            }
            System.exit(0);
        }
    }
}

class SelectivelyEnablePeer implements Runnable {

    public SelectivelyEnablePeer() {
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                while (Peer.numFiles < SupervisorThread.peers.size()) {

                    HashSet<Integer> pC = new HashSet<Integer>(Peer.interestPeers);
                    HashSet<Integer> cP = new HashSet<Integer>(Peer.peerN);
                    pC.removeAll(cP);

                    Random rand = new Random();
                    if (pC.size() > 0) {
                        int cIN = rand.nextInt(pC.size());
                        Peer.oUPeer = (int) pC.toArray()[cIN];


                        Peer.cMaps.put(Peer.oUPeer, false);
                        Socket portal = SupervisorThread.peers.get(Peer.oUPeer).sock;
                        if (portal == null) {
                            break;
                        }
                        DataOutputStream dOS = new DataOutputStream(portal.getOutputStream());
                        dOS.write(PacketCoordinator.buildPacket(NetworkPacket.UNCHOKE, null, -1));
                        dOS.flush();
                    }
                    SupervisorThread.log.logInfo("Peer " + SupervisorThread.ps + " has the optimistically unchoked adj " + Peer.oUPeer);

                    Thread.sleep(ParseSharedConfig.oUI * 1000);
                
                }


            }
            catch (SocketException e)
            {}
            catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}


