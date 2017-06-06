package bchd.com.phone.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import java.util.List;

import bchd.com.phone.Adapter.CalllogListAdapter;
import bchd.com.phone.R;
import bchd.com.phone.model.PhoneCallLog;

public class CalllogActivity extends AppCompatActivity {
    private List<PhoneCallLog> mDatas;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calllog);

        mDatas = (List<PhoneCallLog>) getIntent().getSerializableExtra("mDatas");

        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setAdapter(new CalllogListAdapter(this,mDatas));
    }
}
