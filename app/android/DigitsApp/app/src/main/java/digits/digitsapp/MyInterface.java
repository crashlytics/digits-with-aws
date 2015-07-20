package digits.digitsapp;

import android.provider.ContactsContract;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

/*
 * A holder for lambda functions
 */
public interface MyInterface {

    /**
     * Invoke lambda function "echo". The function name is the method name
     */
    @LambdaFunction
    String digitsLogin(PhoneInfo phoneInfo);

    /**
     * Invoke lambda function "echo". The functionName in the annotation
     * overrides the default which is the method name
     */
    @LambdaFunction(functionName = "digitsLogin")
    void noDigitsLogin(PhoneInfo phoneInfo);
}