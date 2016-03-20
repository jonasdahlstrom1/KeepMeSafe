package se.mah.ad0025.keepmesafe;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.ArrayList;

import se.mah.ad0025.keepmesafe.help.HelpActivity;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AddContactFragment.OnImportClickedListener, AddContactFragment.OnAddContactClickedListener,
        ManageContactsFragment.OnManageAddContactClickedListener, ManageContactsFragment.OnManageListItemClickedListener, ContactDetailsFragment.OnDeleteContactClickedListener,
        ContactDetailsFragment.OnUpdateContactClickedListener, EditMessageFragment.OnSaveMessageClickedListener {

    private static final int PICK_CONTACT = 123;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 64;
    private NavigationView navigationView;
    private FragmentManager fm;
    private SharedPreferences prefs;
    private ArrayList<Contact> contacts = new ArrayList<>();    //Används för att lagra alla kontakter man har sparat i appen.
    private DBController dbController;
    private MainFragment mainFragment;
    private AddContactFragment addContactFragment;
    private ManageContactsFragment manageContactsFragment;
    private ContactDetailsFragment contactDetailsFragment;
    private EditMessageFragment editMessageFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("KeepMeSafePrefs", MODE_PRIVATE);

        dbController = new DBController(this);
        mainFragment = new MainFragment();
        manageContactsFragment = new ManageContactsFragment();
        contactDetailsFragment = new ContactDetailsFragment();
        editMessageFragment = new EditMessageFragment();


        //---------- DETTA KAN VI NOG ÄNDRA OM EN DEL OM VI TAR BORT LANDSKAPSLÄGE -----------------

        if (findViewById(R.id.container) != null) {
//Här läggs det som alltid ska ske.

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();
            navigationView = (NavigationView) findViewById(R.id.nav_view); //Drawer-menyn. Används bl.a. för att avmarkera i menyn vid bakåtklick.
            navigationView.setNavigationItemSelectedListener(this);
            if (savedInstanceState != null) {
//Här läggs det som bara ska ske vid rotation men inte första gången. Tex hämta värden via savedInstanceState.
                addContactFragment = (AddContactFragment) fm.findFragmentByTag("contacts");
                manageContactsFragment = (ManageContactsFragment)fm.findFragmentByTag("manage");
                return;
            }
//Här läggs det som ska ske första gången men inte efter rotation.
            fm = getSupportFragmentManager();
            fm.beginTransaction().replace(R.id.container, mainFragment).commit();
        }

        //------------------------------------------------------------------------------------------


        manageContactsFragment.setAdapter(new ContactListAdapter(this, contacts));
        getAllContactsFromDB();
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = fm.findFragmentById(R.id.container);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else if (!(currentFragment instanceof MainFragment)) {
            fm.beginTransaction().replace(R.id.container, mainFragment).commit();
            //Följande rader avmarkerar samtliga meny-items vid bakåtklick.
            navigationView.getMenu().getItem(0).setChecked(false);
            navigationView.getMenu().getItem(1).setChecked(false);
            navigationView.getMenu().getItem(2).setChecked(false);
            navigationView.getMenu().getItem(3).setChecked(false);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        clearBackStack();

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_Manage) {
            if(manageContactsFragment == null)
                manageContactsFragment = new ManageContactsFragment();
            fm.beginTransaction().replace(R.id.container, manageContactsFragment, "manage").commit();
        } else if (id == R.id.nav_Edit) {
            if(editMessageFragment == null)
                editMessageFragment = new EditMessageFragment();
            fm.beginTransaction().replace(R.id.container, editMessageFragment).commit();
            editMessageFragment.setMessage(prefs.getString("textMessage", ""));
        } else if (id == R.id.nav_What) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_How) {

        }

        item.setChecked(true);  //Markerar vald item i drawern.
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Metod som tömmer backstacken. Sker när användaren klickar på något i drawern.
     */
    private void clearBackStack() {
        if (fm.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry first = fm.getBackStackEntryAt(0);
            fm.popBackStack(first.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    openContacts();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void onImportBtnClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == -1) {
// Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CONTACTS)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.


                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("You need this permission to pick a contact and automatically import its name and number. Are you sure you don't want it?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();

                } else {

                    // No explanation needed, we can request the permission.
                   requestPermission();

                }
            } else {
               openContacts();
            }
        } else {
            openContacts();
        }
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CONTACTS},
                MY_PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    private void openContacts(){
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, PICK_CONTACT);
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    requestPermission();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == PICK_CONTACT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                // Get the URI that points to the selected contact
                Uri uri = data.getData();

                String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER};

                Cursor people = getContentResolver().query(uri, projection, null, null, null);

                int indexName = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int indexNumber = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                people.moveToFirst();

                String name = people.getString(indexName);
                String number = people.getString(indexNumber);
                number = number.replace("-", "");
                addContactFragment.setNameAndNumber(name, number);

                people.close();
            }
        }
    }

    /**
     * Metod som körs när användaren klickar på knappen som tar en till sidan där man lägger till
     * en ny kontakt.
     */
    @Override
    public void onManageAddContactBtnClicked() {
        if(addContactFragment == null)
            addContactFragment = new AddContactFragment();
        fm.beginTransaction().replace(R.id.container, addContactFragment, "contacts").addToBackStack(null).commit();
        addContactFragment.setNameAndNumber("", "");
    }

    /**
     * Metod som körs när användaren klickar på knappen som lägger till ny kontakt.
     * Lägger till kontakten i databasen och uppdaterar ArrayListan med hjälp av metoden
     * "getAllContactsFromDB".
     * @param name
     *          Namnet på kontakten.
     * @param number
     *          Numret till kontakten.
     */
    public void onAddContactBtnClicked(String name, String number) {
        for(int i = 0; i < contacts.size(); i++) {
            if(contacts.get(i).getNumber().equals(number)) {
                Snackbar.make(findViewById(R.id.container), "Contact already added", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                return;
            }
        }

        dbController.open();
        dbController.addContact(name, number);
        dbController.close();
        getAllContactsFromDB();
        fm.popBackStack();
        Snackbar.make(findViewById(R.id.container), "Contact added successfully", Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    /**
     * Hämtar alla kontakter från databasen och lagrar i ArrayListan "contacts".
     * Används vid programstart och när användaren lagt till en ny kontakt.
     */
    private void getAllContactsFromDB() {
        Contact newContact;
        contacts.clear();
        dbController.open();
        Cursor c = dbController.getContacts();
        if( c.moveToFirst() ){
            do{
                newContact = new Contact(c.getString(1), c.getString(2));
                newContact.setID(c.getInt(0));
                contacts.add(newContact);
            }while(c.moveToNext());
        }
        c.close();
        dbController.close();
    }

    @Override
    public void onManageListItemClicked(int position) {
        fm.beginTransaction().replace(R.id.container, contactDetailsFragment).addToBackStack(null).commit();
        contactDetailsFragment.setNameAndNumber(contacts.get(position).getName(), contacts.get(position).getNumber(), contacts.get(position).getID());
    }

    /**
     * Metod som raderar en kontakt från databasen och uppdaterar kontaktlistan.
     * @param ID
     *          Unikt ID till den kontakt som ska raderas från databasen.
     */
    @Override
    public void onDeleteContactClicked(int ID) {
        final int finalID = ID;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Confirm");
        builder.setMessage("Delete contact?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dbController.open();
                dbController.deleteContact(finalID);
                dbController.close();
                getAllContactsFromDB();
                fm.popBackStack();
                dialog.dismiss();
                Snackbar.make(findViewById(R.id.container), "Contact deleted", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }

        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Metod som uppdaterar en kontakt i databasen med nytt namn/nummer.
     * @param ID
     *          Unikt ID till den kontakt som ska uppdateras.
     * @param name
     *          Det namn det ska uppdateras till.
     * @param number
     *          Det nummer det ska uppdateras till.
     */
    @Override
    public void onUpdateContactClicked(int ID, String name, String number) {
        for(int i = 0; i < contacts.size(); i++) {
            if(contacts.get(i).getNumber().equals(number)) {
                Snackbar.make(findViewById(R.id.container), "Number already added", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                return;
            }
        }

        if(name.trim().length() == 0 || number.trim().length() == 0) {
            Snackbar.make(findViewById(R.id.container), "Please enter name and number", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        } else {
            dbController.open();
            dbController.updateContact(ID, name.trim(), number.replace(" ", ""));
            dbController.close();
            getAllContactsFromDB();
            fm.popBackStack();
            Snackbar.make(findViewById(R.id.container), "Contact saved", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    /**
     * Metod som sparar användarens textmeddelande i SharedPreferences.
     * @param message
     *          Textmeddelandet som användaren vill spara.
     */
    @Override
    public void onSaveMessageBtnClicked(String message) {
        prefs.edit().putString("textMessage", message).apply();
        fm.beginTransaction().replace(R.id.container, mainFragment).commit();
        navigationView.getMenu().getItem(0).setChecked(false);
        navigationView.getMenu().getItem(1).setChecked(false);
        navigationView.getMenu().getItem(2).setChecked(false);
        navigationView.getMenu().getItem(3).setChecked(false);
    }
}
