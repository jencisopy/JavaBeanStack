/*
* Copyright (c) 2015-2017 OyM System Group S.A.
* Capitan Cristaldo 464, Asunción, Paraguay
* All rights reserved. 
*
* NOTICE:  All information contained herein is, and remains
* the property of OyM System Group S.A. and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to OyM System Group S.A.
* and its suppliers and protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from OyM System Group S.A.
 */
package org.javabeanstack.aws;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import static org.junit.Assert.*;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 *
 * @author Jorge Enciso
 */
public class AmazonS3test {

    static String accessKey;
    static String secretKey;
    static AwsCredentials credentials;
    static S3Client s3Client;

    @BeforeClass
    public static void setUpClass() {
        accessKey = "";
        secretKey = "";
        credentials = AwsBasicCredentials.create(accessKey, secretKey);
        s3Client = S3Client
                .builder()
                .region(Region.SA_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    boolean bucketExists(String bucketName) {
        try {
            s3Client.headBucket(request -> request.bucket(bucketName));
            return true;
        } catch (NoSuchBucketException exception) {
            return false;
        }
    }

    @Test
    public void test01Connection() throws Exception {
        credentials = AwsBasicCredentials.create(accessKey, secretKey);
        assertNotNull(credentials);
    }

    @Test
    public void test02CreateBucket() throws Exception {
        String bucketName = "oym-bucket-prb";
        if (!bucketExists(bucketName)) {
            s3Client.createBucket(request -> request.bucket(bucketName));
        }
        assertTrue(bucketExists(bucketName));
    }

    @Test
    public void test03ListBucket() throws Exception {
        List<Bucket> allBuckets = new ArrayList();
        String nextToken = null;

        do {
            String continuationToken = nextToken;
            ListBucketsResponse listBucketsResponse = s3Client.listBuckets(
                    request -> request.continuationToken(continuationToken)
            );

            allBuckets.addAll(listBucketsResponse.buckets());
            nextToken = listBucketsResponse.continuationToken();
        } while (nextToken != null);
        assertNotNull(allBuckets);
    }

    @Test
    public void test04DeleteBucket() throws Exception {
        String bucketName = "oym-bucket-prb";
        try {
            s3Client.deleteBucket(request -> request.bucket(bucketName));
        } catch (S3Exception exception) {
            if (exception.statusCode() == HttpStatus.SC_CONFLICT) {
                throw new Exception("No esta vacio");
            }
            throw exception;
        }
    }

    @Test
    public void test05PutObject() throws Exception {
        String bucketName = "oym.backup";
        File file = new File("/Proyectos/Java/JavaBeanStack/aws/src/test/archivo_enviar.txt");

        Map<String, String> metadata = new HashMap();
        metadata.put("company", "oym");
        metadata.put("info", "prueba");

        s3Client.putObject(request
                -> request
                        .bucket(bucketName)
                        .key(file.getName())
                        .metadata(metadata)
                        .ifNoneMatch("*"), //si no existe
                file.toPath());

    }

    @Test
    public void test06ListObject() throws Exception {
        String bucketName = "oym.backup";
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
        List<S3Object> contents = listObjectsV2Response.contents();

        System.out.println("Number of objects in the bucket: " + contents.stream().count());
        contents.stream().forEach(System.out::println);
    }

    @Test
    public void test07DeleteObject() throws Exception {
        String bucketName = "oym.backup";
        String objectKey = "archivo_enviar.txt";
        s3Client.deleteObject(request
                -> request
                        .bucket(bucketName)
                        .key(objectKey));
    }

    @AfterClass
    public static void tearDownClass() {
        s3Client.close();
    }

//
//
//String bucketName = "baeldung-bucket";
//try {
//    s3Client.deleteBucket(request -> request.bucket(bucketName));
//} catch (S3Exception exception) {
//    if (exception.statusCode() == HttpStatus.SC_CONFLICT) {
//        throw new BucketNotEmptyException();
//    }
//    throw exception;
//}
//
//String bucketName = "baeldung-bucket";
//File file = new File("path-to-file");
//
//Map<String, String> metadata = new HashMap<>();
//metadata.put("company", "Baeldung");
//metadata.put("environment", "development")
//
//s3Client.putObject(request -> 
//  request
//    .bucket(bucketName)
//    .key(file.getName())
//    .metadata(metadata)
//    .ifNoneMatch("*"), 
//  file.toPath());
}
