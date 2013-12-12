package net.beadsproject.beads.data.audiofile;

public class OperationUnsupportedException extends Exception {
	private static final long serialVersionUID = 1L;
	private String operation;

	public OperationUnsupportedException(String operation) {
		super(operation);
		this.operation = operation;
	}

	public String getError() {
		return this.operation;
	}
}
