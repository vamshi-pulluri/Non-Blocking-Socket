package main.java.Server.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

import main.java.Plugins.Constant;
import main.java.Plugins.MessageException;
import main.java.Plugins.MessageSplitter;
import main.java.Plugins.MsgType;
import main.java.Server.startup.HangmanServer;


public class ClientHandler implements Runnable{
	private static final String JOIN_MESSAGE = " joined conversation.";
	private static final String LEAVE_MESSAGE = " left conversation.";
	private static final String USERNAME_DELIMETER = ": ";

	private final HangmanServer server;
	private final SocketChannel clientChannel;
	private final ByteBuffer msgFromClient = ByteBuffer.allocateDirect(Constant.MAX_MSG_LENGTH);
	private final MessageSplitter msgSplitter = new MessageSplitter();
	private String username = "anonymous";

	private String currWord;
	private String guessedLetters;
	private int Remaining;
	private int score;
	private final ArrayList<String> wordlist;

	/**
	 * Creates a new instance, which will handle communication with one specific
	 * client connected to the specified channel.
	 *
	 * @param clientChannel
	 *            The socket to which this handler's client is connected.
	 * @throws IOException
	 */
	public ClientHandler(HangmanServer server, SocketChannel clientChannel, ArrayList<String> wordlist) {
		this.wordlist = wordlist;
		this.server = server;
		this.clientChannel = clientChannel;
	}

	/**
	 * Receives and handles one message from the connected client.
	 */
	@Override
	public void run() {
		while (msgSplitter.hasNext()) {
			Message msg = new Message(msgSplitter.nextMsg());

			//System.out.println(msg.msgBody);
			//System.out.println(msg.msgType);
			StringBuilder sb = HangmanGame(msg.msgBody);

			switch (msg.msgType) {
			case USER:
				username = msg.msgBody;
				server.broadcast(username + JOIN_MESSAGE);
				break;
			case ENTRY:
				server.broadcast("Client Request - "+msg.msgBody+" || Server Response: " + sb.toString());
				break;
			case DISCONNECT:
				server.broadcast(username + LEAVE_MESSAGE);
				break;
			default:
				throw new MessageException("Received corrupt message: " + msg.receivedString);
			}
		}
	}

	/**
	 * Sends the specified message to the connected client.
	 *
	 * @param msg
	 *            The message to send.
	 * @throws IOException
	 *             If failed to send message.
	 */
	public void sendMsg(ByteBuffer msg) throws IOException {
		clientChannel.write(msg);
		if (msg.hasRemaining()) {
			throw new MessageException("Could not send message");
		}
	}

	/**
	 * Reads a message from the connected client, then submits a task to the default
	 * <code>ForkJoinPool</code>. That task which will handle the received message.
	 *
	 * @throws IOException
	 *             If failed to read message
	 */
	public void recvMsg() throws IOException {
		msgFromClient.clear();
		int numOfReadBytes;
		numOfReadBytes = clientChannel.read(msgFromClient);
		if (numOfReadBytes == -1) {
			throw new IOException("Client has closed connection.");
		}
		String recvdString = extractMessageFromBuffer();
		msgSplitter.appendRecvdString(recvdString);
		ForkJoinPool.commonPool().execute(this);
	}

	private String extractMessageFromBuffer() {
		msgFromClient.flip();
		byte[] bytes = new byte[msgFromClient.remaining()];
		msgFromClient.get(bytes);
		return new String(bytes);
	}

	/**
	 * Closes this instance's client connection.
	 *
	 * @throws IOException
	 *             If failed to close connection.
	 */
	public void disconnectClient() throws IOException {
		clientChannel.close();
	}

	private static class Message {
		private MsgType msgType;
		private String msgBody;
		private String receivedString;

		private Message(String receivedString) {
			parse(receivedString);
			this.receivedString = receivedString;
		}

		private void parse(String strToParse) {
			try {
				String[] msgTokens = strToParse.split(Constant.MSG_TYPE_DELIMETER);
				msgType = MsgType.valueOf(msgTokens[Constant.MSG_TYPE_INDEX].toUpperCase());
				if (hasBody(msgTokens)) {
					msgBody = msgTokens[Constant.MSG_BODY_INDEX].trim();
				}
			} catch (Throwable throwable) {
				throw new MessageException(throwable);
			}
		}

		private boolean hasBody(String[] msgTokens) {
			return msgTokens.length > 1;
		}
	}

	private String getWordFromDictionary() {
		Random rand = new Random();
		return wordlist.get(rand.nextInt(wordlist.size()));
	}

	private String ReceiveWordFromServer() {

		System.out.println(guessedLetters);
		if (guessedLetters.length() > 0) {
			return currWord.replaceAll("[^" + guessedLetters + "]", "-");
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < currWord.length(); i++) {
				sb.append("-");
			}
			return sb.toString();
		}
	}

	StringBuilder HangmanGame(String msg) {
		String str;
		while ((str = msg) != null) {
			// When you press the new game key
			if (str.equals("start")) {
				currWord = getWordFromDictionary();
				Remaining = currWord.length();
				guessedLetters = "";
				StringBuilder sb = new StringBuilder();
				sb.append(ReceiveWordFromServer()).append(',').append("Guess the word!").append(',').append(Remaining)
						.append(',').append(score);
				return sb;
			}
			// When the correct word is found
			else if (str.equals(currWord)) {
				score++;
				StringBuilder sb = new StringBuilder();
				sb.append(currWord).append(',').append("Congratulations! You won!").append(',').append(Remaining)
						.append(',').append(score);
				return sb;
			}

			else if (str.length() == 1) {
				// When 1character is guessed by the user
				String receivedWord = ReceiveWordFromServer();
				guessedLetters += str;
				String updatedWord = ReceiveWordFromServer();

				// When the complete word is found
				if (updatedWord.equals(currWord)) {
					score++;
					StringBuilder sb = new StringBuilder();
					sb.append(currWord).append(',').append("Congratulations! You won!").append(',').append(Remaining)
							.append(',').append(score);
					return sb;
				} else if (receivedWord.equals(updatedWord)) {
					// If your Guess is wrong
					Remaining--;
					// If no of attempts remaining=0,Sorry, game done!
					if (Remaining == 0) {
						score--;
						StringBuilder sb = new StringBuilder();
						sb.append(currWord).append(',').append("Game Over!").append(',').append(Remaining).append(',')
								.append(score);
						return sb;
					} else {
						// If your guess was wrong!!
						StringBuilder sb = new StringBuilder();
						sb.append(updatedWord).append(',').append("Guess was wrong!!").append(',').append(Remaining)
								.append(',').append(score);
						return sb;
					}
				} else {
					// Guess is right!!
					StringBuilder sb = new StringBuilder();
					sb.append(updatedWord).append(',').append("Guess was right!!").append(',').append(Remaining)
							.append(',').append(score);
					return sb;
				}
			} else {
				Remaining--;
				if (Remaining == 0) {
					score--;
					System.out.println("6");
					StringBuilder sb = new StringBuilder();
					sb.append(currWord).append(',').append("Game Over!").append(',').append(Remaining).append(',')
							.append(score);
					return sb;
				} else {
					StringBuilder sb = new StringBuilder();
					sb.append(ReceiveWordFromServer()).append(',').append("Guess was wrong!!").append(',')
							.append(Remaining).append(',').append(score);
					return sb;
				}
			}
		}
		return null;
	}
}
