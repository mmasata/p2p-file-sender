package dsv.mma.filesender.client;

import dsv.mma.filesender.GlobalVariables;
import dsv.mma.filesender.communication.Message;
import dsv.mma.filesender.communication.MessageBuilder;
import dsv.mma.filesender.server.SocketServer;
import dsv.mma.filesender.server.SocketWorker;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.json.simple.parser.ParseException;

public class FileSenderController implements Initializable {
    private File logFile;
    private File fileToSend = null;
    private byte[] fileData;
    private SocketServer server;
    

    @FXML
    private Button joinOrLeaveBtn;
    
    @FXML
    private Button leadDialogBtn;
    
    @FXML
    private Button selectFileBtn;
    
    @FXML
    private Button sendFileBtn;
    
    @FXML
    private TextArea logsAreaClient;
    
    @FXML
    private TextArea logsAreaServer;
    
    @FXML
    private TextField joinIpValue;
    
    @FXML
    private TextField filePathValue;
    
    @FXML
    private TextField recipientIpValue;
    
    @FXML
    private TextField leaderNodeLabel;
    
    @FXML
    private TextField previousNodeLabel;
    
    @FXML 
    private TextField nextNodeLabel;
    
    @FXML
    private Label myIp;
    
    @FXML
    private Label idLabel;
    
    @FXML
    public void exitApplication(ActionEvent event) {
        server.disableServer();
        System.exit(0);
        Platform.exit();
        
    }

    @FXML
    private void joinOrLeaveAction(ActionEvent event) throws ParseException, IOException{
        if(joinOrLeaveBtn.getText().equals("Join")){
            //budu připojovat
            addToClientLog("INFO - Pokouším se připojit ke kruhu přes Node: " + joinIpValue.getText());
            
            try {
                //connect socketu
                Socket client = new Socket(joinIpValue.getText().split(":")[0], Integer.valueOf(joinIpValue.getText().split(":")[1]));
                addToClientLog("INFO - připojil jsem se k cílovému socketu");
                
                //pripraveni streamu pro poslani objektu
                OutputStream outputStream = client.getOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream); 
                //objekt a jeho odeslani
                objectOutputStream.writeObject(MessageBuilder.createJoinMessage(GlobalVariables.getMyIpAddress(), GlobalVariables.getId()));
                    
                //dostanu nova data zpet
                InputStream inputStream = client.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                //response obj
                Message message2 = (Message) objectInputStream .readObject();
                
                //zapisu je do GUI a do Variables
                GlobalVariables.setPreviousNode(message2.getNewPreviousNode());              
                GlobalVariables.setNextNode(message2.getNewNextNode());           
                GlobalVariables.setLeadNode(message2.getNewLeadNode());
                
                this.leadDialogShow();
                              
                //vypnu textArea a zmenim button na odpojit
                joinIpValue.setDisable(true);
                joinOrLeaveBtn.setText("Leave");
                
                addToClientLog("SUCCESS - Připojení do kruhu bylo úspěšné, nastavuji nové parametry.");
            } catch (IOException ex) {
                addToClientLog("ERROR - pokus o připojení ke kruhu byl neúspěšný.");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(FileSenderController.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        else if(joinOrLeaveBtn.getText().equals("Leave")){
            //obeznamim sveho Previous, aby zmenil sveho Nexta na noveho Nexta (muj Next)
            Socket myPreviousNodeSocket = new Socket(GlobalVariables.getPreviousNode().split(":")[0] , Integer.valueOf(GlobalVariables.getPreviousNode().split(":")[1]));
            OutputStream outputStreamPrevious = myPreviousNodeSocket.getOutputStream();
            ObjectOutputStream objectOutputStreamPrevious = new ObjectOutputStream(outputStreamPrevious);
            objectOutputStreamPrevious.writeObject(MessageBuilder.createChangeNextMessage(GlobalVariables.getNextNode()));
            myPreviousNodeSocket.close();
                  
            //obeznamim sveho Nexta, aby zmenil sveho Previous na noveho Previous (muj Previous)
            Socket myNextNodeSocket = new Socket(GlobalVariables.getNextNode().split(":")[0] , Integer.valueOf(GlobalVariables.getNextNode().split(":")[1]));
            OutputStream outputStreamNext = myNextNodeSocket.getOutputStream();
            ObjectOutputStream objectOutputStreamNext = new ObjectOutputStream(outputStreamNext); 
            objectOutputStreamNext.writeObject(MessageBuilder.createChangePreviousMessage(GlobalVariables.getPreviousNode()));
            myNextNodeSocket.close();
            
            //obeznamim leada, aby me odregistroval ze sve evidence, pokud ja nejsem lead
            if(!GlobalVariables.isIsLead()){
                Socket myLeadSocket = new Socket(GlobalVariables.getLeadNode().split(":")[0] , Integer.valueOf(GlobalVariables.getLeadNode().split(":")[1]));
                OutputStream outputStreamLead = myLeadSocket.getOutputStream();
                ObjectOutputStream objectOutputStreamLead = new ObjectOutputStream(outputStreamLead); 
                objectOutputStreamLead.writeObject(MessageBuilder.createUnregisterClientMessage(GlobalVariables.getMyIpAddress()));
                myLeadSocket.close();
            }
            
            //pokud je odpojujici se lead musi byt zvolen novy Lead a tato informace predana vsem procesum v kruhu
            if(GlobalVariables.isIsLead()){
                String newLeadIp = this.leadElection(GlobalVariables.getIpList());
                for(String ip : GlobalVariables.getIpList()){
                    //chceme poslat vsem az na noveho leada, tomu totiz posleme jinou zpravu jen pro nej
                    if(!ip.equals(newLeadIp)){
                        String addr = ip.split(":")[0];
                        Integer port = Integer.valueOf(ip.split(":")[1]);
                        Socket clientSocket = new Socket(addr, port);
                        OutputStream outputStreamAll = clientSocket.getOutputStream();
                        ObjectOutputStream objectOutputStreamAll = new ObjectOutputStream(outputStreamAll);
                        objectOutputStreamAll.writeObject(MessageBuilder.createNewLeadMessage(newLeadIp));
                        clientSocket.close();
                    }
                }
                //posilame zpravu leadovi, ze je lead a seznam vsech ip co je v kruhu
                Socket newLeadSocket = new Socket(newLeadIp.split(":")[0], Integer.valueOf(newLeadIp.split(":")[1]));
                OutputStream outputStreamForNewLead = newLeadSocket.getOutputStream();
                ObjectOutputStream objectOutputStreamForNewLead = new ObjectOutputStream(outputStreamForNewLead);
                Message messageForNewLead = new Message();
                messageForNewLead.setMessageType("becameNewLead");
                //posleme mu vsechny ip adresy v kruhu az na jeho samotneho
                List<String> ipToSend = GlobalVariables.getIpList();
                ipToSend.remove(newLeadIp);
                messageForNewLead.setIpList(ipToSend);
                objectOutputStreamForNewLead.writeObject(messageForNewLead);
            }
            
            
            //odpojim z kruhu a zmenim button na Join a odemknu label
            addToClientLog("INFO - Odpojuji se z kruhu");
            
            joinIpValue.setDisable(false);
            
            String myIpAddr = GlobalVariables.getMyIpAddress();
            //nastavim na default hodnoty
            GlobalVariables.setIsLead(true);
            GlobalVariables.setLeadNode(myIpAddr);
            GlobalVariables.setPreviousNode(myIpAddr);
            GlobalVariables.setNextNode(myIpAddr);
                               
            joinOrLeaveBtn.setText("Join");
        }
        else {
            //error, nemelo by nastat
            addToClientLog("ERROR - Nastala chyba při Join/Leave");
        }
    }
    
    private String leadElection(List<String> ipList){
        String currentElected = null;
        Long currentId = 0l;
        List<Long> arrOfIds = new ArrayList();
        for(int i=0; i < ipList.size(); i++){
            long id = this.calculateId(ipList.get(i));
            
            if(id > currentId){
                currentElected = ipList.get(i);
                currentId = id;
            }
        }
        return currentElected;
    }
    
    public synchronized void leadDialogShow(){
        Platform.runLater(() -> {
            if(!GlobalVariables.isIsLead()){
                    leadDialogBtn.setDisable(true);
                }
            else {
                    leadDialogBtn.setDisable(false);
                }
        });
    }
    
    @FXML
    private void showLeadDialogAction(ActionEvent event){
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Lead Dialog");
        alert.setHeaderText("List of processes in the Ring");
        String s = Arrays.toString(GlobalVariables.getIpList().toArray());
        alert.setContentText(s);
        alert.showAndWait();
    }
    
    @FXML
    private void chooseFileAction(ActionEvent event) throws IOException{
        FileChooser fileChooser = new FileChooser();
        fileToSend = fileChooser.showOpenDialog(null);
        fileData = Files.readAllBytes(fileToSend.toPath());
        filePathValue.setText(fileToSend.getPath());
        addToClientLog("INFO - zvolil jsem si soubor na cestě: " + fileToSend.getPath());
    }
    
    @FXML
    private void sendFileAction(ActionEvent event) throws IOException{
        if(fileToSend == null){
            //error, klient si nevybral zadny soubor
            addToClientLog("ERROR - Nevybral jste žádný soubor");
        }
        else {
            try {
                String ip = GlobalVariables.getLeadNode().split(":")[0];
                Integer port = Integer.valueOf(GlobalVariables.getLeadNode().split(":")[1]);
                Socket fileSendSocket = new Socket(ip, port);
                OutputStream outputStreamToLead = fileSendSocket.getOutputStream();
                ObjectOutputStream objectOutputStreamToLead = new ObjectOutputStream(outputStreamToLead);
                objectOutputStreamToLead.writeObject(MessageBuilder.createFileMessageForLeader(recipientIpValue.getText(), fileToSend, fileData));
                fileSendSocket.close();
                
                addToClientLog("INFO - Posílám soubor: " + fileToSend.getName());
                addToClientLog("INFO - Adresatovi s Ip adresou: " + recipientIpValue.getText());
            } catch (IOException ex) {
                addToClientLog("ERROR - Nenašel jsem Leader Server, nejspíše jeho proces spadl.");
                //pripad ze jsme v kruhu jen ja a leader (zaroven prijemce)
                // to pozname tak ze previous i next jsou to same
                addToServerLog("FIX - Začíná oprava Leadera.");
                if(GlobalVariables.getPreviousNode().equals(GlobalVariables.getNextNode())){
                    //ted uz jsem tedy v kruhu sam, tedy jsem sam svym leaderem
                    String myIp = GlobalVariables.getMyIpAddress();
                    GlobalVariables.setLeadNode(myIp);
                    GlobalVariables.setPreviousNode(myIp);
                    GlobalVariables.setNextNode(myIp);
                    GlobalVariables.setIpList(new ArrayList());
                    addToServerLog("FIX - Oprava dokončena, jelikož původně byl kruh o 2 procesech, nyní jste sám v kruhu.");
                    this.setBtnTextToJoin();
                    this.leadDialogShow();
                    addToServerLog("Nyní se můžete připojit k jinému kruhu.");
                }
                else {
                    //pripad ze je nas 3 a vice v kruhu
                    Message droppedMessage = MessageBuilder.fixDropMessage(GlobalVariables.getLeadNode());
                    if(SocketWorker.isMyNextDropped(droppedMessage)){
                       droppedMessage.setIpOfDroppedNeighbourNext(GlobalVariables.getMyIpAddress());
                       droppedMessage.setMessageType("fixDropGoPrevious");
                       //jdu doleva (previous)
                       SocketWorker.goPreviousSocket(droppedMessage);
                    }
                    else if(SocketWorker.isMyPreviousDropped(droppedMessage)){
                        droppedMessage.setIpOfDroppedNeighbourPrevious(GlobalVariables.getMyIpAddress());
                        droppedMessage.setMessageType("fixDropGoNext");
                        //jdu doprava (next)
                        SocketWorker.goNextSocket(droppedMessage);
                    }
                    else {
                        //jdu doprava a preposlu message
                        droppedMessage.setMessageType("fixDropGoNext");
                        SocketWorker.goNextSocket(droppedMessage);
                    }
                }
            }
        }
    }
    
    public synchronized void changePrevious(String node){
        Platform.runLater(() -> {
            previousNodeLabel.setText(node);
        });
    }
    
    public synchronized void changeNext(String node){
        Platform.runLater(() -> {
            nextNodeLabel.setText(node);
        });
    }
    
    public synchronized void changeLead(String node){
        Platform.runLater(() -> {
            leaderNodeLabel.setText(node);
        });
    }
    
    public synchronized void changeId(String newId){
        Platform.runLater(() -> {
            idLabel.setText(newId);
        });
    }
    
    public synchronized void setBtnTextToLeave(){
       Platform.runLater(() -> {
            joinOrLeaveBtn.setText("Leave");
        });
    }
    
    public synchronized void setBtnTextToJoin(){
       Platform.runLater(() -> {
            joinOrLeaveBtn.setText("Join");
        });
    }
    
    public synchronized void addToServerLog(String message){
        Platform.runLater(() -> {
            addLog(logsAreaServer, message);
        });
    }
    
    public synchronized void addToClientLog(String message){
        Platform.runLater(() -> {
            addLog(logsAreaClient, message);
        });
    }
    
    private synchronized void addLog(TextArea area, String message){
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss.SSS");  
        String strDate = dateFormat.format(date);  
        String currentText = area.getText();
        String newText = currentText+ strDate + "  " + message + "\r\n";
        area.setText(newText);
        
        try(FileWriter fw = new FileWriter(logFile.getAbsolutePath(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(strDate + " " + message);
        } catch (IOException e) {
            //error file jsem nenasel
        }
    }
    
    
    
    
    @Override
    //prvotni spusteni procesu
    public void initialize(URL url, ResourceBundle rb) {        
        //predani sve reference globalnim promennym
        GlobalVariables.guiReference = this;
        
        //zamknuti TextArea, nechceme aby byla uzivatelem editovatelna
        filePathValue.setDisable(true);
        leaderNodeLabel.setDisable(true);
        previousNodeLabel.setDisable(true);
        nextNodeLabel.setDisable(true);
        try {
            String myIpAddr = InetAddress.getLocalHost().getHostAddress() + ":" + GlobalVariables.getMyPort() ;
            GlobalVariables.setMyIpAddress(myIpAddr);
            GlobalVariables.setId(this.calculateId(myIpAddr));
            myIp.setText(myIpAddr);
            
            //vytvorime File pro logy
            String fileName = GlobalVariables.getMyIpAddress().replace(".", "-").replace(":", "-");
            logFile = new File("C:\\DSV\\Logs\\" + fileName + ".log");
            
            //defaultne je proces sam, tudiz je leader ve svem vlastnim kruhu sam o sobe
            //a je si predchozim i nasledujicim nodem sam sobe
            GlobalVariables.setIsLead(true);
            GlobalVariables.setLeadNode(myIpAddr);
            GlobalVariables.setPreviousNode(myIpAddr);
            GlobalVariables.setNextNode(myIpAddr);
            
            
            
        } catch (UnknownHostException ex) {
            addToClientLog("ERROR - Nemohu najít svoji lokální IP adresu.");
        }
        
        //Nove vlakno serveru
        server = new SocketServer(this);
        new Thread(server).start();
    }   
    
    public static synchronized long calculateId(String addrWithPort){
        long id=0;
        String addr = addrWithPort.split(":")[0];
        Integer port = Integer.valueOf(addrWithPort.split(":")[1]);
        String[] arr = addr.split("\\.");
        long shift = 0, temp;
        for(int i=0; i < arr.length; i++){
            temp = Long.parseLong(arr[i]);
            for (int z=1; (temp-z) >= -1; z=z*10) shift++;
            id = (long)(id*Math.pow(10.0, shift));
            id += temp;
            shift = 0;
        }
        shift = 0;
        for (int y=1; (port-y)>0; y=y*10) shift++;
        id = (long) (id* Math.pow(10.0, shift));
        id+=port;
        
        return id;
    }
}
