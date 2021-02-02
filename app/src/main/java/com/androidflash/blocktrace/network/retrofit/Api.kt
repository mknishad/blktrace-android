package com.androidflash.blocktrace.network.retrofit

import com.androidflash.blocktrace.network.response.FileResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*


interface Api {
  @FormUrlEncoded
  @POST("buckets")
  fun createBuckets(
      @Field("name") name: String
  ): Call<String>

  @FormUrlEncoded
  @PUT("files")
  fun putFiles(
      @Field("deviceUid") deviceUid: String,
      @Field("bucketName") bucketName: String,
      @Field("timestamp") timestamp: String
  ): Call<FileResponse>

  @Multipart
  @PUT("{address}")
  fun uploadFile(
      @Path("address") address: String,
      @Part file: MultipartBody.Part
  ): Call<String>
}