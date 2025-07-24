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
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 *
 * @author Jorge Enciso
 */
public class S3Manager {
    private S3Client s3Client = null;

    public S3Client getS3Client() {
        return s3Client;
    }

    public void setS3Client(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public boolean isBucketExists(String bucketName) {
        if (s3Client == null) {
            return false;
        }
        return S3Util.isBucketExists(s3Client, bucketName);
    }

    public boolean createBucket(String bucketName) {
        if (s3Client == null) {
            return false;
        }
        return S3Util.createBucket(s3Client, bucketName);
    }

    public List<Bucket> listBuckets() {
        if (s3Client == null) {
            return null;
        }
        return S3Util.listBuckets(s3Client);
    }

    public void deleteBucket(String bucketName) throws Exception {
        if (s3Client == null) {
            throw new Exception("No esta instanciado el cliente para la conexión");
        }
        S3Util.deleteBucket(s3Client, bucketName);
    }

    public List<S3Object> listObject(String bucketName) throws Exception {
        if (s3Client == null) {
            throw new Exception("No esta instanciado el cliente para la conexión");
        }
        return S3Util.listObject(s3Client, bucketName);
    }

    public void copyObject(String bucketName, File file, Map<String, String> metadata) throws Exception {
        if (s3Client == null) {
            throw new Exception("No esta instanciado el cliente para la conexión");
        }
        S3Util.copyObject(s3Client, bucketName, file, metadata);
    }
    
    public void deleteObject(String bucketName, String objectKey) throws Exception {
        if (s3Client == null) {
            throw new Exception("No esta instanciado el cliente para la conexión");
        }
        S3Util.deleteObject(s3Client, bucketName, objectKey);
    }
}
