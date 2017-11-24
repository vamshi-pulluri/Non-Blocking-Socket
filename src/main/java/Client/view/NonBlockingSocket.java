package main.java.Client.view;

import java.net.InetSocketAddress;
import java.util.Scanner;

import main.java.Client.net.CommunicationListener;
import main.java.Client.net.ServerConnection;

public class NonBlockingSocket implements Runnable{
	private static final String PROMPT = "> ";
    private final Scanner console = new Scanner(System.in);
    private final ThreadSafeStdOut outMgr = new ThreadSafeStdOut();
    private boolean receivingCmds = false;
    private ServerConnection server;



    /**
     * Interprets and performs user commands.
     */
    public void run() {
        while (receivingCmds) {
            try {
                CmdLine cmdLine = new CmdLine(readNextLine());
                switch (cmdLine.getCmd()) {
                    case QUIT:
                        receivingCmds = false;
                        server.disconnect();
                        break;
                    case CONNECT:
                        server.addCommunicationListener(new ConsoleOutput());
                        server.connect(cmdLine.getParameter(0),
                                       Integer.parseInt(cmdLine.getParameter(1)));
                        System.out.println("Type start to start the game!");
                        break;
                    case USER:
                        server.sendUsername(cmdLine.getParameter(0));
                        break;
                    default:
                        server.sendChatEntry(cmdLine.getUserInput());
                }
            } catch (Exception e) {
                outMgr.println("Operation failed");
            }
        }
    }

    private String readNextLine() {
        outMgr.print(PROMPT);
        return console.nextLine();
    }

    private class ConsoleOutput implements CommunicationListener {
        public void recvdMsg(String msg) {
            printToConsole(msg);
        }

        public void connected(InetSocketAddress serverAddress) {
            printToConsole("Connected to " + serverAddress.getHostName() + ":"
                           + serverAddress.getPort());
        }

        public void disconnected() {
            printToConsole("Disconnected from server.");
        }

        private void printToConsole(String output) {
            outMgr.println(output);
            outMgr.print(PROMPT);
        }
    }


	public void start() {
		// TODO Auto-generated method stub
		if (receivingCmds) {
            return;
        }
        receivingCmds = true;
        server = new ServerConnection();
        new Thread(this).start();
	}
}
