package iooojik.app.klass.api;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface FileUploadApi {

    @Multipart
    @POST("upload_file/upload")
    Call<Void> uploadFile(@Part MultipartBody.Part file);

}
