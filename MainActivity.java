package com.techwin.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText input;
    private TextView result;
    private Spinner mode;

    private int dp(float x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }

    private TextView text(String s, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setPadding(dp(12), dp(8), dp(12), dp(8));
        return t;
    }

    private Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(15);
        return b;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(12,15,22));

        TextView title = text("TECHWIN", 30, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(55)));

        TextView sub = text("Aviator + Crash • Advanced Historical Analyzer", 14, false);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(42)));

        mode = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Aviator analysis","Crash analysis"});
        mode.setAdapter(ad);
        root.addView(mode, new LinearLayout.LayoutParams(-1, dp(48)));

        input = new EditText(this);
        input.setHint("Paste multipliers: 1.12 2.45 1.03 5.80 ...");
        input.setHintTextColor(Color.LTGRAY);
        input.setTextColor(Color.WHITE);
        input.setGravity(Gravity.TOP);
        input.setInputType(2 | 8192);
        input.setMinLines(7);
        root.addView(input, new LinearLayout.LayoutParams(-1, dp(175)));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button analyze = btn("Analyze");
        Button clear = btn("Clear");
        buttons.addView(analyze, new LinearLayout.LayoutParams(0, dp(54), 1));
        buttons.addView(clear, new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(buttons);

        ScrollView scroll = new ScrollView(this);
        result = text("Paste historical multipliers and tap Analyze.\n\nHistorical statistics cannot guarantee the next crash.", 15, false);
        result.setTextIsSelectable(true);
        scroll.addView(result);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        analyze.setOnClickListener(v -> analyze());
        clear.setOnClickListener(v -> { input.setText(""); result.setText("Cleared."); });

        setContentView(root);
    }

    private ArrayList<Double> parse(String raw) {
        ArrayList<Double> a = new ArrayList<Double>();
        if (raw == null) return a;
        for (String p : raw.trim().split("[,;\\s]+")) {
            try {
                double v = Double.parseDouble(p.toLowerCase(Locale.US).replace("x",""));
                if (Double.isFinite(v) && v > 0 && v <= 100000) a.add(v);
            } catch(Exception ignored) {}
        }
        return a;
    }

    private double pct(int n, int d) { return d == 0 ? 0 : 100.0*n/d; }
    private String f(double x) { return String.format(Locale.US, "%.2f", x); }

    private double median(ArrayList<Double> s) {
        int n=s.size();
        return (n%2==1) ? s.get(n/2) : (s.get(n/2-1)+s.get(n/2))/2.0;
    }

    private void analyze() {
        ArrayList<Double> a=parse(input.getText().toString());
        if(a.size()<2){ result.setText("Please enter at least 2 valid multipliers."); return; }

        int n=a.size(), lt120=0, lt150=0, lt200=0, ge200=0, ge500=0, ge1000=0;
        double sum=0, min=Double.MAX_VALUE, max=0;
        for(double v:a){
            sum+=v; min=Math.min(min,v); max=Math.max(max,v);
            if(v<1.2)lt120++; if(v<1.5)lt150++; if(v<2)lt200++;
            if(v>=2)ge200++; if(v>=5)ge500++; if(v>=10)ge1000++;
        }
        double mean=sum/n, var=0;
        for(double v:a) var+=(v-mean)*(v-mean);
        double sd=Math.sqrt(var/n);

        ArrayList<Double> s=new ArrayList<>(a); Collections.sort(s);
        int longest=0, cur=0;
        for(double v:a){ if(v<2){cur++; longest=Math.max(longest,cur);} else cur=0; }

        int[] bucket=new int[7];
        for(double v:a){
            if(v<1.2)bucket[0]++; else if(v<1.5)bucket[1]++;
            else if(v<2)bucket[2]++; else if(v<3)bucket[3]++;
            else if(v<5)bucket[4]++; else if(v<10)bucket[5]++; else bucket[6]++;
        }

        StringBuilder o=new StringBuilder();
        o.append("TECHWIN ANALYSIS\n==============================\n");
        o.append("Mode: ").append(mode.getSelectedItem()).append("\nRounds: ").append(n).append("\n\n");
        o.append("CORE STATISTICS\n");
        o.append("Average: ").append(f(mean)).append("x\n");
        o.append("Median: ").append(f(median(s))).append("x\n");
        o.append("Minimum: ").append(f(min)).append("x\n");
        o.append("Maximum: ").append(f(max)).append("x\n");
        o.append("Std. deviation: ").append(f(sd)).append("\n\n");

        o.append("DISTRIBUTION\n");
        String[] labels={"<1.20x","1.20–1.49x","1.50–1.99x","2.00–2.99x","3.00–4.99x","5.00–9.99x","10x+"};
        for(int i=0;i<7;i++) o.append(labels[i]).append(": ").append(bucket[i])
            .append(" (").append(f(pct(bucket[i],n))).append("%)\n");

        o.append("\nTHRESHOLDS\n");
        o.append("<1.20x: ").append(f(pct(lt120,n))).append("%\n");
        o.append("<1.50x: ").append(f(pct(lt150,n))).append("%\n");
        o.append("<2.00x: ").append(f(pct(lt200,n))).append("%\n");
        o.append("≥2.00x: ").append(f(pct(ge200,n))).append("%\n");
        o.append("≥5.00x: ").append(f(pct(ge500,n))).append("%\n");
        o.append("≥10.00x: ").append(f(pct(ge1000,n))).append("%\n");
        o.append("Longest <2x streak: ").append(longest).append("\n\n");

        o.append("LOW-STREAK FOLLOW-UP BACKTEST\n------------------------------\n");
        for(int k=1;k<=5;k++){
            int occ=0,n2=0,n5=0; double ns=0;
            for(int i=k;i<n;i++){
                boolean low=true;
                for(int j=i-k;j<i;j++) if(a.get(j)>=2){low=false;break;}
                if(low){ occ++; double next=a.get(i); ns+=next; if(next>=2)n2++; if(next>=5)n5++; }
            }
            if(occ==0) o.append("After ").append(k).append(" consecutive <2x: no sample\n");
            else o.append("After ").append(k).append(" consecutive <2x: ").append(occ)
                .append(" samples; next ≥2x ").append(f(pct(n2,occ))).append("%; next ≥5x ")
                .append(f(pct(n5,occ))).append("%; next avg ").append(f(ns/occ)).append("x\n");
        }

        o.append("\nIMPORTANT\n");
        o.append("• This is historical/statistical analysis, not a guaranteed signal.\n");
        o.append("• A low streak does not prove the next round will be high.\n");
        o.append("• Multipliers alone cannot establish the cause of a low result.\n");
        o.append("• No account password, OTP, PIN, cookie or session token is used.\n");
        result.setText(o.toString());
    }
}
