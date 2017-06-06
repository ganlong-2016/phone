package bchd.com.phone.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import bchd.com.phone.R;
import bchd.com.phone.model.PhoneCallLog;
import bchd.com.phone.utils.Mutils;

/**
 * Created by ikomacbookpro on 2017/6/6.
 */

public class CalllogListAdapter extends BaseAdapter {
    private Context mContext;
    private List<PhoneCallLog> mDatas;

    public CalllogListAdapter(Context context, List<PhoneCallLog> list){
        super();
        mContext = context;
        mDatas = list;
    }

    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return mDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView==null){
            viewHolder=new ViewHolder();

            convertView= LayoutInflater.from(mContext).inflate( R.layout.calllog_list_item,null);
            viewHolder.nickName_textview = (TextView)convertView.findViewById(R.id.nickname_textview);
            viewHolder.phoneNumber = (TextView)convertView.findViewById(R.id.phonenumber_textview);
            viewHolder.duration = (TextView)convertView.findViewById(R.id.duration_textview);
            viewHolder.callDate = (TextView)convertView.findViewById(R.id.time_textview);
            convertView.setTag(viewHolder);
        }else{
            viewHolder=(ViewHolder) convertView.getTag();
        }

        PhoneCallLog phoneCallLog = (PhoneCallLog) getItem(position);
        viewHolder.nickName_textview.setText(phoneCallLog.getNickname());
        viewHolder.phoneNumber.setText(phoneCallLog.getPhoneNumber());
        viewHolder.duration.setText(phoneCallLog.getCallduration());
        String dateString = Mutils.getFormatDate(String.valueOf(phoneCallLog.getCallDate().getTime() / 1000),"yyyy-MM-dd hh:mm:ss");
        viewHolder.callDate.setText(dateString);

        return convertView;
    }

    class ViewHolder{
        public TextView nickName_textview;
        public TextView phoneNumber;
        public TextView duration;
        public TextView callDate;
    }
}
