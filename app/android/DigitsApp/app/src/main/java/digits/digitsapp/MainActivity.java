package digits.digitsapp;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import io.fabric.sdk.android.Fabric;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;

import com.digits.sdk.android.AuthCallback;
import com.digits.sdk.android.Digits;
import com.digits.sdk.android.DigitsAuthButton;
import com.digits.sdk.android.DigitsException;
import com.digits.sdk.android.DigitsSession;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MainActivity extends Activity {

    static Properties properties = null;

    // Get your Twitter key/secret by following the below steps:
    //
    //  1.) Download and install Fabric (https://fabric.io)
    //  2.) Install the Digits Kit into your Android app
    //  3.) Log into your Fabric dashboard (https://fabric.io/dashboard)
    //  4.) Select your app from the top drop-down and then click on the Digits icon to the left
    //  5.) Your key/secret should appear on the page to the right
    //
    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    // Note: moved these into a properties file; please see getProperty()
    // private static final String TWITTER_KEY = "YOUR_TWITTER_KEY_HERE";
    // private static final String TWITTER_SECRET = "YOUR_TWITTER_SECRET_HERE";

    // Get your AWS identity pool by following the below steps:
    //
    //  1.) Log into the Amazon Cognito console (https://console.aws.amazon.com/cognito)
    //  2.) Click on "Create new Identity pool"
    //  3.) Specify a name for the pool and also specify your Twitter key/secret
    //
    // Note: moved these into a properties file; please see getProperty()
    // private static final String AWS_IDENTITY_POOL_ID = "YOUR_AWS_IDENTITY_POOL_ID";

    private static final String TAG = "DIGITS_SAMPLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        TwitterAuthConfig authConfig = new TwitterAuthConfig(getProperty("TWITTER_KEY"), getProperty("TWITTER_SECRET"));
        Fabric.with(this, new TwitterCore(authConfig), new Digits());
        setContentView(R.layout.activity_main);

        final CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    this, // get the context for the current activity
                getProperty("AWS_IDENTITY_POOL_ID"), // your identity pool id
                Regions.US_EAST_1 //Region
        );

        // Create a digits button and callback
        DigitsAuthButton digitsButton = (DigitsAuthButton) findViewById(R.id.auth_button);
        digitsButton.setCallback(new AuthCallback() {

            @Override
            public void success(DigitsSession session, String phoneNumber) {
                Log.v(TAG, "DIGITS SUCCESSFUL authentication");
                TwitterAuthToken authToken = (TwitterAuthToken)session.getAuthToken();
                String value = authToken.token + ";" + authToken.secret;
                Map<String, String> logins = new HashMap<String, String>();
                logins.put("www.digits.com", value);

                // Store the data in Amazon Cognito
                credentialsProvider.setLogins(logins);

                // Send the data to Amazon Lambda
                // 1. Setup a PhoneInfo (containing relevant information)
                PhoneInfo ph = new PhoneInfo();
                ph.setPhoneNumber(phoneNumber);
                ph.setId(session.getId());
                ph.setAccessToken(authToken.token);
                ph.setAccessTokenSecret(authToken.secret);

                // 2. Send the data to the function sendData to parse the request asynchronously
                sendData(ph);

            }

            @Override
            public void failure(DigitsException exception) {
                // Do something on failure
                Log.d(TAG, "Oops Digits issue");
            }
        });

        // Create a Twitter login button and callback
        TwitterLoginButton twitterButton = (TwitterLoginButton) findViewById(R.id.login_button);
        twitterButton.setCallback(new Callback<TwitterSession>() {

            @Override
            public void success(Result<TwitterSession> result) {

                Log.v(TAG, "TWITTER SUCCESSFUL authentication");

                TwitterSession session = result.data;
                TwitterAuthToken authToken = session.getAuthToken();
                String value = authToken.token + ";" + authToken.secret;
                Map<String, String> logins = new HashMap<String, String>();
                logins.put("api.twitter.com", value);

                // Store the data in Amazon Cognito
                credentialsProvider.setLogins(logins);

                // Send the data to Amazon Lambda
                // 1. Setup a PhoneInfo (containing relevant information)
                PhoneInfo ph = new PhoneInfo();
                ph.setUserName(session.getUserName());
                ph.setUserId(session.getUserId());
                ph.setId(session.getId());
                ph.setAccessToken(authToken.token);
                ph.setAccessTokenSecret(authToken.secret);

                // 2. Send the data to the function sendData to parse the request asynchronously
                sendData(ph);

            }

            @Override
            public void failure(TwitterException exception) {
                // Do something on failure
                Log.d(TAG, "Oops Twitter issue");
            }
        });
    }

    private void sendData(PhoneInfo phoneInfo){

        Log.d(TAG, "LAMBDA: Sending Data");
        // 1. Setup a provider to allow posting to Amazon Lambda
        final AWSCredentialsProvider provider = new CognitoCachingCredentialsProvider(
                this,
                getProperty("AWS_IDENTITY_POOL_ID"),
                Regions.US_EAST_1);

        // 2. Setup a LambdaInvoker Factory w/ provider data
        LambdaInvokerFactory factory = new LambdaInvokerFactory(
                this.getApplicationContext(),
                Regions.US_EAST_1,
                provider);

        // 3. Create an interface (see MyInterface)
        final MyInterface myInterface = factory.build(MyInterface.class);

        // 3. Send the data to the "digitsLogin" function on Amazon Lambda.
        // Note: Make sure it is done in background, not in main thread.
        new AsyncTask<PhoneInfo, Void, String>() {
            
            @Override
            protected String doInBackground(PhoneInfo... params) {
                // invoke "echo" method. In case it fails, it will throw a
                // LambdaFunctionException.
                try {
                    Log.d(TAG, "LAMBDA: Attempting to send data");
                    return myInterface.digitsLogin(params[0]);
                } catch (LambdaFunctionException lfe) {
                    Log.e("amazon", "Failed to invoke echo", lfe);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result == null) {
                    Log.d(TAG, "LAMBDA: Response from request is null");
                    return;
                } else {
                    Log.d(TAG, "LAMBDA: Received result");
                    Log.d(TAG, result);
                }
                // Do a toast
                Log.d(TAG, "LAMBDA: Making Toast with result");
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();

            }
        }.execute(phoneInfo);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {

            // Clear session on logout
            Digits.getSessionManager().clearActiveSession();
            Twitter.getSessionManager().clearActiveSession();

        } else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String getProperty(String key) {

        try {
            if (properties == null) {
                properties = new Properties();
                InputStream inputStream =
                        this.getClass().getClassLoader().getResourceAsStream("assets/app.properties");
                properties.load(inputStream);
            }
            return properties.getProperty(key);
        } catch (IOException e){
            Log.d(TAG, "Error reading properties file: " + e.toString());
            return null;
        }
    }
}
