package net.beadsproject.beads.data.audiofile;

/**
 * Exception for when an operation requested of an AudioFileReader or AudioFileWriter is not supported (but perhaps could be in the future, or 
 * by another class implementing the same interface), e.g. attempting to write a file type that is not currently supported.
 * @author aengus
 *
 */
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
