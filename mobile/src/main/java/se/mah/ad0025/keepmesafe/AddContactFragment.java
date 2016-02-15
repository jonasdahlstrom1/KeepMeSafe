package se.mah.ad0025.keepmesafe;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


/**
 * A simple {@link Fragment} subclass.
 */
public class AddContactFragment extends Fragment {
    OnImportClickedListener mCallback;
    EditText et_contactName, et_contactNumber;

    public interface OnImportClickedListener {
        void onImportBtnClicked();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = context instanceof Activity ? (Activity) context : null;

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnImportClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnImportClickedListener");
        }

    }

    public AddContactFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_contact, container, false);

        et_contactName = (EditText)view.findViewById(R.id.et_contactName);
        et_contactNumber = (EditText)view.findViewById(R.id.et_contactNumber);
        Button btn_openContacts = (Button)view.findViewById(R.id.btn_openContacts);

        btn_openContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onImportBtnClicked();
            }
        });
        return view;
    }

    public void setNameAndNumber(String name, String number) {
        et_contactName.setText(name);
        et_contactNumber.setText(number);
    }
}