package pmoschos.chatgptapp.activities;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import pmoschos.chatgptapp.R;
import pmoschos.chatgptapp.adapters.PostAdapter;
import pmoschos.chatgptapp.helpers.SQLiteDBHelper;
import pmoschos.chatgptapp.interfaces.ApiResponseCallback;
import pmoschos.chatgptapp.models.Post;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText questionET;
    private Button sendBtn;
    private EditText searchET;
    private ArrayList<Post> postArrayList;
    private PostAdapter postAdapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initRecyclerView();
        loadPostsFromDatabase();
        setupSearchFilter();
        handleSendButton();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        questionET = findViewById(R.id.questionET);
        sendBtn = findViewById(R.id.sendBtn);
        searchET = findViewById(R.id.searchET);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initRecyclerView() {
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        manager.setStackFromEnd(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setHasFixedSize(true);
        postArrayList = new ArrayList<>();
    }

    @SuppressLint("Range")
    private void loadPostsFromDatabase() {
        SQLiteDBHelper sqLiteDBHelper = new SQLiteDBHelper(this);
        Cursor cursor = sqLiteDBHelper.getAllData();

        while (cursor.moveToNext()) {
            postArrayList.add(
                    new Post(
                            cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.POST_ID)),
                            cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.POST_QUESTION)),
                            cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.POST_RESPONSE))
                    ));
        }
        cursor.close();

        if (!postArrayList.isEmpty()) {
            postAdapter = new PostAdapter(postArrayList, this);
            recyclerView.setAdapter(postAdapter);
        }
    }

    private void setupSearchFilter() {
        searchET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                postAdapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void handleSendButton() {
        sendBtn.setOnClickListener(view -> {
            String question = questionET.getText().toString().trim();

            if (question.isEmpty()) {
                questionET.setError("Please insert some text...");
                return;
            }

            // Disable the send button and show the progress bar
            sendBtn.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            callApi(question, response -> {
                // Enable the send button and hide the progress bar
                sendBtn.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response != null && !response.isEmpty()) {
                    addPostToDatabase(question, response);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to get a response", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void addPostToDatabase(String question, String response) {
        SQLiteDBHelper sqLiteDBHelper = new SQLiteDBHelper(this);
        SQLiteDatabase sqLiteDatabase = sqLiteDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SQLiteDBHelper.POST_QUESTION, question);
        values.put(SQLiteDBHelper.POST_RESPONSE, response);

        long newRowNum = sqLiteDatabase.insert(SQLiteDBHelper.TABLE_NAME, null, values);

        if (newRowNum != -1) {
            Post newPost = new Post((int) newRowNum, question, response);

            if (postAdapter == null) {
                postArrayList = new ArrayList<>();
                postArrayList.add(newPost);
                postAdapter = new PostAdapter(postArrayList, this);
                recyclerView.setAdapter(postAdapter);
            } else {
                postArrayList.add(newPost);
                postAdapter.notifyItemInserted(postArrayList.size() - 1);
            }

            recyclerView.scrollToPosition(postArrayList.size() - 1);
            questionET.setText("");
            Toast.makeText(MainActivity.this, "Post created", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Failed to create post", Toast.LENGTH_SHORT).show();
        }
    }

    private void callApi(String question, final ApiResponseCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.openai.com/v1/chat/completions";

        JSONObject requestBodyJson = new JSONObject();
        try {
            requestBodyJson.put("model", "gpt-3.5-turbo");
            JSONArray messagesArray = new JSONArray();
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a helpful assistant.");
            messagesArray.put(systemMessage);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messagesArray.put(userMessage);

            requestBodyJson.put("messages", messagesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestBodyJson,
                response -> {
                    Log.d("Volley", "Response: " + response.toString());
                    try {
                        JSONArray choicesArray = response.getJSONArray("choices");
                        if (choicesArray.length() > 0) {
                            JSONObject choice = choicesArray.getJSONObject(0);
                            JSONObject messageObject = choice.getJSONObject("message"); // Get the message object
                            String textResponse = messageObject.getString("content"); // Extract only the content
                            callback.onResponseReceived(textResponse); // Use callback to return the response
                        } else {
                            callback.onResponseReceived(null); // No choices found, handle as error or empty response
                        }
                    } catch (JSONException e) {
                        Log.e("Volley", "JSON parsing error: ", e);
                        callback.onResponseReceived(null); // Parsing error, handle appropriately
                    }
                },
                error -> {
                    Log.e("Volley", "Error: " + error.toString());
                    callback.onResponseReceived(null); // Network or server error
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "YOUR_API_KEY");
                return headers;
            }
        };

        queue.add(jsonObjectRequest);

    }

}
