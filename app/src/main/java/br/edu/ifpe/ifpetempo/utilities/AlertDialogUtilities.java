package br.edu.ifpe.ifpetempo.utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;

import br.edu.ifpe.ifpetempo.R;

public class AlertDialogUtilities {

    public static void showNoLocationSettingsEnabledAlert(final Context c) {

        AlertDialog.Builder alert = new AlertDialog.Builder(c);
        alert.setMessage(c.getString(R.string.location_service_disabled));
        alert.setPositiveButton(c.getString(R.string.enable), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Intent locationSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                c.startActivity(locationSettingsIntent);
            }
        });
        alert.setNegativeButton(c.getString(R.string.cancel), null);
        alert.show();
    }

    public static void showDeleteFromDatabaseAlert(final Context c, final Runnable eraseFunc) {

        AlertDialog.Builder alert = new AlertDialog.Builder(c);
        alert.setTitle(c.getString(R.string.delete_entry));
        alert.setMessage(c.getString(R.string.are_you_sure));
        alert.setPositiveButton(c.getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                eraseFunc.run();
            }
        });
        alert.setNegativeButton(c.getString(R.string.cancel), null);
        alert.show();
    }

    public static void showNeedSigniInGooglePlusAlert(final Context c) {

        AlertDialog.Builder alert = new AlertDialog.Builder(c);
        alert.setMessage(c.getString(R.string.sign_in_gplus_needed));
        alert.setPositiveButton(c.getString(R.string.close), null);
        alert.show();
    }

}
