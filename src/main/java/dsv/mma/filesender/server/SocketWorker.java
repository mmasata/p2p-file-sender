/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dsv.mma.filesender.server;

import dsv.mma.filesender.GlobalVariables;
import dsv.mma.filesender.communication.Message;
import dsv.mma.filesender.client.FileSenderController;
import dsv.mma.filesender.communication.MessageBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Martas-NTB
 */
public class SocketWorker implements Runnable {
    private Socket connection;
    private static FileSenderController controller;

    public SocketWorker(Socket connection, FileSenderController controller) {
        this.connection = connection;
        this.controller = controller;
    }
    
    /******* METODY K ZACHOVANI TOPOLOGY *********/  
    private synchronized void join(Message message) throws IOException{
        controller.addToServerLog("Proces: " + message.getIpAndPort() + " se připojuje k Vám do kruhu.");

        //vratím data zpátky tazateli s novými PREVIOUS, NEXT a LEADEM
        OutputStream outputStream = connection.getOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        //jeho previous budu ja, jeho nextem bude muj next, lead bude nas aktualni lead, tyto data mu poslu
        Message message2 = MessageBuilder.createResponseForJoinMessage(GlobalVariables.getMyIpAddress(), GlobalVariables.getNextNode(), GlobalVariables.getLeadNode());
        objectOutputStream.writeObject(message2);
        controller.addToServerLog("Vracím připojovanému nová data a uzavírám spojení.");
        
        //jelikoz uz i ja jsem v nejakem kruhu, znemoznim se pripojovat, nybrz odpojovat, upravim GUI
        //nejdulezitejsi kdyz mame prave 2 procesy, aby ten prvni vedel ze uz je v kruhu a ne jen sam se sebou
        controller.setBtnTextToLeave();   
        
        //obeznamim sveho stareho NEXTa aby zmenil sveho previous 
        Socket myOldNextNodeSocket = socketRequest(GlobalVariables.getNextNode(), MessageBuilder.createChangePreviousMessage(message.getIpAndPort()));
        myOldNextNodeSocket.close();
        controller.addToServerLog("Upozorňuji starého NEXTa, aby si změnil svého PREVIOUS na nový proces a uzavírám spojení.");
        
        //nastavim u sebe sveho noveho NEXTa
        GlobalVariables.setNextNode(message.getIpAndPort());
        controller.addToServerLog("Nastavuji u sebe nového NEXTa.");

        //obeznamim LEADA ze mame noveho clena kruhu
        Socket leadSocket = socketRequest(GlobalVariables.getLeadNode(), MessageBuilder.createRegisterClientMessage(message.getIpAndPort()));
        leadSocket.close();
        controller.addToServerLog("Upozorňuji Leada, aby zaregistroval nový proces a uzavírám spojení.");
       
    }
    
    
    private void changePrevious(Message message){
        String newPrevious = message.getNewPreviousNode();
        GlobalVariables.setPreviousNode(newPrevious);
        controller.addToServerLog("INFO - Nastavuji nového previous na: " + newPrevious);
    }
    
    private synchronized void changeNext(Message message){
        String newNext = message.getNewNextNode();
        GlobalVariables.setNextNode(newNext);
        controller.addToServerLog("INFO - Nastavuji nového next na: " + newNext);
    }
    
    private synchronized void changeLead(Message message){
        String newLead = message.getNewLeadNode();
        GlobalVariables.setLeadNode(newLead);
        controller.addToServerLog("INFO - Nastavuji nového Leada na: " + newLead);
    }
    
    private synchronized void becameNewLead(Message message){
        GlobalVariables.setIpList(message.getIpList());
        GlobalVariables.setLeadNode(GlobalVariables.getMyIpAddress());
        GlobalVariables.setIsLead(true);
        controller.leadDialogShow();
        controller.addToServerLog("INFO - Gratulujeme, stal jste se novým Leaderem kruhu.");
    }
    
    private synchronized void registerClient(Message message){
        controller.addToServerLog("Jakožto leader budu registrovat.");
        String ipToRegister = message.getRegisteredIp();
        GlobalVariables.registerClient(ipToRegister);
        controller.addToServerLog("Zaregistroval jsem nový proces: " + ipToRegister);
        
    }
    
    private synchronized void unregisterClient(Message message){
        controller.addToServerLog("Jakožto leader budu odebírat evidenci procesu.");
        String ipToRegister = message.getRegisteredIp();
        GlobalVariables.unregisterClient(ipToRegister);
        controller.addToServerLog("Odregistroval jsem odpojený proces: " + ipToRegister);
    }
    
    private synchronized void fixDropGoPrevious(Message message) throws IOException{
        //doleva jdu tehdy jen kdyz uz znam praveho, tedy jakmile je shoda.. jdu uzavirat kruh
        if(isMyPreviousDropped(message)){
            controller.addToServerLog("FIX - PREVIOUS našel jsem souseda výpadlého procesu.");
            message.setIpOfDroppedNeighbourPrevious(GlobalVariables.getMyIpAddress());
            //jdu opravovat kruh
            this.fixRing(message);
        }
        else {
            this.goPreviousSocket(message);
        }
    }
    
    private synchronized void fixDropGoNext(Message message) throws IOException{
        //doprava mohu jit jakmile neznam nic, nebo jakmile znam leveho
        if(isMyNextDropped(message)){
            controller.addToServerLog("FIX - NEXT našel jsem souseda výpadlého procesu.");
            message.setIpOfDroppedNeighbourNext(GlobalVariables.getMyIpAddress());
            if(message.getIpOfDroppedNeighbourPrevious() == null){
                //mohu zacit sekvenci doleva
                controller.addToServerLog("FIX - Měním směr, hledám nyní Previous");
                message.setMessageType("fixDropGoPrevious");
                this.goPreviousSocket(message);
            }
            else {
                //znam leveho i praveho
                // jdu opravovat kruh
                this.fixRing(message);
            }
        }
        else {
            this.goNextSocket(message);
        }
    }
    
    private synchronized void fixRing(Message message) throws IOException{
        String ip = null;
        Socket socket = null;
        Message responseMessage = null;
        if(message.getMessageType().equals("fixDropGoNext")){
            //opravuju z toho co mel za nexta vypadleho
            //opravim si sveho nexta na noveho
            controller.addToServerLog("FIX - Měním svého Nexta na nového");
            controller.addToServerLog("FIX - Posílám nového Previous pro mého nového Nexta");
            GlobalVariables.setNextNode(message.getIpOfDroppedNeighbourPrevious());
            //zavolam svemu nextu at si opravi previous
            responseMessage = MessageBuilder.createChangePreviousMessage(GlobalVariables.getMyIpAddress());
            ip = GlobalVariables.getNextNode();
        }
        else if(message.getMessageType().equals("fixDropGoPrevious")){
            //opravuju z toho co mel za previouse vypadleho
            //opravim si sveho prevous na noveho
            controller.addToServerLog("FIX - Měním svého Previous na nového");
            controller.addToServerLog("FIX - Posílám nového Nexta pro mého nového Previousa");
            GlobalVariables.setPreviousNode(message.getIpOfDroppedNeighbourNext());
            //zavolam svemu previous at si zmeni nexta
            responseMessage = MessageBuilder.createChangeNextMessage(GlobalVariables.getMyIpAddress());
            ip = GlobalVariables.getPreviousNode();
        }
        else {
            controller.addToServerLog("ERROR - Nevalidní typ zprávy!");
            return;
        }
       socket = socketRequest(ip, responseMessage);
       socket.close();
       
       //kdyz byl vypadly leader, musime jeste provest election na noveho leadera
       if(message.getDroppedIp().equals(GlobalVariables.getLeadNode())){
         controller.addToServerLog("FIX LEADER - Vypadlý proces byl Leader, zahajuji nové volby.");
         //budeme volat nexta a porovnávat ID, až se vrátí zpět k původnímu volájícímu známe nejvyšší id i s jeho ip a můžeme ho zvolit vůdcem
         Message electionMessage = MessageBuilder.createElectionMessage(GlobalVariables.getMyIpAddress());
         Socket electionSocket = socketRequest(GlobalVariables.getNextNode(), electionMessage);
         electionSocket.close();
       }
       else {
        //pokud neni vypadly leader, tak mu poslu message leaderovi aby si odregistroval daneho vypadleho klienta
        controller.addToServerLog("FIX - Posílám Leaderovi request pro odregistrování vypadlého klienta");
        Message messageToLeader = MessageBuilder.createUnregisterClientMessage(message.getDroppedIp());
        Socket leaderSocket = socketRequest(GlobalVariables.getLeadNode(), messageToLeader);
        leaderSocket.close();
       }
    }
    
    private synchronized void election(Message message) throws IOException{
        //kdyz se zdrojova ip rovna me, vim ze jsem uz udelal cele kolecko, tedy znam nejvyssi ID -> noveho leadera
        if(message.getIpAndPort().equals(GlobalVariables.getMyIpAddress())){
            Message electionWinnerMessage = MessageBuilder.electionWinnerMessage();
            Socket socket = socketRequest(message.getNewLeadNode(), electionWinnerMessage);
            controller.addToServerLog("FIX LEADER - Volby dokončeny, novým leaderem je: " + message.getNewLeadNode() );
        }
        else {
            //porovnam hodnoty ip a posilam dal
            long myId= GlobalVariables.getId();
            long calculatedId = FileSenderController.calculateId(message.getNewLeadNode());
            
            if(myId > calculatedId){
                message.setNewLeadNode(GlobalVariables.getMyIpAddress());
            }
            
            socketRequest(GlobalVariables.getNextNode(), message);
        }
    }
    
    private synchronized void electionWinner() throws IOException{
        //stal jste se vudcem.. nyni musite vsem rict ze jste novy vudce a aby se u vas zaregistrovali
        controller.addToServerLog("INFO - Jsem nový leader, budu informovat všechny aby mi poslali své IP.");
        GlobalVariables.setLeadNode(GlobalVariables.getMyIpAddress());
        Message message = MessageBuilder.leaderInformationRequestMessage(GlobalVariables.getLeadNode());
        Socket socket = socketRequest(GlobalVariables.getNextNode(), message);
        socket.close();
    }
    
    private synchronized void leaderInformationRequest(Message message) throws IOException{
        if(!GlobalVariables.getMyIpAddress().equals(message.getNewLeadNode())){
            this.changeLead(message);
            controller.addToServerLog("INFO - Leader si vyžádal moje informace posílám mu je.");
            
            //leaderovi volam at me zaregistruje
            Message registerMessage = MessageBuilder.createRegisterClientMessage(GlobalVariables.getMyIpAddress());
            Socket leaderSocket = socketRequest(message.getNewLeadNode(), registerMessage);
            leaderSocket.close();
            
            //volám znovu pravého
            Socket socket = socketRequest(GlobalVariables.getNextNode(), message);
            socket.close();
        }
        else {
            controller.addToServerLog("INFO - Zpráva se ke mně vrátila, všechny procesy jsou zaregistrovány. Kruh je opět v pořádku.");
        }
    }
    
    /******* METODY K POSILANI SOUBORU *********/ 
    private void sendFileToLead(Message message) throws IOException{
        try {
            //prijme data od odesilatele a preposle je cilovemu adresatovi
            Message messageToDestination = MessageBuilder.createFileMessageToDestionation(message.getFile(), message.getData());
            Socket socket = socketRequest(message.getIpAndPort(),messageToDestination);
            socket.close();
            controller.addToServerLog("INFO - Jako Leader jsem v pořádku přijmul File a přeposlal jsem ho na cílový proces.");
        } catch (IOException ex) {
            controller.addToServerLog("ERROR - Nemohu najít cílového adresáta souboru.");
            //kdyz nenajdeme ciloveho adresata, musime zajistit aby se kruh opravil
            //deli se na dva pripady, preklep odesilatele (neni v kruhu, nikdy nebyl), nebo chyba cile, ze vypadl
            if(GlobalVariables.getIpList().contains(message.getIpAndPort())){
                controller.addToServerLog("INFO - Cílový adresát vypadl, bude se opravovat kruh.");
                String dropedIp = message.getIpAndPort();
                
                Message droppedMessage = MessageBuilder.fixDropMessage(dropedIp);
                
                //muze nastat ze uz ja budu jednim ze sousedu, proto se zeptam a pokud budu jeho sousedem z jedne strany ulozim a vydam se druhou stranou
                if(isMyNextDropped(droppedMessage)){
                   droppedMessage.setIpOfDroppedNeighbourNext(GlobalVariables.getMyIpAddress());
                   droppedMessage.setMessageType("fixDropGoPrevious");
                   //jdu doleva (previous)
                   this.goPreviousSocket(droppedMessage);
                }
                else if(isMyPreviousDropped(droppedMessage)){
                    droppedMessage.setIpOfDroppedNeighbourPrevious(GlobalVariables.getMyIpAddress());
                    droppedMessage.setMessageType("fixDropGoNext");
                    //jdu doprava (next)
                    this.goNextSocket(droppedMessage);
                }
                else {
                    //jdu doprava a preposlu message
                    droppedMessage.setMessageType("fixDropGoNext");
                    this.goNextSocket(droppedMessage);
                }
            }
            else {
                 controller.addToServerLog("INFO - Cílový adresát nikdy v kruhu nebyl, jedná se o překlep.");
            }
        }
    }
    
    public static synchronized Socket socketRequest(String ip, Message message) throws IOException{
        Socket socket = new Socket(ip.split(":")[0], Integer.valueOf(ip.split(":")[1]));
        OutputStream outputStream = socket.getOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(message);
        return socket;
    }
    
    public static synchronized void goNextSocket(Message message) throws IOException{
       controller.addToServerLog("FIX - Jdu na Next: " + GlobalVariables.getNextNode());
       Socket socket = socketRequest(GlobalVariables.getNextNode(), message);
       socket.close();   
    }
    
    public static synchronized void goPreviousSocket(Message message) throws IOException{
       controller.addToServerLog("FIX - Jdu na Previous: " + GlobalVariables.getPreviousNode());
       Socket socket = socketRequest(GlobalVariables.getPreviousNode(), message);
       socket.close();
        
    }
    
    public static synchronized boolean isMyNextDropped(Message message){
        String droppedIp = message.getDroppedIp();
        return droppedIp.equals(GlobalVariables.getNextNode());
    }
    
    public static synchronized boolean isMyPreviousDropped(Message message){
        String droppedIp = message.getDroppedIp();
        return droppedIp.equals(GlobalVariables.getPreviousNode());
    }
    
    private void redirectFileToDestination(Message message) throws FileNotFoundException, IOException{
        File file = message.getFile();
        byte[] data = message.getData();
        String newFileName = "C:\\DSV\\Files\\" + file.getName();
        try (FileOutputStream fos = new FileOutputStream(newFileName)) {
            fos.write(data);
            fos.close(); 
            controller.addToServerLog("SUCCESS - Přijal jsem nový soubor a uložil ho: " + newFileName);
        }
    }
    
    
    

    @Override
    public void run() {
        ObjectInputStream objectInputStream = null;
        try {
            
            //ziskame request na Server v objektu
            InputStream inputStream = connection.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            
            //request obj
            Message message = (Message) objectInputStream .readObject();
            
            if(message.getMessageType().equals("join")){
                this.join(message);
            }
            else if(message.getMessageType().equals("registerClient")){
                this.registerClient(message);
            }
            else if(message.getMessageType().equals("changePrevious")){
                this.changePrevious(message);
            }
            else if(message.getMessageType().equals("changeNext")){
                this.changeNext(message);
            }
            else if(message.getMessageType().equals("unregisterClient")){
                this.unregisterClient(message);
            }
            else if(message.getMessageType().equals("newLead")){
                this.changeLead(message);
            }
            else if(message.getMessageType().equals("becameNewLead")){
                this.becameNewLead(message);
            }
            else if(message.getMessageType().equals("sendFileToLead")){
                this.sendFileToLead(message);
            }
            else if(message.getMessageType().equals("redirectFileToDestination")){
                this.redirectFileToDestination(message);
            }
            else if(message.getMessageType().equals("fixDropGoPrevious")){
                this.fixDropGoPrevious(message);
            }
            else if(message.getMessageType().equals("fixDropGoNext")){
                this.fixDropGoNext(message);
            }
            else if(message.getMessageType().equals("election")){
                this.election(message);
            }
            else if(message.getMessageType().equals("electionWinner")){
                this.electionWinner();
            }
            else if(message.getMessageType().equals("leaderInformationRequest")){
                this.leaderInformationRequest(message);
            }
            
            
            connection.close();
            
        } catch (IOException ex) {
            Logger.getLogger(SocketWorker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SocketWorker.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                objectInputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(SocketWorker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    
}
