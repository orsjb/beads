package net.beadsproject.beads.data.audiofile;

public class FileFormatException extends Exception{
	private static final long serialVersionUID = 1L;
	private String problem;

	public FileFormatException(String problem) {
		super(problem);
		this.problem = problem;
	}

	public String getError() {
		return this.problem;
	}
}
