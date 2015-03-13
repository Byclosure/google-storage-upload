package com.byclosure.maven.plugins;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.codehaus.plexus.util.FileUtils;

/**
 * Goal which uploads a file (or directory) to google storage
 */
@Mojo(name = "google-storage-upload", requiresProject = false)
public class GoogleStorageUploadMojo extends AbstractMojo {
	/**
	 * E-mail address of the service account.
	 **/
	@Parameter(property = "google-storage-upload.serviceAccountEmail", required = true)
	private String serviceAccountEmail;

	/**
	 * Key P12 file for service account.
	 **/
	@Parameter(property = "google-storage-upload.keyP12", required = true)
	private File keyP12;

	/**
	 * Google Cloud Storage OAuth 2.0 scope.
	 **/
	@Parameter(property = "google-storage-upload.storageScope", defaultValue = "https://www.googleapis.com/auth/devstorage.full_control")
	private String storageScope;
	
	/**
	 * x-goog-acl header value. Read https://developers.google.com/storage/docs/reference-headers#xgoogacl
	 **/
	@Parameter(property = "google-storage-upload.xGoogAcl", defaultValue = "public-read")
	private String xGoogAcl;

	@Parameter(property = "google-storage-upload.upload", required = true)
	private List upload;

	/**
	 * Execute all steps except the upload to the google storage. This can be
	 * set to true to perform a "dryRun" execution.
	 **/
	@Parameter(property = "google-storage-upload.doNotUpload", defaultValue = "false")
	private boolean doNotUpload;

	/** Global instance of the HTTP transport. */
	private static HttpTransport httpTransport;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	/** Global instance of the HttpRequest factory. */
	private static HttpRequestFactory requestFactory;

	public void execute() throws MojoExecutionException {
		if (upload == null) {
			throw new MojoExecutionException("Copy isn't defined");
		}

		for (Object obj : upload) {
			final UploadConfiguration uploadDefinition = (UploadConfiguration)obj;
			final File sourceFile = new File(uploadDefinition.getSource());

			if (sourceFile.isDirectory()) {
				final List<File> files;
				try {
					files = FileUtils.getFiles(sourceFile, uploadDefinition.getSourceIncludes(), null);
				} catch (IOException e) {
					throw new MojoExecutionException(e.getMessage());
				}

				for (File file : files) {
					uploadFile(uploadDefinition, file);
				}

			} else {
				uploadFile(uploadDefinition, new File(uploadDefinition.getSource()));
			}
		}
	}

	private void uploadFile(UploadConfiguration uploadDefinition, File sourceFile) throws MojoExecutionException {
		if (!sourceFile.exists()) {
			throw new MojoExecutionException("File/folder doesn't exist: "
					+ uploadDefinition.getSource());
		}
		if (uploadDefinition.getDestination() != null &&
				(uploadDefinition.getDestination().startsWith("/") || !uploadDefinition.getDestination().endsWith("/"))) {
			throw new MojoExecutionException("destination can't start with /(slash) and can't end without it: "
					+ uploadDefinition.getDestination());
		}
		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			// Build service account credential.
			GoogleCredential credential = new GoogleCredential.Builder()
					.setTransport(httpTransport)
					.setJsonFactory(JSON_FACTORY)
					.setServiceAccountId(serviceAccountEmail)
					.setServiceAccountScopes(
							Collections.singleton(storageScope))
					.setServiceAccountPrivateKeyFromP12File(keyP12).build();
			requestFactory = httpTransport.createRequestFactory(credential);

			List<File> files = getUploadFiles(sourceFile, uploadDefinition.isRecursive());
			for (File file : files) {
				uploadFile(file, uploadDefinition.getBucketName(), uploadDefinition.getDestination());
			}
		} catch (GeneralSecurityException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private List<File> getUploadFiles(File fileOrDirectory, boolean recursive)
			throws MojoExecutionException {
		List<File> files = new ArrayList<File>();
		if (fileOrDirectory.isFile()) {
			if (!files.add(fileOrDirectory)) {
				throw new MojoExecutionException("Couldn't add file "
						+ fileOrDirectory.getPath());
			} else {
				getLog().info(
						"Added file to upload list "
								+ fileOrDirectory.getPath());
			}
		} else if (fileOrDirectory.isDirectory()) {
			for (File subFile : fileOrDirectory.listFiles()) {
				if (!subFile.isDirectory() || recursive) {
					if (!files.addAll(getUploadFiles(subFile, recursive))) {
						throw new MojoExecutionException(
								"Couldn't add all files in directory "
										+ fileOrDirectory.getPath());
					} else {
						getLog().info(
								"Added files to upload list in directory "
										+ fileOrDirectory.getPath());
					}
				}
			}
		} else {
			throw new MojoExecutionException(
					"File is neither a regular file nor a directory "
							+ fileOrDirectory.getPath());
		}
		return files;
	}

	private void uploadFile(File file, String bucketName, String destination) throws IOException {
		String type = "binary/octet-stream";

		FileContent putFC = new FileContent(type, file);

		String putURI = "https://" + bucketName + ".storage.googleapis.com/";
		if (destination != null) {
			putURI += destination;
		}
		putURI += file.getName();
		getLog().info("putURI - " + putURI);

		GenericUrl putUrl = new GenericUrl(putURI);
		getLog().info("putUrl - " + putUrl);

		HttpRequest putRequest = requestFactory.buildPutRequest(putUrl, putFC);
		putRequest.getHeaders().set("x-goog-acl", xGoogAcl);
		getLog().info("putRequest headers - " + putRequest.getHeaders());

		if (!doNotUpload) {
			HttpResponse putResponse = putRequest.execute();
			getLog().info(
					"Uploaded with " + putResponse.getStatusCode() + " "
							+ putResponse.getStatusMessage());
		} else {
			getLog().info("Doing dryrun");
		}
	}

	// TODO: Check if this works and add support for any mime type!!!
	@SuppressWarnings("unused")
	private String getMimeType(File file) {
		String fileUrl = "file://" + file.getAbsolutePath();
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		return fileNameMap.getContentTypeFor(fileUrl);
	}

}