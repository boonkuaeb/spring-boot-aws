package com.bk.springbootaws.service;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.bk.springbootaws.exception.FileArchiveServiceException;
import com.bk.springbootaws.model.CustomerImage;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;

@Service
public class FileArchiveService {


	private AmazonS3 s3Client;

	@Value("${aws.s3.endpoint}")
	private String endpointUrl;
	@Value("${aws.s3.bucket}")
	private String bucketName;
	@Value("${aws.s3.accessKeyId}")
	private String accessKey;
	@Value("${aws.s3.secretAccessKey}")
	private String secretKey;

	@PostConstruct
	private void initializeAmazon() {


		AWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setSignerOverride("AWSS3V4SignerType");

		 this.s3Client = AmazonS3ClientBuilder
				.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(this.endpointUrl, Regions.AP_SOUTHEAST_1.name()))
				.withPathStyleAccessEnabled(true)
				.withClientConfiguration(clientConfiguration)
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.build();


		System.out.println(this.endpointUrl);
		System.out.println(this.bucketName);
		System.out.println(this.accessKey);
		System.out.println(this.secretKey);

	}
	/**
	 * Save image to S3 and return CustomerImage containing key and public URL
	 * 
	 * @param multipartFile
	 * @return
	 * @throws IOException
	 */
	public CustomerImage saveFileToS3(MultipartFile multipartFile) throws FileArchiveServiceException {

		try{
			File fileToUpload = convertFromMultiPart(multipartFile);
			String key = Instant.now().getEpochSecond() + "_" + fileToUpload.getName();

			/* save file */
			s3Client.putObject(new PutObjectRequest(this.bucketName, key, fileToUpload));

			/* get signed URL (valid for one year) */
			GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(this.bucketName, key);
			generatePresignedUrlRequest.setMethod(HttpMethod.GET);
			generatePresignedUrlRequest.setExpiration(DateTime.now().plusDays(7).toDate());

			URL signedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

			return new CustomerImage(key, signedUrl.toString());
		}
		catch(Exception ex){			
			throw new FileArchiveServiceException("An error occurred saving file to S3", ex);
		}		
	}

	/**
	 * Delete image from S3 using specified key
	 * 
	 * @param customerImage
	 */
	public void deleteImageFromS3(CustomerImage customerImage){
		s3Client.deleteObject(new DeleteObjectRequest(this.bucketName, customerImage.getKey()));	
	}

	/**
	 * Convert MultiPartFile to ordinary File
	 * 
	 * @param multipartFile
	 * @return
	 * @throws IOException
	 */
	private File convertFromMultiPart(MultipartFile multipartFile) throws IOException {

		File file = new File(multipartFile.getOriginalFilename());
		file.createNewFile(); 
		FileOutputStream fos = new FileOutputStream(file); 
		fos.write(multipartFile.getBytes());
		fos.close(); 

		return file;
	}
}