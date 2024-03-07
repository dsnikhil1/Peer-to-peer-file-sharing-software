JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
		$(JC) $(JFLAGS) $*.java

CLASSES = \
        NetworkPacket.java \
		PacketCoordinator.java \
		PeerProcess.java \
		Utils.java \
		LoggingRecords.java \
		FileProcessingUtility.java \
		Peer.java \
		ParseSharedConfig.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
		$(RM) *.class
