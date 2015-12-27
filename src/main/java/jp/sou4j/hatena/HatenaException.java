package jp.sou4j.hatena;

public class HatenaException extends Exception {

	private static final long serialVersionUID = 1L;

	public HatenaException() {
		super();
	}

	public HatenaException(String message) {
		super(message);
	}

	public HatenaException(String message, Throwable cause) {
		super(message, cause);
	}

	public HatenaException(Throwable cause) {
		super(cause);
	}
}
