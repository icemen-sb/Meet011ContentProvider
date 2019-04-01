package ru.relastic.meet011contentprovider;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DetailsActivity extends AppCompatActivity {
    //DETAILS / EDIT NOTES
    public static final int REQUEST_CODE_DETAILS = 2000;
    public static final int MESSAGE_WHAT_KEY = 2;
    private Button mButtonCancel, mButtonDelete, mButtonEdit, mButtonCommit;
    private TextView mEditText;
    private int current_id = 0;
    private Bundle data;
    MyFileManager.FileListeners mListeners;
    private static final String KEY_EDIT_MODE = "key_edit_mode";
    private boolean edit_mode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        initViews();
        initListeners();
        init();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        edit_mode = savedInstanceState.getBoolean(KEY_EDIT_MODE);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_EDIT_MODE,edit_mode);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void initViews() {
        mButtonCancel = findViewById(R.id.btn_delails_cancel);
        mButtonDelete = findViewById(R.id.btn_delails_delete);
        mButtonEdit = findViewById(R.id.btn_delails_edit);
        mButtonCommit = findViewById(R.id.btn_delails_commit);
        //mEditText ....
        assignEditMode();
    }

    private void assignEditMode(){
        if (!edit_mode) {
            mEditText = findViewById(R.id.textViewDetails);
        }else {
            String text = mEditText.getText().toString();
            mEditText = findViewById(R.id.editTextDetails);
            mEditText.setText(text);
        }
        findViewById(R.id.textViewDetails).setVisibility(edit_mode ? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.editTextDetails).setVisibility(!edit_mode ? View.INVISIBLE : View.VISIBLE);
        mButtonEdit.setEnabled(!edit_mode);
        mButtonCommit.setEnabled(edit_mode);
        PrefActivity.MySharedPreferences pref = new PrefActivity.MySharedPreferences(this);
        pref.applyPrefByView(mEditText);
    }

    private void initListeners() {
        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mButtonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialod = new AlertDialog.Builder(DetailsActivity.this)
                        .setTitle("Удаление данных")
                        .setMessage("Вы действительно хотите удалить текущую запись?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                deleteData();
                                finish();
                            }
                        }).create();
                dialod.show();
            }
        });
        mButtonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Обновляем БД, файл
                edit_mode = true;
                assignEditMode();
                //finish();
            }
        });

        mButtonCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Обновляем БД, файл
                AlertDialog dialod = new AlertDialog.Builder(DetailsActivity.this)
                        .setTitle("Сохранение данных")
                        .setMessage("Вы действительно хотите сохранить изменения?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                updateData();
                                finish();
                            }
                        }).create();
                dialod.show();
            }
        });

        mListeners = new MyFileManager.FileListeners(){
            @Override
            public void readed(String text) {
                Handler h = new Handler(getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == MESSAGE_WHAT_KEY) {
                            DetailsActivity.this.mEditText.setText(
                                    msg.getData().getString(MainActivity.MESSAGE_KEY_STRING_EXTRA));
                        }

                    }
                };
                Bundle bundle = new Bundle();
                bundle.putString(MainActivity.MESSAGE_KEY_STRING_EXTRA,text);

                Message message = Message.obtain(h,MESSAGE_WHAT_KEY);
                message.setData(bundle);
                message.sendToTarget();
            }

            @Override
            public void writed(boolean completed) {
                if (completed) {
                    System.out.println("------ "+"file writed.");
                }else {
                    System.out.println("------ "+"error file writed.");
                }
            }

            @Override
            public void deleted(boolean completed) {
                if (completed) {
                    System.out.println("------ "+"file deleted.");
                }else {
                    System.out.println("------ "+"error file delete.");
                }
            }
        };
    }

    private void init() {
        Intent intent = getIntent();
        current_id = intent.getIntExtra(DBManager.FIELD_ID,0);
        if (current_id<1) {
            Log.v("ERROR ID","ERROR IN INPUT ID, EXPECT ID > 0");
            finish();
        }
        DBManager dbm = DBManager.getInstance(this);
        data = dbm.getDataById(current_id);
        mEditText.setText(MyFileManager.LOADING_STATE_STAB);
        getFileStorage();
    }

    private void getFileStorage() {
        MyFileManager fileManager = MyFileManager.getInstance(this,mListeners,
                Integer.toString(current_id), MyFileManager.RUN_TYPE_READ,null);
        fileManager.startWorkedThread();
    }
    private void setFileStorage(String text, int id) {
        MyFileManager fileManager =MyFileManager.getInstance(this,mListeners,
                Integer.toString(id), MyFileManager.RUN_TYPE_WRITE,text);
        fileManager.startWorkedThread();
    }
    private void deleteFileStorage () {
        MyFileManager fileManager =MyFileManager.getInstance(this,mListeners,
                Integer.toString(current_id), MyFileManager.RUN_TYPE_DELETE,null);
        fileManager.startWorkedThread();
    }

    private void updateData() {
        Bundle bundle = new Bundle();
        bundle.putInt(DBManager.FIELD_POS,0);
        bundle.putInt(DBManager.FIELD_ID,current_id);
        String note = mEditText.getText().toString();
        bundle.putString(DBManager.FIELD_NOTE,note);

        DBManager dbm = DBManager.getInstance(DetailsActivity.this);
        int id = dbm.updateData(bundle);

        setFileStorage(mEditText.getText().toString(), id);

        Intent intent = new Intent();
        intent.putExtra(MainActivity.RESULT_VALUE,true);
        intent.putExtra(DBManager.FIELD_ID,current_id);
        setResult(DetailsActivity.REQUEST_CODE_DETAILS,intent);
    }
    private void deleteData() {
        Bundle bundle = new Bundle();
        bundle.putInt(DBManager.FIELD_POS,0);
        bundle.putInt(DBManager.FIELD_ID,current_id);
        String note = mEditText.getText().toString();
        bundle.putString(DBManager.FIELD_NOTE,"");

        DBManager dbm = DBManager.getInstance(DetailsActivity.this);
        dbm.deleteData(bundle);

        deleteFileStorage();

        Intent intent = new Intent();
        intent.putExtra(MainActivity.RESULT_VALUE,true);
        intent.putExtra(DBManager.FIELD_ID,current_id);
        setResult(DetailsActivity.REQUEST_CODE_DETAILS,intent);
    }

    public static Intent getIntent(Context context) {
        return new Intent(context, DetailsActivity.class);
    }
}
