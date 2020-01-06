/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dsv.mma.filesender.communication;

import java.io.File;

/**
 *
 * @author Martas-NTB
 */
public class MessageBuilder {
    //Slouzi jako factory pro Message
    
    public static Message createJoinMessage(String myIp, long id){
        Message message = new Message();
        message.setMessageType("join");
        message.setIpAndPort(myIp);
        message.setRegisteredId(id);
        return message;
    }
    
    public static Message createChangeNextMessage(String newNextNode){
        Message message = new Message();
        message.setMessageType("changeNext");
        message.setNewNextNode(newNextNode);
        return message;
    }
    
    public static Message createChangePreviousMessage(String newPreviousNode){
        Message message = new Message();
        message.setMessageType("changePrevious");
        message.setNewPreviousNode(newPreviousNode);
        return message;
    }
    
    public static Message createUnregisterClientMessage(String ipToUnregister){
        Message message = new Message();
        message.setMessageType("unregisterClient");
        message.setRegisteredIp(ipToUnregister);
        return message;
    }
    
    public static Message createRegisterClientMessage(String ipToRegister){
        Message message = new Message();
        message.setMessageType("registerClient");
        message.setRegisteredIp(ipToRegister);
        return message;
    }
    
    public static Message createNewLeadMessage(String newLeadIp){
        Message message = new Message();
        message.setMessageType("newLead");
        message.setNewLeadNode(newLeadIp);
        return message;
    }
    
    public static Message createResponseForJoinMessage(String newPreviousNode, String newNextNode, String newLeadNode){
        Message message = new Message();
        message.setNewPreviousNode(newPreviousNode);
        message.setNewNextNode(newNextNode);
        message.setNewLeadNode(newLeadNode);
        return message;
    }
    
    public static Message createFileMessageForLeader(String destinationIp, File file, byte[] data){
        Message message = new Message();
        message.setMessageType("sendFileToLead");
        message.setIpAndPort(destinationIp);
        message.setFile(file);
        message.setData(data);
        return message;
    }
    
    public static Message createFileMessageToDestionation(File file, byte[] data){
        Message message = new Message();
        message.setMessageType("redirectFileToDestination");
        message.setFile(file);
        message.setData(data);
        return message;
    }
    
    public static Message fixDropMessage(String dropedIp){
        Message droppedMessage = new Message();
        droppedMessage.setDroppedIp(dropedIp);
        droppedMessage.setIpOfDroppedNeighbourNext(null);
        droppedMessage.setIpOfDroppedNeighbourPrevious(null);
        return droppedMessage;
    }
    
    
    public static Message createElectionMessage(String sourceIp){
        Message message = new Message();
        message.setMessageType("election");
        message.setIpAndPort(sourceIp);
        message.setNewLeadNode(sourceIp);
        return message;
    }
    
    public static Message electionWinnerMessage(){
        Message message = new Message();
        message.setMessageType("electionWinner");
        return message;
    }
    
    public static Message leaderInformationRequestMessage(String ip){
        Message message = new Message();
        message.setMessageType("leaderInformationRequest");
        message.setNewLeadNode(ip);
        return message;
    }
}
