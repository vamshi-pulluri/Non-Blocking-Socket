package main.java.Server.startup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.Queue;
import java.util.StringJoiner;

import main.java.Plugins.Constant;
import main.java.Plugins.MessageException;
import main.java.Plugins.MessageSplitter;
import main.java.Plugins.MsgType;
import main.java.Server.controller.Controller;
import main.java.Server.net.ClientHandler;

public class HangmanServer {

	private static final int LINGER_TIME = 5000;
	private static final int TIMEOUT_HALF_HOUR = 1800000;
	private final Controller contr = new Controller();
	private final Queue<ByteBuffer> messagesToSend = new ArrayDeque<>();
	private int portNo = 8080;
	private Selector selector;
	private ServerSocketChannel listeningSocketChannel;
	private volatile boolean timeToBroadcast = false;
	private static ArrayList<String> words;

	private void parseArguments(String[] arguments) {
		if (arguments.length > 0) {
			try {
				portNo = Integer.parseInt(arguments[1]);
			} catch (NumberFormatException e) {
				System.err.println("Invalid port number, using default.");
			}
		}
	}

	public void broadcast(String msg) {
		contr.appendEntry(msg);
		timeToBroadcast = true;
		ByteBuffer completeMsg = createBroadcastMessage(msg);
		synchronized (messagesToSend) {
			messagesToSend.add(completeMsg);
		}
		selector.wakeup();
	}

	private ByteBuffer createBroadcastMessage(String msg) {
		StringJoiner joiner = new StringJoiner(Constant.MSG_TYPE_DELIMETER);
		joiner.add(MsgType.BROADCAST.toString());
		joiner.add(msg);
		String messageWithLengthHeader = MessageSplitter.prependLengthHeader(joiner.toString());
		return ByteBuffer.wrap(messageWithLengthHeader.getBytes());
	}

	private void serve() {
		try {
			initSelector();
			int port=0;
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				System.out.print("Enter Hangman Server Port: ");
				port = Integer.parseInt(br.readLine());
			} catch (InputMismatchException e) {
				System.out.print("Invalid Input / Starting with default Port: 8080");
				port = 8080;
			}
			initListeningSocketChannel(port);
			while (true) {
				if (timeToBroadcast) {
					writeOperationForAllActiveClients();
					appendMsgToAllClientQueues();
					timeToBroadcast = false;
				}
				selector.select();
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					if (!key.isValid()) {
						continue;
					}
					if (key.isAcceptable()) {
						startHandler(key);
					} else if (key.isReadable()) {
						recvFromClient(key);
					} else if (key.isWritable()) {
						sendToClient(key);
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Server failure.");
		}
	}

	private void startHandler(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel clientChannel = serverSocketChannel.accept();
		clientChannel.configureBlocking(false);
		ClientHandler handler = new ClientHandler(this, clientChannel, words);
		clientChannel.register(selector, SelectionKey.OP_WRITE, new Client(handler, contr.getConversation()));
		clientChannel.setOption(StandardSocketOptions.SO_LINGER, LINGER_TIME); // Close will probably
		// block on some JVMs.
		// clientChannel.socket().setSoTimeout(TIMEOUT_HALF_HOUR); Timeout is not
		// supported on
		// socket channels. Could be implemented using a separate timer that is checked
		// whenever the
		// select() method in the main loop returns.
	}

	private void recvFromClient(SelectionKey key) throws IOException {
		Client client = (Client) key.attachment();
		try {
			client.handler.recvMsg();
		} catch (IOException clientHasClosedConnection) {
			removeClient(key);
		}
	}

	private void sendToClient(SelectionKey key) throws IOException {
		Client client = (Client) key.attachment();
		try {
			client.sendAll();
			key.interestOps(SelectionKey.OP_READ);
		} catch (MessageException couldNotSendAllMessages) {
		} catch (IOException clientHasClosedConnection) {
			removeClient(key);
		}
	}

	private void removeClient(SelectionKey clientKey) throws IOException {
		Client client = (Client) clientKey.attachment();
		client.handler.disconnectClient();
		clientKey.cancel();
	}

	private void initSelector() throws IOException {
		selector = Selector.open();
	}

	private void initListeningSocketChannel(int port) throws IOException {
		listeningSocketChannel = ServerSocketChannel.open();
		listeningSocketChannel.configureBlocking(false);
		if (port != 0)
			listeningSocketChannel.bind(new InetSocketAddress(port));
		else
			listeningSocketChannel.bind(new InetSocketAddress(portNo));
		listeningSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	private void writeOperationForAllActiveClients() {
		for (SelectionKey key : selector.keys()) {
			if (key.channel() instanceof SocketChannel && key.isValid()) {
				key.interestOps(SelectionKey.OP_WRITE);
			}
		}
	}

	private void appendMsgToAllClientQueues() {
		synchronized (messagesToSend) {
			ByteBuffer msgToSend;
			while ((msgToSend = messagesToSend.poll()) != null) {
				for (SelectionKey key : selector.keys()) {
					Client client = (Client) key.attachment();
					if (client == null) {
						continue;
					}
					synchronized (client.messagesToSend) {
						client.queueMsgToSend(msgToSend);

					}
				}
			}
		}
	}

	private class Client {
		private final ClientHandler handler;
		private final Queue<ByteBuffer> messagesToSend = new ArrayDeque<>();

		private Client(ClientHandler handler, String[] conversation) {
			this.handler = handler;
			for (String entry : conversation) {
				messagesToSend.add(createBroadcastMessage(entry));
			}
		}

		private void queueMsgToSend(ByteBuffer msg) {
			synchronized (messagesToSend) {
				messagesToSend.add(msg.duplicate());
			}
		}

		private void sendAll() throws IOException, MessageException {
			ByteBuffer msg = null;
			synchronized (messagesToSend) {
				while ((msg = messagesToSend.peek()) != null) {
					handler.sendMsg(msg);
					messagesToSend.remove();
				}
			}
		}
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String data;
		words = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new FileReader("words.txt"));
		while ((data = reader.readLine()) != null)
			words.add(data);
		reader.close();

		HangmanServer server = new HangmanServer();
		server.parseArguments(args);
		server.serve();
	}

}
