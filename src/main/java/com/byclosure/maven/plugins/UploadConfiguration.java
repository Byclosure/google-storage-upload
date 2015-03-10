package com.byclosure.maven.plugins;

public class UploadConfiguration {
	private String bucketName;
	private String source;
	private String sourceIncludes;
	private String destination;
	private Boolean isRecursive;

	public UploadConfiguration() {
		super();

		destination = null;
		isRecursive = false;
	}

	public String getSourceIncludes() {
		return sourceIncludes;
	}

	public void setSourceIncludes(String sourceIncludes) {
		this.sourceIncludes = sourceIncludes;
	}

	public Boolean isRecursive() {
		return isRecursive;
	}

	public void setRecursive(Boolean isRecursive) {
		this.isRecursive = isRecursive;
	}


	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}
}
