import com.google.gson.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;

public class HttpRequest {
    /**
     * @param sheetid
     * @param range
     * @param key
     * @return
     * @throws IOException
     */
    public String Request(String sheetid, String range, String key) throws IOException {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/"  + sheetid + "/values/" + range + "?key=" + key)
            .build(); // defaults to GET

        Response response = client.newCall(request).execute();   
        String stringresponse = response.body().string();
        
        return stringresponse;

    }

}

