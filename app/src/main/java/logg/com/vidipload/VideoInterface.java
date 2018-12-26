package logg.com.vidipload;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface VideoInterface {
    @Multipart
    @POST("dev/api/api.php")
    Call<UploadResponse> uploadVideoToServer(@Part("customer_id") RequestBody customer_id,@Query("req") String type, @Part MultipartBody.Part body);
}