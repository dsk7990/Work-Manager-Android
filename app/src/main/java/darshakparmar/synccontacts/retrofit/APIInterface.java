package darshakparmar.synccontacts.retrofit;

import darshakparmar.synccontacts.model.ResponseModel;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by darshakparmar on 02/01/18.
 */

public interface APIInterface {


    @POST("adduser.php")
    Call<ResponseModel> sync(@Body RequestBody requestBody);


}
