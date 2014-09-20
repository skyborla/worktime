package com.github.skyborla.worktime;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.cocosw.undobar.UndoBarController;
import com.github.skyborla.worktime.model.DataSource;
import com.github.skyborla.worktime.model.LeaveRecord;
import com.github.skyborla.worktime.model.MetaLeaveRecord;
import com.github.skyborla.worktime.model.WorkRecord;
import com.github.skyborla.worktime.ui.leave.NewLeaveRecordFragment;
import com.github.skyborla.worktime.ui.list.RecordsFragment;
import com.github.skyborla.worktime.ui.work.EditWorkRecordFragment;
import com.github.skyborla.worktime.ui.work.NewWorkRecordFragment;

import org.threeten.bp.LocalDate;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Worktime extends FragmentActivity implements RecordsFragment.RecordsFragmentInteractionListener, ModelInteraction {


    public static final String PENDING_RECORD = "PENDING_RECORD";
    public static final String PENDING_DATE = "PENDING_DATE";
    public static final String PENDING_START_TIME = "PENDING_START_TIME";
    public static final String PENDING_END_TIME = "PENDING_END_TIME";

    private DataSource dataSource;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    private List<LocalDate> months;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worktime);

        NewLeaveRecordFragment.newInstance().show(getSupportFragmentManager(), "newLeaveRecord");

        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);

        if (pref.getBoolean(PENDING_RECORD, false)) {
            NewWorkRecordFragment.newInstance(
                    pref.getString(PENDING_DATE, ""),
                    pref.getString(PENDING_START_TIME, ""),
                    pref.getString(PENDING_END_TIME, "")).show(getSupportFragmentManager(), "newWorkRecord");
        }


        dataSource = new DataSource(this);
        try {
            dataSource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        months = dataSource.getMonths();


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        LocalDate now = LocalDate.now().withDayOfMonth(1);
        if (months.contains(now)) {
            mViewPager.setCurrentItem(months.indexOf(now));
        } else if (months.size() > 0) {
            mViewPager.setCurrentItem(months.size() - 1);
        }
    }

    @Override
    protected void onResume() {
        try {
            dataSource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        dataSource.close();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.global, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (item.getItemId()) {
            case R.id.action_new_work_record:
                NewWorkRecordFragment.newInstance().show(getSupportFragmentManager(), "newWorkRecord");
                return true;

            case R.id.action_new_leave_record:
                NewLeaveRecordFragment.newInstance().show(getSupportFragmentManager(), "newLeaveRecord");
                return true;

            case R.id.action_send_email:
                try {
                    File temp = File.createTempFile("asd", "bsd", getCacheDir());

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/octet-stream");
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(temp));
                    startActivity(Intent.createChooser(intent, "Email senden"));

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beginEditWorkRecord(WorkRecord workRecord) {
        EditWorkRecordFragment
                .newInstance(workRecord.getId(),
                        workRecord.getDate().toString(),
                        workRecord.getStartTime().toString(),
                        workRecord.getEndTime().toString())
                .show(getSupportFragmentManager(), "editRecord");
    }

    @Override
    public void beginDeleteWorkRecord(final WorkRecord workRecord) {

        String message = "\n \u2022 " + FormatUtil.DATE_FORMAT_MEDIUM.format(workRecord.getDate());
        message += " (" + FormatUtil.formatTimes(workRecord) + ")\n";

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_confirm_delete_header)
                .setMessage(message)
                .setNegativeButton(R.string.dialog_generic_abort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton(R.string.dialog_delete_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        modelChanged(dataSource.deleteWorkRecord(workRecord));

                        new UndoBarController.UndoBar(Worktime.this)
                                .message(R.string.undo_delete)
                                .listener(new UndoBarController.UndoListener() {
                                    @Override
                                    public void onUndo(Parcelable parcelable) {
                                        modelChanged(dataSource.persistWorkRecord(workRecord));
                                    }
                                })
                                .duration(10000)
                                .show(true);
                    }
                })
                .create().show();
    }

    @Override
    public void beginEditLeaveRecord(LeaveRecord leaveRecord) {

        MetaLeaveRecord toEdit = dataSource.getMetaLeaveRecord(leaveRecord);
        System.out.println(toEdit);


    }

    @Override
    public void beginDeleteLeaveRecord(final LeaveRecord leaveRecord) {
        final MetaLeaveRecord metaLeaveRecord = getDatasource().getMetaLeaveRecord(leaveRecord);

        String message = "\n \u2022 von: " + FormatUtil.DATE_FORMAT_MEDIUM.format(metaLeaveRecord.getStartDate());
        message += "\n \u2022 bis: " + FormatUtil.DATE_FORMAT_MEDIUM.format(metaLeaveRecord.getEndDate());
        message += "\n \u2022 Grund: " + getString(metaLeaveRecord.getReason().stringResource);
        message += "\n";

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_confirm_delete_header)
                .setMessage(message)
                .setNegativeButton(R.string.dialog_generic_abort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton(R.string.dialog_delete_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Set<LocalDate> affectedMonth = dataSource.deleteLeaveRecord(leaveRecord);
                        modelChanged(affectedMonth);

                        new UndoBarController.UndoBar(Worktime.this)
                                .message(R.string.undo_delete)
                                .listener(new UndoBarController.UndoListener() {
                                    @Override
                                    public void onUndo(Parcelable parcelable) {
                                        Set<LocalDate> affectedMonth = dataSource.persistLeaveRecord(metaLeaveRecord);
                                        modelChanged(affectedMonth);
                                    }
                                })
                                .duration(10000)
                                .show(true);
                    }
                })
                .create().show();
    }

    @Override
    public DataSource getDatasource() {
        return dataSource;
    }

    @Override
    public void modelChanged(Set<LocalDate> changed) {

        LocalDate currentMonth = null;
        try {
            currentMonth = months.get(mViewPager.getCurrentItem());
        } catch (Throwable t) {
        }

        months = dataSource.getMonths();
        mSectionsPagerAdapter.notifyDataSetChanged();

        // propagate update to pages
        for (LocalDate date : changed) {
            String tag = "android:switcher:" + R.id.pager + ":" + date.hashCode();
            RecordsFragment fragment = (RecordsFragment) getFragmentManager().findFragmentByTag(tag);

            if (fragment != null) {
                fragment.onRecordsUpdated();
            }
        }

        // determine if we need to change the page
        Set<LocalDate> displayCandidates = new HashSet<LocalDate>();
        displayCandidates.addAll(changed);
        displayCandidates.retainAll(months);

        LocalDate firstDisplayCandidate = displayCandidates.iterator().next();

        // current month changed -> do nothing
        if (displayCandidates.contains(currentMonth)) {
            return;
        }

        // try to go to a visible changed page
        else if (firstDisplayCandidate != null) {
            mViewPager.setCurrentItem(months.indexOf(firstDisplayCandidate));
        }

        // ensure valid page (unrelated to change)
        else {
            LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);

            if (currentMonth != null && months.contains(currentMonth)) {
                return;
            } else if (months.contains(thisMonth)) {
                mViewPager.setCurrentItem(months.indexOf(thisMonth));
            } else {
                mViewPager.setCurrentItem(0);
            }
        }
    }

    @Override
    public void modelChanged(LocalDate date) {
        Set<LocalDate> affectedMonth = new HashSet<LocalDate>();
        affectedMonth.add(date);
        modelChanged(affectedMonth);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return RecordsFragment.newInstance(months.get(position));
        }

        @Override
        public int getCount() {
            return months.size();
        }

        @Override
        public long getItemId(int position) {
            LocalDate month = months.get(position);
            return Long.valueOf(FormatUtil.DATE_FORMAT_DB_MONTH.format(month));
        }

        @Override
        public int getItemPosition(Object object) {
            return months.indexOf(((RecordsFragment) object).getMonth());
        }

        @Override
        public CharSequence getPageTitle(int position) {
            LocalDate date = months.get(position);
            return FormatUtil.DATE_FORMAT_MONTH.format(date).toUpperCase();
        }
    }
}
