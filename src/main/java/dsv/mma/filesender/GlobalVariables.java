/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dsv.mma.filesender;

import dsv.mma.filesender.client.FileSenderController;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Martas-NTB
 */
public class GlobalVariables {
    //reference na GUI
    public static FileSenderController guiReference;
    
    //Trida drzi promenne ke kterym pristupuji/edituji je vsechna vlakna aplikace
    private static String previousNode = null;
    private static String nextNode = null;
    private static String leadNode = null;    
    private static String myIpAddress;
    private static int myPort;   
    private static boolean isLead = false;
    private static List<String> ipList = new ArrayList();

    //moje id
    private static long id;

    public static long getId() {
        return id;
    }

    public static void setId(long id) {
        GlobalVariables.id = id;
        guiReference.changeId(String.valueOf(id));
    }
    
    
    
    public static synchronized void registerClient(String ip){
        ipList.add(ip);
    }
    
    public static synchronized void unregisterClient(String ip){
        ipList.remove(ip);
    }  
    
    public static synchronized String getPreviousNode() {
        return previousNode;
    }

    public static synchronized void setPreviousNode(String previousNode) {
        GlobalVariables.previousNode = previousNode;
        guiReference.changePrevious(previousNode);
    }

    public static synchronized String getNextNode() {
        return nextNode;
    }

    public static synchronized void setNextNode(String nextNode) {
        GlobalVariables.nextNode = nextNode;
        guiReference.changeNext(nextNode);
    }

    public static synchronized String getLeadNode() {
        return leadNode;
    }

    public static synchronized void setLeadNode(String leadNode) {
        if(!leadNode.equals(GlobalVariables.myIpAddress)){
           GlobalVariables.setIsLead(false);
           GlobalVariables.setIpList(new ArrayList());
        }
        else {
            GlobalVariables.setIsLead(true);
            guiReference.leadDialogShow();
        }
        
        GlobalVariables.leadNode = leadNode;
        guiReference.changeLead(leadNode);
    }

    public static synchronized boolean isIsLead() {
        return isLead;
    }

    public static synchronized void setIsLead(boolean isLead) {
        GlobalVariables.isLead = isLead;
    }

    public static synchronized List<String> getIpList() {
        return ipList;
    }

    public static synchronized void setIpList(List<String> ipList) {
        GlobalVariables.ipList = ipList;
    }

    public static synchronized String getMyIpAddress() {
        return myIpAddress;
    }

    public static synchronized void setMyIpAddress(String myIpAddress) {
        GlobalVariables.myIpAddress = myIpAddress;
    }

    public static synchronized int getMyPort() {
        return myPort;
    }

    public static synchronized void setMyPort(int myPort) {
        GlobalVariables.myPort = myPort;
    }   
    
}
