package nu.firetech.android.pactrack.frontend;

import nu.firetech.android.pactrack.R;
import nu.firetech.android.pactrack.backend.ParcelDbAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

public class ParcelOptionsMenu {
	private static final int AUTO_ID = Menu.FIRST;

	private long mRowId;
	private int mPosition;
	private ParcelDbAdapter mDbAdapter;
	private UpdateableView mViewToUpdate;
	
	public ParcelOptionsMenu(Menu parentMenu, boolean inSubMenu, long rowId, int position, ParcelDbAdapter dbAdapter, UpdateableView viewToUpdate) {
		mRowId = rowId;
		mPosition = position;
		mDbAdapter = dbAdapter;
		mViewToUpdate = viewToUpdate;

		Menu menu;
		if (inSubMenu) {
			menu = parentMenu.addSubMenu(R.string.menu_parcel_options).setIcon(android.R.drawable.ic_menu_manage);
		} else {
			menu = parentMenu;
		}
		Listener l = new Listener();
		menu.add(0, AUTO_ID, 0, R.string.menu_auto_include).setCheckable(true).setChecked(dbAdapter.getAutoUpdate(rowId)).setOnMenuItemClickListener(l);
	}
	
	public class Listener implements OnMenuItemClickListener {
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			switch(item.getItemId()) {
			case AUTO_ID:
				boolean autoUpdate = !item.isChecked();
				mDbAdapter.setAutoUpdate(mRowId, autoUpdate);
				item.setChecked(autoUpdate);
				if (mViewToUpdate != null) {
					mViewToUpdate.updateAutoUpdateView(mPosition, autoUpdate);
				}
				return true;
			}
			return false;
		}
	}
	
	public interface UpdateableView {
		public void updateAutoUpdateView(int position, boolean value);
	}
}
