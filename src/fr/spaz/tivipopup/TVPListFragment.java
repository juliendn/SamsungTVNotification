package fr.spaz.tivipopup;

import java.util.ArrayList;

import org.teleal.cling.model.meta.Device;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

public class TVPListFragment extends SherlockListFragment implements OnDeviceListChangeListener
{
	private DeviceAdapter mAdapter;

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		mAdapter = new DeviceAdapter(getActivity(), null);
		setListAdapter(mAdapter);
		setEmptyText(getString(R.string.notification_text_empty));

	}

	public class DeviceAdapter extends BaseAdapter
	{

		private ArrayList<Device<?, ?, ?>> mList;
		private Context mContext;
		private Handler mHandler;
		private Runnable mRunnable;

		public DeviceAdapter(Context context, ArrayList<Device<?, ?, ?>> list)
		{
			mHandler = new Handler();
			mRunnable = new Runnable()
			{

				@Override
				public void run()
				{
					notifyDataSetChanged();
				}
			};
			mContext = context;
			mList = list;
		}

		public void setList(ArrayList<Device<?, ?, ?>> deviceList)
		{
			mList = deviceList;
			notifyDataSetChanged();
		}

		@Override
		public int getCount()
		{
			if (null != mList)
			{
				synchronized (mList)
				{
					return mList.size();
				}
			}
			else
			{
				return 0;
			}
		}

		@Override
		public Object getItem(int position)
		{
			synchronized (mList)
			{
				return mList.get(position);
			}
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		public void uiNotifyDataSetChanged()
		{
			mHandler.post(mRunnable);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (null == view)
			{
				view = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_2, parent, false);
			}

			Device<?, ?, ?> device = null;

			synchronized (mList)
			{
				device = mList.get(position);
			}

			final TextView text1 = (TextView) view.findViewById(android.R.id.text1);
			final TextView text2 = (TextView) view.findViewById(android.R.id.text2);
			text1.setText(device.getDetails().getModelDetails().getModelName());
			text2.setText(device.getDetails().getManufacturerDetails().getManufacturer());
			return view;
		}

	}

	@Override
	public void deviceListChange()
	{
		if(null!=mAdapter)
		{
			mAdapter.uiNotifyDataSetChanged();
		}
	}

	public void setList(ArrayList<Device<?, ?, ?>> deviceList)
	{
		if (null != mAdapter)
		{
			mAdapter.setList(deviceList);
		}
	}
}
