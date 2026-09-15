package com.lordtolan.promptvault;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    static final int PURPLE=Color.rgb(108,77,255), BG=Color.rgb(247,247,252), INK=Color.rgb(25,27,38);
    LinearLayout list; EditText search; TextView empty; ArrayList<Prompt> prompts=new ArrayList<>();
    SharedPreferences prefs; String filter="All"; static final int EXPORT=10, IMPORT=11;

    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("vault",0); load(); home(); }
    TextView text(String s,int sp,boolean bold){ TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(INK);v.setPadding(8,8,8,8);if(bold)v.setTypeface(null,Typeface.BOLD);return v; }
    Button button(String s){ Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b; }
    LinearLayout vertical(){ LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(20,18,20,18);return l; }
    public void home(){
        LinearLayout root=vertical();root.setBackgroundColor(BG);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("PromptVault",25,true);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button io=button("Backup");io.setOnClickListener(v->backupMenu());bar.addView(io);root.addView(bar);
        search=new EditText(this);search.setHint("Search prompts, tags, or categories");search.setSingleLine();root.addView(search,new LinearLayout.LayoutParams(-1,-2));
        HorizontalScrollView hsv=new HorizontalScrollView(this);LinearLayout cats=new LinearLayout(this);String[] cs={"All","Favorites","Solar","Customer","Coding","OrbitFlowAI","Legal","Personal"};
        for(String c:cs){Button x=button(c);x.setOnClickListener(v->{filter=c;render();});cats.addView(x);}hsv.addView(cats);root.addView(hsv);
        ScrollView sv=new ScrollView(this);list=vertical();sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        Button add=button("＋  New prompt");add.setTextSize(17);add.setTextColor(Color.WHITE);add.setBackgroundColor(PURPLE);add.setOnClickListener(v->edit(null));root.addView(add);
        setContentView(root);search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){}public void onTextChanged(CharSequence s,int a,int b,int c){render();}public void afterTextChanged(Editable e){}});render();
    }
    void render(){ list.removeAllViews();String q=search.getText().toString().toLowerCase(Locale.US);int count=0;
        ArrayList<Prompt> sorted=new ArrayList<>(prompts);sorted.sort((a,b)->Boolean.compare(b.favorite,a.favorite));
        for(Prompt p:sorted){String hay=(p.name+" "+p.category+" "+p.tags+" "+p.body).toLowerCase(Locale.US);if(!hay.contains(q))continue;if(filter.equals("Favorites")&&!p.favorite)continue;if(!filter.equals("All")&&!filter.equals("Favorites")&&!p.category.equals(filter))continue;count++;list.addView(card(p));}
        if(count==0){empty=text(prompts.isEmpty()?"Your prompt vault is empty.\nTap New prompt to save your first reusable prompt.":"No matching prompts.",16,false);empty.setGravity(Gravity.CENTER);empty.setPadding(20,80,20,20);list.addView(empty);}
    }
    View card(Prompt p){LinearLayout c=vertical();c.setBackgroundColor(Color.WHITE);LinearLayout row=new LinearLayout(this);TextView n=text(p.name,18,true);row.addView(n,new LinearLayout.LayoutParams(0,-2,1));Button star=button(p.favorite?"★":"☆");star.setOnClickListener(v->{p.favorite=!p.favorite;save();render();});row.addView(star);c.addView(row);c.addView(text(p.category+(p.tags.isEmpty()?"":"  •  "+p.tags),13,false));String preview=p.body.length()>160?p.body.substring(0,160)+"…":p.body;c.addView(text(preview,15,false));LinearLayout actions=new LinearLayout(this);Button use=button("Use");use.setOnClickListener(v->usePrompt(p));Button copy=button("Copy");copy.setOnClickListener(v->copy(p.body));Button share=button("Share");share.setOnClickListener(v->share(p.body));Button edit=button("Edit");edit.setOnClickListener(v->edit(p));actions.addView(use);actions.addView(copy);actions.addView(share);actions.addView(edit);c.addView(actions);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,8,0,8);return wrap(c,lp); }
    View wrap(View v,LinearLayout.LayoutParams lp){FrameLayout f=new FrameLayout(this);f.setPadding(2,2,2,2);f.addView(v);f.setLayoutParams(lp);return f;}
    void edit(Prompt existing){final Prompt p=existing==null?new Prompt():existing;LinearLayout form=vertical();EditText name=input("Prompt name",p.name);EditText cat=input("Category",p.category);EditText tags=input("Tags (comma separated)",p.tags);EditText notes=input("Notes / when to use",p.notes);EditText body=input("Prompt text — variables use {braces}",p.body);body.setMinLines(8);form.addView(name);form.addView(cat);form.addView(tags);form.addView(notes);form.addView(body);new AlertDialog.Builder(this).setTitle(existing==null?"New prompt":"Edit prompt").setView(form).setNegativeButton("Cancel",null).setNeutralButton(existing==null?"":"Delete",(d,w)->confirmDelete(p)).setPositiveButton("Save",(d,w)->{if(name.getText().toString().trim().isEmpty()){toast("A name is required");return;}p.name=name.getText().toString().trim();p.category=cat.getText().toString().trim();if(p.category.isEmpty())p.category="Personal";p.tags=tags.getText().toString().trim();p.notes=notes.getText().toString().trim();p.body=body.getText().toString();p.updated=System.currentTimeMillis();if(existing==null)prompts.add(p);save();render();}).show();}
    EditText input(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(INK);return e;}
    void usePrompt(Prompt p){LinkedHashSet<String> vars=new LinkedHashSet<>();Matcher m=Pattern.compile("\\{([^{}]+)\\}").matcher(p.body);while(m.find())vars.add(m.group(1));if(vars.isEmpty()){result(p.body);return;}LinearLayout f=vertical();HashMap<String,EditText> fields=new LinkedHashMap<>();for(String key:vars){EditText e=input(key,"");fields.put(key,e);f.addView(e);}new AlertDialog.Builder(this).setTitle("Fill template variables").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Generate",(d,w)->{String out=p.body;for(Map.Entry<String,EditText> e:fields.entrySet())out=out.replace("{"+e.getKey()+"}",e.getValue().getText().toString());result(out);}).show();}
    void result(String out){LinearLayout l=vertical();TextView t=text(out,15,false);t.setTextIsSelectable(true);l.addView(t);new AlertDialog.Builder(this).setTitle("Completed prompt").setView(l).setNegativeButton("Close",null).setNeutralButton("Share",(d,w)->share(out)).setPositiveButton("Copy",(d,w)->copy(out)).show();}
    void copy(String s){((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Prompt",s));toast("Copied");}
    void share(String s){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,s);startActivity(Intent.createChooser(i,"Share prompt"));}
    void confirmDelete(Prompt p){new AlertDialog.Builder(this).setTitle("Delete prompt?").setMessage(p.name).setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{prompts.remove(p);save();render();}).show();}
    void backupMenu(){new AlertDialog.Builder(this).setTitle("Backup and restore").setItems(new String[]{"Export JSON backup","Import JSON backup"},(d,w)->{if(w==0){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"promptvault-backup.json");startActivityForResult(i,EXPORT);}else{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");startActivityForResult(i,IMPORT);}}).show();}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(c!=RESULT_OK||data==null)return;try{Uri u=data.getData();if(r==EXPORT){try(OutputStream o=getContentResolver().openOutputStream(u)){o.write(toJson().toString(2).getBytes(StandardCharsets.UTF_8));}toast("Backup exported");}else if(r==IMPORT){StringBuilder s=new StringBuilder();try(BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(u)))){String line;while((line=br.readLine())!=null)s.append(line);}fromJson(new JSONArray(s.toString()));save();render();toast("Backup imported");}}catch(Exception e){toast("Could not read that backup");}}
    JSONArray toJson(){JSONArray a=new JSONArray();for(Prompt p:prompts)a.put(p.json());return a;}
    void fromJson(JSONArray a)throws JSONException{ArrayList<Prompt> next=new ArrayList<>();for(int i=0;i<a.length();i++)next.add(Prompt.from(a.getJSONObject(i)));prompts=next;}
    void save(){prefs.edit().putString("prompts",toJson().toString()).apply();}
    void load(){try{String s=prefs.getString("prompts","");if(!s.isEmpty())fromJson(new JSONArray(s));}catch(Exception ignored){} }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    static class Prompt{String id=UUID.randomUUID().toString(),name="",category="",tags="",notes="",body="";boolean favorite=false;long updated=System.currentTimeMillis();JSONObject json(){try{return new JSONObject().put("id",id).put("name",name).put("category",category).put("tags",tags).put("notes",notes).put("body",body).put("favorite",favorite).put("updated",updated);}catch(Exception e){return new JSONObject();}}static Prompt from(JSONObject o){Prompt p=new Prompt();p.id=o.optString("id",p.id);p.name=o.optString("name");p.category=o.optString("category","Personal");p.tags=o.optString("tags");p.notes=o.optString("notes");p.body=o.optString("body");p.favorite=o.optBoolean("favorite");p.updated=o.optLong("updated");return p;}}
}
