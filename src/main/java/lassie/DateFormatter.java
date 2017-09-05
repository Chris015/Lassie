package lassie;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatter {
    private SimpleDateFormat simpleDateFormat;

    public DateFormatter() {
        this.simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
    }

    public String format(long milliseconds){
        return simpleDateFormat.format(new Date(milliseconds));
    }

}
