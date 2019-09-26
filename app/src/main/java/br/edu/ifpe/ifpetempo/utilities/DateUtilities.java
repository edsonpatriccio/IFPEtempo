package br.edu.ifpe.ifpetempo.utilities;

import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtilities {

    private static String TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    @NonNull
    public static String milisToDate(long milis) {

        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        Date resultDate = new Date(milis);

        return sdf.format(resultDate);
    }
}
