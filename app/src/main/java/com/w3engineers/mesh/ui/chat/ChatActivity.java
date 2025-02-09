package com.w3engineers.mesh.ui.chat;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import com.w3engineers.ext.viper.R;
import com.w3engineers.ext.viper.databinding.ActivityChatBinding;
import com.w3engineers.mesh.application.ui.util.ToastUtil;
import com.w3engineers.mesh.model.MessageModel;
import com.w3engineers.mesh.model.UserModel;
import com.w3engineers.mesh.ui.Nearby.NearbyCallBack;
import com.w3engineers.mesh.util.ConnectionManager;
import com.w3engineers.mesh.util.Constants;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.TimeUtil;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


/**
 * * ============================================================================
 * * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * * Unauthorized copying of this file, via any medium is strictly prohibited
 * * Proprietary and confidential
 * * ----------------------------------------------------------------------------
 * * Created by: Sikder Faysal Ahmed on [15-Jan-2019 at 1:01 PM].
 * * Email: sikderfaysal@w3engineers.com
 * * ----------------------------------------------------------------------------
 * * Project: meshrnd.
 * * Code Responsibility: <Purpose of code>
 * * ----------------------------------------------------------------------------
 * * Edited by :
 * * --> <First Editor> on [15-Jan-2019 at 1:01 PM].
 * * --> <Second Editor> on [15-Jan-2019 at 1:01 PM].
 * * ----------------------------------------------------------------------------
 * * Reviewed by :
 * * --> <First Reviewer> on [15-Jan-2019 at 1:01 PM].
 * * --> <Second Reviewer> on [15-Jan-2019 at 1:01 PM].
 * * ============================================================================
 **/
public class ChatActivity extends AppCompatActivity implements View.OnClickListener, MessageListener, NearbyCallBack, DbUpdate {

    private ActivityChatBinding mBinding;
    private ChatAdapter mChatAdapter;
    private UserModel mUserModel;
    private MenuItem status, repeatedMsg;
    private Timer mTimer;
    private boolean isRepeatModeOn;
    private final int REQUEST_FILE_PICK = 202;
    public static int FILE_MESSAGE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGuiWithUserdata();
        initAdapter();
        setUserInfo();
        ConnectionManager.on(this).initMessageListener(this);
        ConnectionManager.on(this).initNearByCallBackForChatActivity(this);
        ChatDataProvider.On().setUpdateListener(this::updateUI);
        fetchAllConversationWithThisUser();

        ConnectionManager.on(this).checkConnectionStatus(mUserModel.getUserId());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_active_status, menu);
        status = menu.findItem(R.id.menu_active_state);
        menu.findItem(R.id.menu_send_large_message).setEnabled(true);
        repeatedMsg = menu.findItem(R.id.menu_send_continuous_msg).setEnabled(true);
        status.setEnabled(true);
        List<UserModel> list = ConnectionManager.on(this).getUserList();
        Collections.sort(list);
        for (UserModel userModel : list) {
            if (mUserModel.getUserId().equalsIgnoreCase(userModel.getUserId())) {
                String connectionType = ConnectionManager.on(this).getConnectionType(mUserModel.getUserId());
                //  status.setTitle(getString(R.string.status_online));
                status.setTitle(connectionType);
            }
        }
        return true;
    }

    private void initGuiWithUserdata() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_chat);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mUserModel = (UserModel) getIntent().getSerializableExtra(UserModel.class.getName());
        mBinding.imageButtonSend.setOnClickListener(this);
        mBinding.imageButtonCamera.setOnClickListener(this);
    }

    private void initAdapter() {
        mChatAdapter = new ChatAdapter(this);
        mBinding.recyclerViewMessage.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerViewMessage.setAdapter(mChatAdapter);
    }

    private void fetchAllConversationWithThisUser() {
        //resendFailedMessage(mUserModel.getUserId());
        mChatAdapter.addItem(ChatDataProvider.On().getAllConversation(mUserModel.getUserId()));
        scrollSmoothly();
    }

/*    private void resendFailedMessage(String userId) {
        List<MessageModel> SendingFailedMessage = ChatDataProvider.On().getSendFailedConversation(userId);
        if (SendingFailedMessage.size() > 0) {
            for (MessageModel message : SendingFailedMessage) {
                ConnectionManager.on().sendMessage(mUserModel.getUserId(), message);
            }
        }
    }*/

    private void setUserInfo() {
        getSupportActionBar().setTitle(mUserModel.getUserName());
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.drawable_reg_page_shape));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILE_PICK) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();

                String path;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    path = getImageRealPath(getContentResolver(), uri, null);
                } else {
                    path = uri.getPath();
                }


                MessageModel messageModel = new MessageModel();
                messageModel.message = path;
                messageModel.incoming = false;
                messageModel.friendsId = mUserModel.getUserId();
                messageModel.messageType = FILE_MESSAGE;


                String fileData = ConnectionManager.on(this).sendFileMessage(mUserModel.getUserId(), path);

                try {
                    JSONObject jsonObject = new JSONObject(fileData);
                    boolean success = jsonObject.getBoolean("success");
                    String msg = jsonObject.getString("msg");
                    if (success) {
                        messageModel.messageId = msg;

                        Log.d("FileMessageTest", "File message id: " + msg);

                        ChatDataProvider.On().insertMessage(messageModel, mUserModel);
                        mChatAdapter.addItem(messageModel);
                        scrollSmoothly();
                    } else {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConnectionManager.on(this).initMessageListener(null);
        ConnectionManager.on(this).initNearByCallBackForChatActivity(null);
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_button_camera:
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, REQUEST_FILE_PICK);
                break;
            case R.id.image_button_send:
                String inputValue = mBinding.edittextMessageInput.getText().toString().trim();
                if (TextUtils.isEmpty(inputValue)) return;
                mBinding.edittextMessageInput.setText("");
                MessageModel messageModel = new MessageModel();
                messageModel.message = inputValue + "\n" + TimeUtil.parseMillisToTime(System.currentTimeMillis());
                messageModel.incoming = false;
                messageModel.friendsId = mUserModel.getUserId();
                messageModel.messageId = UUID.randomUUID().toString();

                ChatDataProvider.On().insertMessage(messageModel, mUserModel);
                mChatAdapter.addItem(messageModel);
                scrollSmoothly();

                ConnectionManager.on(this).sendMessage(mUserModel.getUserId(), messageModel);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //   ConnectionManager.on().initListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_send_large_message:
                sendMessage(Constants.LARGE_MESSAGE);
                break;
            case R.id.menu_send_continuous_msg:
                if (isRepeatModeOn) {
                    isRepeatModeOn = false;
                    if (mTimer != null) {
                        mTimer.cancel();
                    }
                    repeatedMsg.setIcon(R.drawable.ic_action_playback_repeat);
                } else {
                    isRepeatModeOn = true;
                    Toast.makeText(this, "Repeated message started", Toast.LENGTH_SHORT).show();
                    repeatedMsg.setIcon(R.drawable.ic_action_cancel);
                    repeatMessage();
                }

                break;
            case R.id.menu_delete_all_messages:
                long removedItems = ChatDataProvider.On().removeAllConversation(mUserModel.getUserId());
                Toast.makeText(this, "Deleted " + removedItems + " entries", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void repeatMessage() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
//                sendMessage("Hello Bro");
                sendMessage("Promon unearthed the security hole while investigating apps that been found stealing money from bank accounts. In all it found that 60 financial institutions had been targeted with various apps that exploited the vulnerability.\n" +
                        "\n" +
                        "Chief technology officer at Promon, Tom Hansen told the BBC: \"We'd never seen this behavior before. As the operating system gets more complex it's hard to keep track of all its interactions. This looks like the kind of thing that gets lost in that complexity\".\n" +
                        "\n" +
                        "Worryingly, it was found that most of the top 500 apps in Google Play were vulnerable to being exploited. Lookout, another security firm working in conjunction with Promon, identified no fewer than 36 malicious apps already actively exploiting the vulnerability. This included variants of the BankBot banking trojan which has been around since as long ago as 2017.\n" +
                        "\n" +
                        "Promon published a video about the vulnerability:");
            }
        }, 3 * 1000, 5 * 1000);
    }

    private void sendMessage(String message) {
        MessageModel messageModel = new MessageModel();
        messageModel.message = message + "\n" + TimeUtil.parseMillisToTime(System.currentTimeMillis());
        messageModel.incoming = false;
        messageModel.friendsId = mUserModel.getUserId();
        messageModel.messageId = UUID.randomUUID().toString();

        ChatDataProvider.On().insertMessage(messageModel, mUserModel);
        runOnUiThread(() -> {
            mChatAdapter.addItem(messageModel);
            scrollSmoothly();
        });
        ConnectionManager.on(this).sendMessage(mUserModel.getUserId(), messageModel);
    }

    private void scrollSmoothly() {
        int index = mChatAdapter.getItemCount() - 1;
        if (index > 0) {
            mBinding.recyclerViewMessage.smoothScrollToPosition(index);
        }
    }

    @Override
    public void onMessageReceived(MessageModel message) {
        if (message.friendsId.equalsIgnoreCase(mUserModel.getUserId())) {
            HandlerUtil.postForeground(() -> {
                mChatAdapter.addItem(message);
                scrollSmoothly();
            });
        }
    }

    @Override
    public void onMessageDelivered() {
        updateUI();
    }

    @Override
    public void onFileProgressReceived(String fileMessageId, int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChatAdapter.updateProgress(fileMessageId, progress);
            }
        });
    }

    @Override
    public void onFileTransferEvent(String fileMessageId, boolean isSuccess, String errorMessage) {
        runOnUiThread(() -> {
            if (isSuccess) {
                //ToastUtil.showShort(this, "File message received");
                mChatAdapter.updateProgress(fileMessageId, 100);
                mChatAdapter.notifyDataSetChanged();
            } else {
                mChatAdapter.setFileError(fileMessageId);
                mChatAdapter.notifyDataSetChanged();

                if (TextUtils.isEmpty(errorMessage)) {
                    ToastUtil.showShort(this, "Message sending failed");
                } else {
                    ToastUtil.showShort(this, errorMessage);
                }

            }
        });
    }

    @Override
    public void onUserFound(UserModel model) {
        if (model.getUserId().equalsIgnoreCase(mUserModel.getUserId())) {
            // runOnUiThread(() -> mBinding.edittextMessageInput.setEnabled(true));
            runOnUiThread(() -> {
                if (status != null) {
                  /*  String connectionType = ConnectionManager.on().getConnectionType(model.getUserId());
                    status.setTitle(connectionType);*/
                }
            });

            // resendFailedMessage(model.getUserId());
        }
    }

    @Override
    public void onDisconnectUser(String userId) {
        if (userId == null || userId.isEmpty()) return;
        if (userId.equalsIgnoreCase(mUserModel.getUserId())) {
            runOnUiThread(() -> {
/*                mBinding.edittextMessageInput.setEnabled(false);
                mBinding.edittextMessageInput.setHint("Currently," + " "+ mUserModel.getUserName() + " " + "is not available ");
                mBinding.edittextMessageInput.setHintTextColor(getResources().getColor(R.color.colorAccent));*/

           /*     if (status != null) {
                    status.setTitle(getString(R.string.status_offline));
                }*/
            });
        }
    }


    @Override
    public void updateUI() {
        if (mChatAdapter != null) {
            runOnUiThread(() -> {
                mChatAdapter.clear();
                List<MessageModel> messageModelList = ChatDataProvider.On().getAllConversation(mUserModel.getUserId());
                mChatAdapter.addItem(messageModelList);
            });
        }
    }

    private String getImageRealPath(ContentResolver contentResolver, Uri uri, String whereClause) {
        String ret = "";

        // Query the uri with condition.
        Cursor cursor = contentResolver.query(uri, null, whereClause, null, null);

        if (cursor != null) {
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst) {

                // Get columns name by uri type.
                String columnName = MediaStore.Images.Media.DATA;
                Log.d("UriTest: ", "uri: " + uri);
                if (uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Images.Media.DATA;
                }


                // Get column index.
                int imageColumnIndex = cursor.getColumnIndex(columnName);

                // Get column value which is the uri related file local path.
                ret = cursor.getString(imageColumnIndex);
            }
        }

        return ret;
    }
}
