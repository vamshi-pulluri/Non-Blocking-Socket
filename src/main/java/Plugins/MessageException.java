package main.java.Plugins;

public class MessageException extends RuntimeException {
	public MessageException(String msg) {
		super(msg);
	}

	public MessageException(Throwable rootCause) {
		super(rootCause);
	}

	public MessageException(String msg, Throwable rootCause) {
		super(msg, rootCause);
	}
}
