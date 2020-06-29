/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dsv.mma.filesender.communication;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Martas-NTB
 */
public class Message implements Serializable {
    
    
    //join , changeNext , registerClient ,  ... , fixDropGoPrevious, fixDropGoNext
    private String messageType;
     
    //odesilateluv identifikator
    private String ipAndPort;
    
    //ip adresa pro leada, pro registraci/odregistraci
    private String registeredIp;
    
    private long registeredId;
    
    //ip adresa vypadleho kruhu
    private String droppedIp;
    
    //ip adresa toho komu vypadl next
    private String ipOfDroppedNeighbourNext;
    
    //ip adresa toho komu vypadl previous
    private String ipOfDroppedNeighbourPrevious;
    
    //novy previous node
    private String newPreviousNode;
    
    //novy next node
    private String newNextNode;
    
    //novy lead
    private String newLeadNode;
    
    //list IP adres
    private List<String> ipList;
    
    //file info
    private File file;
    //file data
    private byte[] data;

    public String getIpOfDroppedNeighbourNext() {
        return ipOfDroppedNeighbourNext;
    }

    public synchronized void setIpOfDroppedNeighbourNext(String ipOfDroppedNeighbourNext) {
        this.ipOfDroppedNeighbourNext = ipOfDroppedNeighbourNext;
    }

    public synchronized String getIpOfDroppedNeighbourPrevious() {
        return ipOfDroppedNeighbourPrevious;
    }

    public void setIpOfDroppedNeighbourPrevious(String ipOfDroppedNeighbourPrevious) {
        this.ipOfDroppedNeighbourPrevious = ipOfDroppedNeighbourPrevious;
    }

    
    
    
    public String getDroppedIp() {
        return droppedIp;
    }

    public void setDroppedIp(String droppedIp) {
        this.droppedIp = droppedIp;
    }
    
    

    public long getRegisteredId() {
        return registeredId;
    }

    public void setRegisteredId(long registeredId) {
        this.registeredId = registeredId;
    }

    
    
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
    
    

    public List<String> getIpList() {
        return ipList;
    }

    public void setIpList(List<String> ipList) {
        this.ipList = ipList;
    }
     
    public String getRegisteredIp() {
        return registeredIp;
    }

    public void setRegisteredIp(String registeredIp) {
        this.registeredIp = registeredIp;
    } 
    
    public String getNewPreviousNode() {
        return newPreviousNode;
    }

    public void setNewPreviousNode(String newPreviousNode) {
        this.newPreviousNode = newPreviousNode;
    }

    public String getNewNextNode() {
        return newNextNode;
    }

    public void setNewNextNode(String newNextNode) {
        this.newNextNode = newNextNode;
    }

    public String getNewLeadNode() {
        return newLeadNode;
    }

    public void setNewLeadNode(String newLeadNode) {
        this.newLeadNode = newLeadNode;
    }
    
    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getIpAndPort() {
        return ipAndPort;
    }

    public void setIpAndPort(String ipAndPort) {
        this.ipAndPort = ipAndPort;
    }   
}
