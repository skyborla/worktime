package com.github.skyborla.worktime.ui.work;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.github.skyborla.worktime.R;
import com.github.skyborla.worktime.ui.control.DateControl;
import com.github.skyborla.worktime.ui.control.FormUpdateListener;
import com.github.skyborla.worktime.ui.control.TimeControl;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;

/**
 * Created by Sebastian on 14.09.2014.
 */
public abstract class WorkRecordFormFragment extends DialogFragment implements FormUpdateListener {

    protected static final String ARG_ID = "id";
    protected static final String ARG_DATE = "date";
    protected static final String ARG_START_TIME = "startTime";
    protected static final String ARG_END_TIME = "EndTime";

    protected WorkRecordFragmentInteractionListener mListener;

    protected long id;

    protected DateControl date;
    protected TimeControl startTime;
    protected TimeControl endTime;

    protected EditText datePreview;
    protected EditText startTimePreview;
    protected EditText endTimePreview;

    protected View view;
    protected AlertDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        date = new DateControl(getActivity());
        startTime = new TimeControl(getActivity());
        endTime = new TimeControl(getActivity());

        if (getArguments() != null) {
            id = getArguments().getLong(ARG_ID);

            date.setDate(getArguments().getString(ARG_DATE));
            startTime.setTime(getArguments().getString(ARG_START_TIME));
            endTime.setTime(getArguments().getString(ARG_END_TIME));

        } else {
            date.setDate(LocalDate.now());
            startTime.setTime(LocalTime.now());
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (WorkRecordFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.fragment_work_record_form, null);

        setupForm();
        onFormUpdated();

        dialog = new AlertDialog.Builder(getActivity())
                .setTitle(getTitle())
                .setView(view)
                .setPositiveButton(R.string.dialog_generic_submit, null)
                .setNegativeButton(R.string.dialog_generic_abort, null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ((AlertDialog) dialog)
                        .getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(getOnSubmitListener());

                ((AlertDialog) dialog)
                        .getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setOnClickListener(getOnAbortListener());

                if (!isDialogCancelable()) {
                    ((AlertDialog) dialog).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            System.out.println("try cancel");
                        }
                    });

                    ((AlertDialog) dialog).setCanceledOnTouchOutside(false);
                    ((AlertDialog) dialog).setCancelable(false);
                }
            }
        });


        return dialog;
    }

    private void setupForm() {
        date.setup(view, R.id.form_date_preview, R.id.form_start_date_today, this);

        startTime.setup(view, R.id.form_start_time_preview, R.id.form_start_time_now, this);
        endTime.setup(view, R.id.form_end_time_preview, R.id.form_end_time_now, this);

    }

    protected boolean validate() {
        if (date.getDate() == null || startTime.getTime() == null || endTime.getTime() == null) {
            Toast.makeText(getActivity(), R.string.record_missing_fields_message, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (endTime.getTime().isBefore(startTime.getTime())) {
            Toast.makeText(getActivity(), R.string.record_end_before_start, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    protected abstract int getTitle();

    protected abstract boolean isDialogCancelable();

    protected abstract View.OnClickListener getOnSubmitListener();

    protected abstract View.OnClickListener getOnAbortListener();

    @Override
    public void onFormUpdated() {

    }

    public interface WorkRecordFragmentInteractionListener {
        void createNewRecord(LocalDate date, LocalTime startTime, LocalTime endTime);

        void updateRecord(long id, LocalDate date, LocalTime startTime, LocalTime endTime);
    }
}
