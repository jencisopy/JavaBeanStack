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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
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
 * 
 * Metodos para la gestión de recursos amazon S3.
 */
public class S3Util {
    /**
     * Crea un cliente para conexión al servicio. 
     * @param accessKey key de acceso.
     * @param secretKey password
     * @param region    region 
     * @return 
     */
    public static S3Client newS3Client(String accessKey, String secretKey, Region region) {
        AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3Client newS3Client = S3Client
                .builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
        return newS3Client;
    }

    /**
     * Ve si existe o no el bucket.
     * 
     * @param s3Client cliente para la conexión al servicio.
     * @param bucketName nombre del bucket (contenedor de datos o carpeta)
     * @return verdadero si existe y falso si no.
     */
    public static boolean isBucketExists(S3Client s3Client, String bucketName) {
        try {
            s3Client.headBucket(request -> request.bucket(bucketName));
            return true;
        } catch (NoSuchBucketException exception) {
            return false;
        }
    }
    
    /**
     * Crea un bucket 
     * 
     * @param s3Client cliente para la conexión al servicio.
     * @param bucketName nombre del bucket, no debe existir.
     * @return verdadero si tuvo exito, falso si no.
     */
    public static boolean createBucket(S3Client s3Client, String bucketName){
        if (!isBucketExists(s3Client, bucketName)) {
            s3Client.createBucket(request -> request.bucket(bucketName));
        }
        return isBucketExists(s3Client, bucketName);
    }

    /**
     * Devuelve todos los buckets de la conexión.
     * 
     * @param s3Client cliente para la conexión al servicio.
     * @return lista de buckets.
     */
    public static List<Bucket> listBuckets(S3Client s3Client) {
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
        return allBuckets;
    }

    /**
     * Borra un bucket, siempre que este vacio y no tenga objectos.
     * @param s3Client cliente para la conexión al servicio.
     * @param bucketName nombre del bucket.
     * @throws Exception 
     */
    public static void deleteBucket(S3Client s3Client, String bucketName) throws Exception {
        try {
            s3Client.deleteBucket(request -> request.bucket(bucketName));
        } catch (S3Exception exception) {
            if (exception.statusCode() == 409) {
                throw new Exception("No esta vacio");
            }
            throw exception;
        }
    }
    
    /**
     * Copia un archivo a un bucket.
     * 
     * @param s3Client cliente para la conexión al servicio.
     * @param bucketName nombre del bucket.
     * @param file    archivo a copiar al bucket
     * @param metadata metadata para el archivo.
     * @throws Exception 
     */
    public static void copyObject(S3Client s3Client, String bucketName, File file, Map<String, String> metadata) throws Exception {
        s3Client.putObject(request
                -> request
                        .bucket(bucketName)
                        .key(file.getName())
                        .metadata(metadata)
                        .ifNoneMatch("*"), //si no existe
                file.toPath());
    }

    /**
     * Devuelve una lista de objetos o archivos en un bucket.
     * @param s3Client cliente para la conexión al servicio.
     * @param bucketName nombre del bucket.
     * @return lista de objetos o archivos.
     * @throws Exception 
     */
    public static List<S3Object> listObject(S3Client s3Client, String bucketName) throws Exception {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
        return listObjectsV2Response.contents();
    }

    /**
     * Elimina un objeto o un archivo
     * @param s3Client cliente para la conexión al servicio.
     * @param bucketName nombre del bucket.
     * @param objectKey nombre del objeto o archivo
     * @throws Exception 
     */
    public static void deleteObject(S3Client s3Client, String bucketName, String objectKey) throws Exception {
        s3Client.deleteObject(request
                -> request
                        .bucket(bucketName)
                        .key(objectKey));
    }
    
}
