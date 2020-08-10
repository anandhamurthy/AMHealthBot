package com.amhealthbot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.amhealthbot.Adapters.ChatAdapter;
import com.amhealthbot.Login.LoginActivity;
import com.amhealthbot.Models.Chat;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import id.zelory.compressor.Compressor;

import static com.android.volley.Request.Method.GET;

public class MainActivity extends AppCompatActivity {

    private RecyclerView Chat_List;
    private EditText Chat_Message;
    private ImageView Chat_Send, Toolbar_More, Image;
    private DatabaseReference mChatDatabase;
    private ChatAdapter chatAdapter;
    List<Chat> chatList;
    private FirebaseAuth mAuth;
    private TextToSpeech textToSpeech;

    private StorageReference mUserImageStorage;
    private StorageTask mUploadTask;
    private Uri mImageUri;

    private Bitmap compressedImage;

    protected Interpreter tflite;
    private MappedByteBuffer tfliteModel;
    private TensorImage inputImageBuffer;
    private int imageSizeX;
    private int imageSizeY;
    private TensorBuffer outputProbabilityBuffer;
    private TensorProcessor probabilityProcessor;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;
    private Bitmap bitmap;
    private List<String> labels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try{
            tflite=new Interpreter(LoadModelFile(MainActivity.this));
        }catch (Exception e) {
            e.printStackTrace();
        }

        mAuth = FirebaseAuth.getInstance();

        Chat_List = findViewById(R.id.chat_list);
        Chat_Message = findViewById(R.id.chat_message);
        Chat_Send = findViewById(R.id.chat_send);
        Image = findViewById(R.id.chat_add_image);
        Toolbar_More = findViewById(R.id.toolbar_more);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        Chat_List.setLayoutManager(mLayoutManager);
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatList);
        Chat_List.setAdapter(chatAdapter);

        if (mAuth.getCurrentUser()!=null){
            FirebaseUser mCurrentUser = mAuth.getCurrentUser();
            String mCurrentUserId = mCurrentUser.getUid();
            mChatDatabase = FirebaseDatabase.getInstance().getReference().child("chats").child(mCurrentUserId);
            mChatDatabase.keepSynced(true);

            mUserImageStorage = FirebaseStorage.getInstance().getReference("Skin Disease");

            Chat_List.setHasFixedSize(true);


            textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech.setLanguage(Locale.UK);
                    }
                }
            });

            Image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CropImage.activity()
                            .start(MainActivity.this);
                }
            });

            getChatMessages();


            Toolbar_More.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case R.id.delete:
                                    mChatDatabase.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Toast.makeText(MainActivity.this, "Deleted All.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    return true;
                                case R.id.logout:
                                    FirebaseAuth.getInstance().signOut();
                                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                    finish();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    popupMenu.inflate(R.menu.chat_menu);
                    popupMenu.show();
                }
            });

            Chat_Send.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    final String message = Chat_Message.getText().toString();
                    if (!message.isEmpty()) {
                        String key = mChatDatabase.push().getKey();
                        HashMap sendMap = new HashMap<>();
                        sendMap.put("id", 1);
                        sendMap.put("message", message);
                        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
                        sendMap.put("time", time);
                        sendMap.put("type", "text");
                        Chat_Message.setText("");

                        mChatDatabase.child(key).setValue(sendMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isComplete()) {
                                    ReadJson("https://amhealthbot.herokuapp.com/predict/", message);
                                }
                            }
                        });

                    } else {
                        Toast.makeText(MainActivity.this, "Ask Something..", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }

    }

    private MappedByteBuffer LoadModelFile(MainActivity activity)  throws IOException{
        AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("model.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    }

    private TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }
    private TensorOperator getPostprocessNormalizeOp(){
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }

    private void getChatMessages() {
        mChatDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    chatList.add(chat);
                }
                    chatAdapter.notifyDataSetChanged();
                Chat_List.smoothScrollToPosition(chatAdapter.getItemCount());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void ReadJson(String msg, final String ur) {

        final String url=msg+ur;

        HashMap receiveMap = new HashMap<>();
        receiveMap.put("id", 0);
        receiveMap.put("message", "");
        receiveMap.put("type", "loading");
        mChatDatabase.child("loading").setValue(receiveMap);

        StringRequest stringRequest = new StringRequest(GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        String key = mChatDatabase.push().getKey();
                        HashMap receiveMap = new HashMap<>();
                        receiveMap.put("id", 0);
                        receiveMap.put("message", response);
                        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
                        receiveMap.put("time", time);
                        receiveMap.put("type", "chat");
                        mChatDatabase.child(key).setValue(receiveMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                mChatDatabase.child("loading").removeValue();
                            }
                        });
                        if(!textToSpeech.isSpeaking())
                            textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null);



                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        ApplicationController.getInstance().addToRequestQueue(stringRequest);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser mCurrentUser = mAuth.getCurrentUser();

        if (mCurrentUser == null) {

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();

        }
    }

    public void onPause(){
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    private void UploadImage() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading");
        pd.setCanceledOnTouchOutside(false);
        pd.show();
        if (mImageUri != null) {

            final StorageReference fileReference = mUserImageStorage.child(System.currentTimeMillis()
                    + ".jpg");

            mUploadTask = fileReference.putFile(mImageUri);
            mUploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String miUrlOk = downloadUri.toString();

                        String key = mChatDatabase.push().getKey();
                        HashMap sendMap = new HashMap<>();
                        sendMap.put("id", 1);
                        sendMap.put("message", "");
                        sendMap.put("image",miUrlOk);
                        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
                        sendMap.put("time", time);
                        sendMap.put("type", "image");

                        mChatDatabase.child(key).setValue(sendMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isComplete()) {
                                    HashMap receiveMap = new HashMap<>();
                                    receiveMap.put("id", 0);
                                    receiveMap.put("message", "");
                                    receiveMap.put("type", "loading");
                                    mChatDatabase.child("loading").setValue(receiveMap);
                                    pd.dismiss();

                                    getResult();
                                }
                            }
                        });


                    } else {
                        Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            Toast.makeText(MainActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {


            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            mImageUri = result.getUri();

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mImageUri);
                //imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

            UploadImage();

        } else {
            Toast.makeText(this, "Something gone wrong!", Toast.LENGTH_SHORT).show();
        }
    }

    private void getResult() {

        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        inputImageBuffer = new TensorImage(imageDataType);
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
        probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

        inputImageBuffer = loadImage(bitmap);

        tflite.run(inputImageBuffer.getBuffer(),outputProbabilityBuffer.getBuffer().rewind());
        showresult();
    }

    private void showresult(){

        try{
            labels = FileUtil.loadLabels(this,"labels.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Float> labeledProbability =
                new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                        .getMapWithFloatValue();
        float maxValueInMap =(Collections.max(labeledProbability.values()));

        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue()==maxValueInMap) {
                String s="";
                String[] words = entry.getKey().split(";");
                for(int i=0;i<words.length;i++){
                    s+=words[i];
                    if (i!=words.length-1)
                    s+="\n";

                }
                String key = mChatDatabase.push().getKey();
                HashMap receiveMap = new HashMap<>();
                receiveMap.put("id", 0);
                receiveMap.put("message", s);
                String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
                receiveMap.put("time", time);
                receiveMap.put("type", "message");
                receiveMap.put("image", "");
                mChatDatabase.child(key).setValue(receiveMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mChatDatabase.child("loading").removeValue();
                    }
                });

                if(!textToSpeech.isSpeaking())
                    textToSpeech.speak(s, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    private TensorImage loadImage(final Bitmap bitmap) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }
}
