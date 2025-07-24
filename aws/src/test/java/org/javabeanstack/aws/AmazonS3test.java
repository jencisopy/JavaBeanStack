/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301  USA
 */
package org.javabeanstack.aws;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;
import static org.javabeanstack.aws.S3Util.*;

/**
 *
 * @author Jorge Enciso
 */
public class AmazonS3test {

    static String accessKey;
    static String secretKey;
    static Region region;
    static S3Client s3Client;

    @BeforeClass
    public static void setUpClass() {
        accessKey = "";
        secretKey = "";
        region = Region.SA_EAST_1;
        s3Client = S3Util.newS3Client(accessKey, secretKey, region);
    }


    @Test
    public void test02CreateBucket() throws Exception {
        String bucketName = "oym-bucket-prb";
        if (!isBucketExists(s3Client, bucketName)) {
            createBucket(s3Client, bucketName);
        }
        assertTrue(isBucketExists(s3Client, bucketName));
    }

    @Test
    public void test03ListBucket() throws Exception {
        List<Bucket> allBuckets = listBuckets(s3Client);
        assertNotNull(allBuckets);
    }

    @Test
    public void test04DeleteBucket() throws Exception {
        String bucketName = "oym-bucket-prb";
        deleteBucket(s3Client, bucketName);
        assertFalse(isBucketExists(s3Client, bucketName));        
    }

    @Test
    public void test05CopyObject() throws Exception {
        String bucketName = "oym.backup";
        File file = new File("/Proyectos/Java/JavaBeanStack/aws/src/test/archivo_enviar.txt");

        Map<String, String> metadata = new HashMap();
        metadata.put("company", "oym");
        metadata.put("info", "prueba");
        
        copyObject(s3Client, bucketName, file, metadata);
    }

    @Test
    public void test06ListObject() throws Exception {
        String bucketName = "oym.backup";
        List<S3Object> contents = listObject(s3Client, bucketName);
        System.out.println("Number of objects in the bucket: " + contents.stream().count());
        contents.stream().forEach(System.out::println);
    }

    @Test
    public void test07DeleteObject() throws Exception {
        String bucketName = "oym.backup";
        String objectKey = "archivo_enviar.txt";
        deleteObject(s3Client, bucketName, objectKey);
    }

    @AfterClass
    public static void tearDownClass() {
        s3Client.close();
    }
}
