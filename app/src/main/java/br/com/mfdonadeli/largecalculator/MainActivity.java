package br.com.mfdonadeli.largecalculator;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity  {

    private final int ADDITION = 1;
    private final int SUBSTRACTION = 2;
    private final int MULTIPLICATION = 3;
    private final int DIVISION = 4;
    private final int EQUAL = 0;
    private final int RANDOM = 5;

    private final int POWER = 6;
    private final int DIV_BY_ONE = 7;
    private final int ROOT = 8;
    private final int FACTORIAL = 9;

    private final int CALC_OK = 0;
    private final int CALC_TIMEOUT = 1;
    private final int CALC_ERROR = 2;

    private final int ROUND_CASES_DEF = 100;
    private final int TIME_OUT_DEF = 60;

    private int ROUND_CASES = ROUND_CASES_DEF;
    private int TIME_OUT = TIME_OUT_DEF;

    private boolean bDot = false;
    private boolean pressOperation = false;
    private String strAtual = "";
    private BigDecimal bigInt1;
    private BigDecimal bigResult;
    private AutoResizeTextView txtView;
    private TextView txtDigits;
    private Toolbar toolbar;
    private int op_to_do = 0;

    private int mResult = CALC_OK;
    private ProgressDialog mDialog;
    private Handler mHandler;
    private controlTask mTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        txtView = (AutoResizeTextView)findViewById(R.id.result);
        txtView.setMovementMethod(new ScrollingMovementMethod());
        txtView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ClipboardManager cManager = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData cData = ClipData.newPlainText("text", txtView.getText());
                cManager.setPrimaryClip(cData);
                Toast.makeText(getBaseContext(), "Text Copied", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        txtDigits = (TextView)findViewById(R.id.txtDigits);

        clear();
        customizeButton();
        setupSettings();



        //startActivity(new Intent(MainActivity.this, Main2Activity.class));
    }


    public void setupSettings()
    {
        final SharedPreferences sharedpreferences = getSharedPreferences("SETTINGS", Context.MODE_PRIVATE);
        ROUND_CASES = sharedpreferences.getInt("Precision", ROUND_CASES_DEF);

        final TextView textPrecision = (TextView) findViewById(R.id.txtPrecision);
        final SeekBar seekPrecision = (SeekBar) findViewById(R.id.seekPrecision);
        textPrecision.setText(String.valueOf(ROUND_CASES));
        seekPrecision.setProgress(ROUND_CASES);

        seekPrecision.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b)
                {
                    if(i>=0 && i<=seekPrecision.getMax())
                    {
                        textPrecision.setText(String.valueOf(i));
                        seekPrecision.setSecondaryProgress(i);
                        ROUND_CASES = i;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putInt("Precision", seekBar.getSecondaryProgress());
                editor.commit();
            }
        });

        TIME_OUT = sharedpreferences.getInt("TimeOut", TIME_OUT_DEF);
        final TextView textTimeOut = (TextView) findViewById(R.id.txtTimeOut);
        final SeekBar seekTimeOut = (SeekBar) findViewById(R.id.seekTimeOut);
        textTimeOut.setText(String.valueOf(TIME_OUT));
        seekTimeOut.setProgress(TIME_OUT);

        seekTimeOut.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b)
                {
                    if(i>=0 && i<=seekTimeOut.getMax())
                    {
                        textTimeOut.setText(String.valueOf(i));
                        seekTimeOut.setSecondaryProgress(i);
                        TIME_OUT = i;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putInt("TimeOut", seekBar.getSecondaryProgress());
                editor.commit();
            }
        });
    }

    public void customizeButton()
    {
        Button btnExp = (Button)findViewById(R.id.btnExp);
        btnExp.setText(Html.fromHtml("y<sup>x</sup"));
        ImageButton imgErase = (ImageButton)findViewById(R.id.btnErase);
        imgErase.setColorFilter(getResources().getColor(android.R.color.holo_green_dark), PorterDuff.Mode.MULTIPLY);
        Button btnRoot = (Button)findViewById(R.id.btnRoot);
        if(btnRoot!=null)
            btnRoot.setText(Html.fromHtml("<sup>y</sup>&#x221a;x"));
    }


    public void clickNumber(View view) {
        //startAnimation(view);

        if(pressOperation) {
            strAtual = "";
            pressOperation = false;
            bDot = false;
        }
        switch (view.getId()) {
            case R.id.btn0:
                if(strAtual.length() > 0)
                    strAtual += "0";
                break;
            case R.id.btnGoogol:
                BigDecimal googol = BigFunctions.intPower(BigDecimal.TEN, 100, 1);
                strAtual = googol.toString();
                break;
            case R.id.btnDot:
                if(!bDot) {
                    if(strAtual.isEmpty())
                        strAtual = "0.";
                    else
                        strAtual += ".";
                    bDot = true;
                }
                break;
            case R.id.btnMod:
                if(!strAtual.isEmpty())
                {
                    if (strAtual.indexOf('-') == -1)
                        strAtual = "-" + strAtual;
                    else
                        strAtual = strAtual.substring(1, strAtual.length());
                }
                break;
            case R.id.btnClear:
                clear();
                break;
            case R.id.btnErase:
                if(strAtual.length()>0) {
                    strAtual = strAtual.substring(0, strAtual.length() - 1);
                    if (strAtual.indexOf('.') == -1)
                        bDot = false;
                }
                else
                    clear();
                break;
            default:
                strAtual += ((Button)view).getText();
        }
        if(strAtual.length()>0)
            format(new BigDecimal(strAtual));
        else
            format(null);
    }

    public void clear()
    {
        strAtual = "";
        bDot = false;
        pressOperation = false;
        txtDigits.setText("0");
        op_to_do = 0;
    }

    public void error()
    {
        clear();
        if(mResult == CALC_ERROR)
            txtView.setText("ERROR");
        else if(mResult == CALC_TIMEOUT)
            txtView.setText("TimeOut");

        mResult = CALC_OK;
    }

    public void setError(int error)
    {
        mResult = error;
    }

    public void clickOperation(View view) {
        pressOperation = true;
        int operation = 0;
        switch (view.getId()) {
            case R.id.btnPlus:
                operation = ADDITION;
                break;
            case R.id.btnMinus:
                operation = SUBSTRACTION;
                break;
            case R.id.btnMult:
                operation = MULTIPLICATION;
                break;
            case R.id.btnDiv:
                operation = DIVISION;
                break;
            case R.id.btnEquals:
                operation = EQUAL;
                break;
            case R.id.btnRoot:
                operation = ROOT;
                break;
            case R.id.btnExp:
                operation = POWER;
                break;
        }

        doOperation(operation);
    }

    public void clickAddOperation(View view)
    {
        int operation = 0;
        switch (view.getId())
        {
            case R.id.btnDivByOne:
                operation = DIV_BY_ONE;
                break;
            case R.id.btnFactorial:
                operation = FACTORIAL;
                break;
            case R.id.btnDigits:
                operation = RANDOM;
                break;
        }

        doSpecialOperation(operation);
    }

    private void callResultado(int operation)
    {
        if(mResult != CALC_OK) {
            error();
            return;
        }

        strAtual = bigResult.toString();
        if(operation != FACTORIAL && operation != RANDOM && operation != DIV_BY_ONE)
            bigInt1 = bigResult;
        format(bigResult);
    }


    private void callOperation(BigDecimal big1, int operation)
    {
        callOperation(big1, null, operation);
    }

    private void callOperation(BigDecimal big1, BigDecimal big2, final int operation)
    {
        mResult = CALC_OK;

        mDialog = ProgressDialog.show(this, "Aguarde", "Processando...", false, false);
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what==0) {
                    callResultado(operation);
                    mDialog.dismiss();
                }
            }
        };

        mTask = new controlTask(big1, big2, operation);
        mTask.execute();


    }

    private void doOperation(int operation) {
        BigDecimal number_1;
        BigDecimal number_2;
        int op_calc;

        if(strAtual.isEmpty())
            strAtual = "0";

        if(op_to_do == 0) {
            op_to_do = operation;
            bigInt1 = new BigDecimal(strAtual);
            return;
        }

        number_2 = new BigDecimal(strAtual);
        number_1 = bigInt1;
        op_calc = op_to_do;

        op_to_do = operation;

        callOperation(number_1, number_2, op_calc);

    }

    private void doSpecialOperation(int operation)
    {
        BigDecimal number_1;

        if(strAtual.isEmpty())
            strAtual = "0";

        number_1 = new BigDecimal(strAtual);

        callOperation(number_1, operation);
    }

    public void format(BigDecimal bd)
    {
        if(bd==null) {
            txtView.setText("");
            return;
        }
        StringBuilder builder = new StringBuilder();
        for(int i=0; i<ROUND_CASES; i++)
            builder.append('#');


        DecimalFormat format = new DecimalFormat("###,###." + builder.toString());
        //DecimalFormat formatNoSep = new DecimalFormat("######" + builder.toString());
        String fmt = format.format(bd);

        txtView.setText(fmt);

        //fmt = formatNoSep.format(bd);
        txtDigits.setText(String.valueOf(bd.toString().length()));

    }

    private class controlTask extends AsyncTask {

        int operation;

        BigDecimal big1;
        BigDecimal big2;

        public controlTask(BigDecimal big1, BigDecimal big2, int operation)
        {
            this.big1 = big1;
            this.big2 = big2;
            this.operation = operation;
        }
        @Override
        protected Object doInBackground(Object[] objects)  {
            ExecutorService executor =  Executors.newSingleThreadExecutor();

            Runnable worker = new TaskOperation(this.big1, this.big2, this.operation);
            Future<?> future = executor.submit(worker);

            try {
                future.get(TIME_OUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                mResult = CALC_ERROR;
                e.printStackTrace();
            } catch (ExecutionException e) {
                mResult = CALC_ERROR;
                e.printStackTrace();
            } catch (TimeoutException e) {
                mResult = CALC_TIMEOUT;
                e.printStackTrace();
            }

            executor.shutdownNow();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDialog.dismiss();
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            mHandler.sendEmptyMessage(0);
        }
    }

    private class TaskOperation implements Runnable
    {
        int operation;

        BigDecimal big1;
        BigDecimal big2;

        public TaskOperation(BigDecimal big1, BigDecimal big2, int operation)
        {
            this.big1 = big1;
            this.big2 = big2;
            this.operation = operation;
        }

        @Override
        public void run()  {
            switch (this.operation) {
                case ADDITION:
                    bigResult = big1.add(big2);
                    break;
                case SUBSTRACTION:
                    bigResult = big1.subtract(big2);
                    break;
                case MULTIPLICATION:
                    bigResult = big1.multiply(big2);
                    break;
                case DIVISION:
                    bigResult = big1.divide(big2, ROUND_CASES, RoundingMode.HALF_UP);
                    break;
                case POWER:
                    if(big2.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) == 1) {
                        setError(CALC_ERROR);
                        break;
                    }
                    bigResult = BigFunctions.intPower(big1, big2.longValue(), 100);
                    break;
                case ROOT:
                    if(big2.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) == 1) {
                        setError(CALC_ERROR);
                        break;
                    }
                    bigResult = BigFunctions.intRoot(big1, big2.longValue(), 100);
                    break;
                case DIV_BY_ONE:
                    bigResult = BigDecimal.ONE.divide(big1, ROUND_CASES, RoundingMode.HALF_UP);
                    break;
                case FACTORIAL:
                    //bigResult = BigFunctions.factorial(big1, big1);
                    if(big1.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) == 1) {
                        setError(CALC_ERROR);
                        break;
                    }
                    bigResult = BigFunctions.factorial(big1.doubleValue());
                    break;
                case RANDOM:
                    bigResult = BigFunctions.generateBig(big1);
                    break;
            }
        }
    }

}
