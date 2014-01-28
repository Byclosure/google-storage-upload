package com.byclosure.maven.plugins;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
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
	 * Global configuration of Google Cloud Storage OAuth 2.0 scope.
	 **/
	@Parameter(property = "google-storage-upload.storageScope", defaultValue = "https://www.googleapis.com/auth/devstorage.full_control")
	private String storageScope;

	/**
	 * The bucket to upload into.
	 **/
	@Parameter(property = "google-storage-upload.bucketName", required = true)
	private String bucketName;

	/**
	 * The file/folder to upload.
	 **/
	@Parameter(property = "google-storage-upload.source", required = true)
	private File source;

	/**
	 * The folder (in the bucket) to create.
	 **/
	@Parameter(property = "google-storage-upload.destination", defaultValue = "/")
	private String destination;

	/**
	 * Execute all steps except the upload to the google storage. This can be
	 * set to true to perform a "dryRun" execution.
	 **/
	@Parameter(property = "google-storage-upload.doNotUpload", defaultValue = "false")
	private boolean doNotUpload;

	/**
	 * In the case of a directory upload, recursively upload the contents.
	 **/
	@Parameter(property = "google-storage-upload.recursive", defaultValue = "false")
	private boolean recursive;

	/** Global instance of the HTTP transport. */
	private static HttpTransport httpTransport;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	/** Global instance of the HttpRequest factory. */
	private static HttpRequestFactory requestFactory;

	public void execute() throws MojoExecutionException {
		if (!source.exists()) {
			throw new MojoExecutionException("File/folder doesn't exist: "
					+ source);
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

			List<File> files = getUploadFiles(source);
			for (File file : files) {
				uploadFile(file);
			}
		} catch (GeneralSecurityException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private List<File> getUploadFiles(File fileOrDirectory)
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
					if (!files.addAll(getUploadFiles(subFile))) {
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

	private void uploadFile(File file) throws IOException {
		String type = "text/plain";

		FileContent putFC = new FileContent(type, file);

		String putURI = "https://storage.googleapis.com/" + bucketName + "/"
				+ destination + "/" + file.getName();
		getLog().info("putURI - " + putURI);

		GenericUrl putUrl = new GenericUrl(putURI);
		getLog().info("putUrl - " + putUrl);

		HttpRequest putRequest = requestFactory.buildPutRequest(putUrl, putFC);
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