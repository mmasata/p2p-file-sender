/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dsv.mma.filesender.server;

import dsv.mma.filesender.client.FileSenderController;
import dsv.mma.filesender.GlobalVariables;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Martas-NTB
 */
public class SocketServer implements Runnable {
    private ServerSocket server;
    private FileSenderController controller;

    private boolean running = true;
    public SocketServer(FileSenderController controller) {
        this.controller = controller;
    }

    
    
    @Override
    public void run() {
        controller.addToServerLog("Serverové vlákno spuštěno na portu - " + GlobalVariables.getMyPort());
        try {
            server = new ServerSocket(GlobalVariables.getMyPort());
            
            //nekonecny cyklus prijimajici klienty
            while(running){
                Socket connection = server.accept();
                
                //zde bude nove vlakno na odbaveni klienta
                //nove vlakno, protoze chceme aby server umel zpracovavat vice klientu naraz, kdyz bude treba
                new Thread(new SocketWorker(connection, controller)).start();         
            }
        } catch (IOException ex) {
            Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public synchronized void disableServer(){
        this.running = false;
    }
    
}
