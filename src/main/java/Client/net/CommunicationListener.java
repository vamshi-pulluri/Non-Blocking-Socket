package main.java.Client.net;

import java.net.InetSocketAddress;

public interface CommunicationListener {
	public void recvdMsg(String msg);

    public void connected(InetSocketAddress serverAddress);

    public void disconnected();
}
