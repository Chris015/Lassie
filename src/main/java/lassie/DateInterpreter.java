package lassie;

import java.time.LocalDate;


public class DateInterpreter {
    /**
     * Interprets the program arguments and returns a date. If the argument is a properly formatted date, the argument is
     * returned unmodified. The argument must be formatted accordingly: yyyy-MM-dd.
     * If the argument is a number, the current date minus the number of days is returned.
     * If no arguments are are passed, the current date is returned.
     *
     * @param programArguments a String[] with a properly formatted date or a number
     * @return a date
     */
    public String interpret(String[] programArguments) {
        if (programArguments.length == 0) {
            return LocalDate.now().toString();
        }

        if (programArguments.length > 1) {
            throw new IllegalArgumentException("Only one argument allowed. Got: " + programArguments.length);
        }

        String argument = programArguments[0];
        String validDate = "^\\d{4}\\-(0?0[1-9]|1[012])\\-(0?0[1-9]|[12][0-9]|3[01])$";
        String validNumber = "^\\d+$";
        if (!argument.matches(validDate) && !argument.matches(validNumber)) {
            throw new IllegalArgumentException("Malformed argument: " + argument
                    + ".\nExpected a date (yyyy-MM-dd) or a number representing the amount of days back from current date."
            );
        }

        if (argument.matches(validDate)) {
            return argument;
        }

        return LocalDate.now().minusDays(Integer.parseInt(argument)).toString();
    }
}
