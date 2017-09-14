package lassie;

import org.junit.Before;
import org.junit.Test;


import java.time.LocalDate;

import static org.junit.Assert.*;

public class DateInterpreterTest {
    private DateInterpreter dateInterpreter;
    private String[] programArguments;

    @Before
    public void setUp() throws Exception {
        this.dateInterpreter = new DateInterpreter();
    }

    @Test
    public void numberOneReturnsCurrentDateMinusOneDay() throws Exception {
        programArguments = new String[]{"1"};
        String expected = LocalDate.now().minusDays(1).toString();
        String result = dateInterpreter.interpret(programArguments);
        assertEquals(expected, result);
    }

    @Test
    public void numberTwoReturnsCurrentDAteMinusTwoDays() throws Exception {
        programArguments = new String[]{"2"};
        String excepted = LocalDate.now().minusDays(2).toString();
        String result = dateInterpreter.interpret(programArguments);
        assertEquals(excepted, result);
    }

    @Test
    public void numberThreeReturnsCurrentDateMinusThreeDays() throws Exception {
        programArguments = new String[]{"3"};
        String expected = LocalDate.now().minusDays(3).toString();
        String result = dateInterpreter.interpret(programArguments);
        assertEquals(expected, result);
    }

    @Test
    public void properlyFormattedDateReturnsTheDate() throws Exception {
        programArguments = new String[]{"2017-09-14"};
        String expected = "2017-09-14";
        String result = dateInterpreter.interpret(programArguments);
        assertEquals(expected, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void improperlyFormattedDateReturnsIllegalArgumentException1() throws Exception {
        programArguments = new String[]{"2017/09/14"};
        dateInterpreter.interpret(programArguments);
    }

    @Test(expected = IllegalArgumentException.class)
    public void improperlyFormattedDateReturnsIllegalArgumentException2() throws Exception {
        programArguments = new String[]{"14-09-2017"};
        dateInterpreter.interpret(programArguments);
    }

    @Test(expected = IllegalArgumentException.class)
    public void improperlyFormattedDateReturnsIllegalArgumentException3() throws Exception {
        programArguments = new String[]{"2017-13-14"};
        dateInterpreter.interpret(programArguments);
    }

    @Test(expected = IllegalArgumentException.class)
    public void moreThanOneProgramArgumentReturnsIllegalArgumentException() throws Exception {
        programArguments = new String[]{"2017-09-12", "1"};
        dateInterpreter.interpret(programArguments);
    }

    @Test
    public void noArgumentsPassedReturnsCurrentDate() throws Exception {
        programArguments = new String[]{};
        String expected = LocalDate.now().toString();
        String result = dateInterpreter.interpret(programArguments);
        assertEquals(expected, result);
    }
}
