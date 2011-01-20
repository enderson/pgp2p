import java.io.*;
import java.net.*;
import java.util.*;

import net.jxta.credential.*;
import net.jxta.discovery.*;
import net.jxta.endpoint.*;
import net.jxta.id.*;
import net.jxta.peergroup.*;
import net.jxta.pipe.*;
import net.jxta.protocol.*;

import net.jxta.membership.Authenticator;
import net.jxta.membership.MembershipService;

import net.jxta.document.*;
import net.jxta.platform.*;
import net.jxta.rendezvous.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Peer implements RendezvousListener, PipeMsgListener {

   public static DiscoveryService netPGDiscoveryService = null;
   public static RendezVousService appPGRdvService = null;
   public static RendezVousService netPGRdvService = null;
   public static NetworkConfigurator configurator = null;
   public static OutputPipe outputPipe = null;
   public static InputPipe inputPipe = null;
   public static PeerGroup netPeerGroup = null;
   public static PeerGroup appPeerGroup = null;
   public static Random rand = null;

   public static String NetPeerGroupID="urn:jxta:uuid-8B33E028B054497B8BF9A446A224B1FF02";
   public static String NetPeerGroupName="My NetPG";
   public static String NetPeerGroupDesc="A Private Net Peer Group";
   public static String jxtaHome = "Peer1";
   public static String rdvlock = new String("rocknroll");
   public static String exitlock = new String("jazz");
   public static String myPeerID;

   public static boolean connected=false;
   //PeerGUI gui = null;
   //PeerInterface gui = null;

   // -------------------------------------

   public Peer() throws NoSuchAlgorithmException, Exception {
      //jxtaHome = System.getenv("JXTA_HOME");
      //System.out.println(jxtaHome);
      if ( null == jxtaHome ) {
         System.out.println("System property JXTA_HOME null. Exiting.");
         System.exit(1);
      }
      rand = new Random();
      //gui = new PeerInterface();
      //gui.setVisible(true);
   }

   public Peer(int a){}
   // -------------------------------------

   private void startJXTA() throws Throwable {

      clearCache(new File(jxtaHome,"cm"));

      NetPeerGroupFactory factory=null;
      try {
         factory = new NetPeerGroupFactory(
            (ConfigParams)configurator.getPlatformConfig(),
            new File(jxtaHome).toURI()
//           ,
//            
//            IDFactory.fromURI(new URI(NetPeerGroupID)),
//            
//            NetPeerGroupName,
//            
//            (XMLElement) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8,
//                "desc", NetPeerGroupName)
         );
      }
      //catch(URISyntaxException e) {
      catch(Exception e) {
         e.printStackTrace();
         System.out.println("Exiting...");
         System.exit(1);
      }
      netPeerGroup = factory.getInterface();
      
      netPGDiscoveryService = netPeerGroup.getDiscoveryService();

      netPGRdvService = netPeerGroup.getRendezVousService();
      netPGRdvService.addListener(this);
   }

   // -------------------------------------

   public void createApplicationPeerGroup() {

      // key parameters for the new "appPeerGroup"
      String name = "MyAppGroup";
      String desc = "MyAppGroup Description goes here";
      String gid =  "urn:jxta:uuid-79B6A084D3264DF8B641867D926C48D902";
      String specID = "urn:jxta:uuid-309B33F10EDF48738183E3777A7C3DE9C5BFE5794E974DD99AC7D409F5686F3306";

      //  create the new application group, and publish its various advertisements
      try {
         ModuleImplAdvertisement implAdv = netPeerGroup.getAllPurposePeerGroupImplAdvertisement();

         ModuleSpecID modSpecID = (ModuleSpecID )IDFactory.fromURI(new URI(specID));
         implAdv.setModuleSpecID(modSpecID);
         PeerGroupID groupID = (PeerGroupID )IDFactory.fromURI(new URI(gid));
         appPeerGroup = netPeerGroup.newGroup(groupID, implAdv, name, desc);
         PeerGroupAdvertisement pgadv = appPeerGroup.getPeerGroupAdvertisement();

         appPGRdvService = appPeerGroup.getRendezVousService();

         myPeerID = appPeerGroup.getPeerID().toString();

         netPGDiscoveryService.publish(implAdv);
         netPGDiscoveryService.remotePublish(null,implAdv);
         netPGDiscoveryService.remotePublish(null,pgadv);

         // listen for app group rendezvous events
         appPeerGroup.getRendezVousService().addListener(this);

         // join the group
         if (appPeerGroup != null) {
            AuthenticationCredential cred = new AuthenticationCredential(appPeerGroup, null, null);
            MembershipService membershipService = appPeerGroup.getMembershipService();
            Authenticator authenticator = membershipService.apply(cred);
            if (authenticator.isReadyForJoin()) {
               membershipService.join(authenticator);
               System.out.println("Joined group: " + appPeerGroup);
               //gui.setMessage("Joined group: " + appPeerGroup+"\n");
            }
            else {
               System.out.println("Impossible to join the group");
               //gui.setMessage("There is a problem in joining the group"+"\n");
            }
         }
      }
      catch(Exception e) {
         e.printStackTrace();
         System.out.println("Exiting.");
         System.exit(1);
      }

   }

   // -----------------------------------

   private void stop() {
      netPeerGroup.stopApp();
   }

   // -----------------------------------

   private void configureJXTA() {
      configurator = new NetworkConfigurator();
      configurator.setHome(new File(jxtaHome));
      configurator.setPeerID(IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID));
      configurator.setName("My Peer Name");
      configurator.setPrincipal("ofno");
      configurator.setPassword("consequence");
      configurator.setDescription("I am a P2P Peer.");
      configurator.setUseMulticast(false);

      // fetch seeds from file, or alternately from network
//      File file = new File("seeds.txt");
//      System.out.println("File Existence "+file.exists());
      URI seedingURI = new File("seeds.txt").toURI();
      configurator.addRdvSeedingURI(seedingURI);
      configurator.addRelaySeedingURI(seedingURI);
      configurator.setUseOnlyRelaySeeds(true);
      configurator.setUseOnlyRendezvousSeeds(true);

      configurator.setTcpIncoming(false);

      try {
         configurator.save();
      }
      catch(IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      System.out.println("Plataform configured and saved");
      //gui.setMessage("Platform configured and saved"+"\n");
   }

   // ---------------------------------

   // the Rendezvous service callback
   public void rendezvousEvent(RendezvousEvent event) {
      String eventDescription;
      int eventType = event.getType();
      switch( eventType ) {
         case RendezvousEvent.RDVCONNECT:
               eventDescription = "RDVCONNECT";
               connected=true;
               break;
            case RendezvousEvent.RDVRECONNECT:
               eventDescription = "RDVRECONNECT";
               connected=true;
               break;
            case RendezvousEvent.RDVDISCONNECT:
               eventDescription = "RDVDISCONNECT";
               break;
            case RendezvousEvent.RDVFAILED:
               eventDescription = "RDVFAILED";
               break;
            case RendezvousEvent.CLIENTCONNECT:
               eventDescription = "CLIENTCONNECT";
               break;
            case RendezvousEvent.CLIENTRECONNECT:
               eventDescription = "CLIENTRECONNECT";
               break;
            case RendezvousEvent.CLIENTDISCONNECT:
               eventDescription = "CLIENTDISCONNECT";
               break;
            case RendezvousEvent.CLIENTFAILED:
               eventDescription = "CLIENTFAILED";
               break;
            case RendezvousEvent.BECAMERDV:
               eventDescription = "BECAMERDV";
               connected=true;
               break;
            case RendezvousEvent.BECAMEEDGE:
               eventDescription = "BECAMEEDGE";
               break;
            default:
               eventDescription = "UNKNOWN RENDEZVOUS EVENT";
      }
      System.out.println(new Date().toString() + "  Rdv: event=" + eventDescription + " from peer = " + event.getPeer());
      //gui.setMessage(new Date().toString() + "  Rdv: event=" + eventDescription + " from peer = " + event.getPeer()+"\n");
      synchronized(rdvlock) {
         if( connected ) {
            rdvlock.notify();
         }
      }
   }

   // ---------------------------------

   public void waitForRdv() {
      synchronized (rdvlock) {
         while (! appPGRdvService.isConnectedToRendezVous() ) {
            System.out.println("Awaiting rendezvous conx...");
            //gui.setMessage("Awaiting rendezvous conx..."+"\n");
            try {
               if (! appPGRdvService.isConnectedToRendezVous() ) {
                  rdvlock.wait();
               }
            } catch (InterruptedException e) { ; }
         }
      }
   }

   // ---------------------------------

   private void waitForQuit() {
      synchronized(exitlock) {
         try {
            System.out.println("waiting for quit");
            exitlock.wait();
            System.out.println("Goodbye");
         }
         catch(InterruptedException e) {
            ;
         }
      }
   }

   // ---------------------------------

   private void setupPipe() {
      PipeAdvertisement propagatePipeAdv = (PipeAdvertisement )AdvertisementFactory.
         newAdvertisement(PipeAdvertisement.getAdvertisementType());

      try {
         byte[] bid  = MessageDigest.getInstance("MD5").digest("abcd".getBytes("ISO-8859-1"));
         PipeID pipeID = IDFactory.newPipeID(appPeerGroup.getPeerGroupID(), bid);

         propagatePipeAdv.setPipeID(pipeID);
         propagatePipeAdv.setType(PipeService.PropagateType);
         propagatePipeAdv.setName("A chattering propagate pipe");
         propagatePipeAdv.setDescription("verbose description");

	 PipeService pipeService = appPeerGroup.getPipeService();

         inputPipe  = pipeService.createInputPipe(propagatePipeAdv, this);
         outputPipe = pipeService.createOutputPipe(propagatePipeAdv, 10000);
         System.out.println("Propagate pipes and listeners created");
         System.out.println("Propagate PipeID: " + pipeID.toString());
         //gui.setMessage("Propagate pipes and listeners created"+"\n");
         //gui.setMessage("Propagate PipeID: " + pipeID.toString()+"\n");
      }
      catch (UnsupportedEncodingException e) {
         e.printStackTrace();
      }
      catch (NoSuchAlgorithmException e) {
         e.printStackTrace();
      }
      catch (IOException e) {
         e.printStackTrace();
      }

   }

   // ---------------------------------

   private void sendSearchToPeers(String value) {
      try {
         Message msg = new Message();
        MessageElement fromElem = new ByteArrayMessageElement(
            "From", null, myPeerID.toString().getBytes("ISO-8859-1"), null );
         String peermsg = "search";
         MessageElement msgElem = new ByteArrayMessageElement(
            "Message", null, peermsg.getBytes(), null);//data.getBytes("ISO-8859-1"), null );
         MessageElement msgValue = new ByteArrayMessageElement(
            "Value", null, value.getBytes(), null);//data.getBytes("ISO-8859-1"), null );
         msg.addMessageElement(fromElem);
         msg.addMessageElement(msgElem);
         msg.addMessageElement(msgValue);
         outputPipe.send(msg);
      }
      catch(IOException e) {
         e.printStackTrace();}
 }
 private void sendFileToPeers(String filename) throws UnsupportedEncodingException, IOException{
         Message msg = new Message();
            MessageElement fromElem = new ByteArrayMessageElement(
            "From", null, myPeerID.toString().getBytes("ISO-8859-1"), null );
         String peermsg = "file";
         MessageElement msgElem = new ByteArrayMessageElement(
            "Message", null, peermsg.getBytes(), null);//data.getBytes("ISO-8859-1"), null );
         File file = new File("kashif.n3");
         byte[] fileinBytes = getBytesFromFile(file);
         MessageElement msgFile = new ByteArrayMessageElement(
            "Value", null, fileinBytes, null);//data.getBytes("ISO-8859-1"), null );
         msg.addMessageElement(fromElem);
         msg.addMessageElement(msgElem);
         msg.addMessageElement(msgFile);
         outputPipe.send(msg);
 }

   private static byte[] getBytesFromFile(File file) throws IOException {

        InputStream is = new FileInputStream(file);
        // Get the size of the file
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            System.out.println("File is too large to process");
            return null;
        }

        byte[] bytes = new byte[(int)length];
        int offset = 0;
        int numRead = 0;
        while ( (offset < bytes.length) &&( (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) ) {
            offset += numRead;
        }
        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }



   // ---------------------------------

   public void doSomething(final String query) {
      //setupPipe();
      new Thread("AppGroup Send Thread") {
         public void run() {
            int sleepy=1000;
            //while(true) {
               sendSearchToPeers(query);
               try {
                  sleep(sleepy);
               }
               catch(InterruptedException e) {}
            //}
         }
      }.start();
   }

   // ---------------------------------

   // the InputPipe callback
   public void pipeMsgEvent(PipeMsgEvent event) {
     try {
       Message msg = event.getMessage();
       byte[] fromBytes = msg.getMessageElement("From").getBytes(true);
       byte[] msgBytes = msg.getMessageElement("Message").getBytes(true);
       byte[] data = msg.getMessageElement("Value").getBytes(true);
       String fromPeerID = new String(fromBytes);
       if( fromPeerID.equals(myPeerID)) {
          System.out.print("(from self): ");
          //gui.setMessage("Message sent: "+new String(msgBytes)+"\n");
       }
       else {
           if(new String(msgBytes).equals("search")){
        	   System.out.print("Search Message from other peer: ");
        	   System.out.println(new Date().toString()+ " "+ fromPeerID+" says "+new String(data));
           //gui.setMessage("Search Message from other peer: ");
           //gui.setMessage(new Date().toString()+ " "+ fromPeerID+" says "+new String(data)+"\n");
           sendFileToPeers("file");// send the retrieved file to all peers
           
            }
           else if(new String(msgBytes).equals("file")){
           //gui.setMessage("File Recieved from other peer: ");
           //gui.setMessage(new Date().toString()+ " "+ fromPeerID+" says "+new String(data)+"\n");
           createFile(data);
           //LinkedList list = RDFHandeling.readRDF("new_file");
          // gui.saveRDFtoDB(list);
           }
       }

     }
     catch (Exception e) {
       e.printStackTrace();
       return;
     }

   }
public void createFile(byte[] data) throws FileNotFoundException, IOException{
    File someFile = new File("new_file");
    FileOutputStream fos = new FileOutputStream(someFile);
    fos.write(data);
    fos.flush();
    fos.close();
}

   // ---------------------------------

   private static void clearCache(final File rootDir) {
      try {
         if (rootDir.exists()) {
            File[] list = rootDir.listFiles();
            for (File aList : list) {
               if (aList.isDirectory()) {
                  clearCache(aList);
               } else {
                  aList.delete();
               }
            }
         }
         rootDir.delete();
         System.out.println("Cache component " + rootDir.toString() + " cleared.");
         //gui.setMessage("Cache component " + rootDir.toString() + " cleared."+"\n");
      }
      catch (Throwable t) {
         System.out.println("Unable to clear " + rootDir.toString());
         t.printStackTrace();
      }
   }

   // ---------------------------------

   public static void main(String[] args) throws Throwable {
      Logger.getLogger("net.jxta").setLevel(Level.SEVERE);
      
      Peer peer = new Peer();
      peer.configureJXTA();
      
      try {
         peer.startJXTA();
         peer.createApplicationPeerGroup();
         peer.waitForRdv();
         System.out.println("Connected now send message");
         peer.setupPipe();
         peer.waitForQuit();
      }
      catch(Exception e) {
         e.printStackTrace();
         System.out.println("Exiting.");
         System.exit(1);
      }
   }
}