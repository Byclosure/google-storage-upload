##GOOGLE STORAGE UPLOAD

A maven plugin for uploading files to a google cloud storage bucket

##How to use

Add the plugin dependency to your pom.xml

```xml
<plugin>
	<groupId>com.byclosure.maven.plugins</groupId>
	<artifactId>google-storage-upload</artifactId>
	<version>0.0.2</version>
</plugin>
```

Configure the credentials
```xml
<plugin>
	<groupId>com.byclosure.maven.plugins</groupId>
	<artifactId>google-storage-upload</artifactId>
	<version>0.0.2</version>
	<configuration>
		<serviceAccountEmail>SERVICE_ACCOUNT_EMAIL</serviceAccountEmail>
		<keyP12>P12_FILE</keyP12>
	</configuration>
</plugin>
````

Configure the files to upload
```xml
<plugin>
	<groupId>com.byclosure.maven.plugins</groupId>
	<artifactId>google-storage-upload</artifactId>
	<version>0.0.2</version>
	<configuration>
		<serviceAccountEmail>SERVICE_ACCOUNT_EMAIL</serviceAccountEmail>
		<keyP12>P12_FILE</keyP12>
		<upload>
			<UploadConfiguration>
				<bucketName>html_bucket</bucketName>
				<source>${project.build.directory}/${tmpTargetDirectory}</source>
				<sourceIncludes>*.html</sourceIncludes>
			</UploadConfiguration>
			<UploadConfiguration>
				<bucketName>css_bucket</bucketName>
				<source>${project.build.directory}/${tmpTargetDirectory}</source>
				<sourceIncludes>*.css</sourceIncludes>
				<destination>folder/on/bucket/</destination>
			</UploadConfiguration>
		</upload>
	</configuration>
</plugin>
```

To invoke upload
```bash
mvn byclosure-plugins:google-storage-upload
```


## Contributions

We welcome all the help we can get!
