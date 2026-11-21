package com.ekbatan.hse;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.io.File;
import java.nio.file.Files;

/** Local-only HSE database. All heavy operations should run off the main thread. */
public class LibraryDb extends SQLiteOpenHelper {
    private static final String DB = "hse.db";
    private static final int VER = 13;
    private static final int MAX_NOTE_CHARS = 250_000;

    public static class DocumentRow { public long id; public String title, category, path; public int pages; public long createdAt; }
    public static class NoteRow { public long id; public String title, body; public long updatedAt; }
    public static class ReminderRow { public long id; public String title, body, deliveryState, lastDeliveryError; public long at; public boolean done; }
    public static class RiskRow { public long id; public String hazard, consequence, controls, action, responsible, status; public int likelihood, severity, score; }
    public static class IncidentRow { public long id; public String title, description, immediate, rootCause, corrective, status; public long eventDate; }
    public static class Stats { public int documents, pages, notes, reminders, pendingReminders, risks, highRisks, incidents, openIncidents; }
    public static class SearchHit { public String title, category, snippet; public int page, score; SearchHit(String t,String c,int p,String s,int sc){title=t;category=c;page=p;snippet=s;score=sc;} }

    public LibraryDb(Context c) { super(c.getApplicationContext(), DB, null, VER); setWriteAheadLoggingEnabled(true); }
    @Override public void onConfigure(SQLiteDatabase db) { super.onConfigure(db); db.setForeignKeyConstraintsEnabled(true); try (Cursor c=db.rawQuery("PRAGMA busy_timeout=5000", null)) { if (c.moveToFirst()) { /* read result to execute pragma safely */ } } }
    @Override public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if(!db.isReadOnly() && hasColumn(db,"documents","title_normalized") && hasColumn(db,"documents","category_normalized")){
            boolean needsBackfill=false;
            Cursor c=db.rawQuery("SELECT COUNT(*) FROM documents WHERE title_normalized IS NULL OR category_normalized IS NULL",null);
            try{needsBackfill=c.moveToFirst() && c.getInt(0)>0;}finally{c.close();}
            if(needsBackfill) backfillNormalizedDocumentText(db);
        }
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE documents(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,category TEXT,filename TEXT,filepath TEXT NOT NULL,file_hash TEXT UNIQUE,created_at INTEGER NOT NULL,title_normalized TEXT,category_normalized TEXT)");
        db.execSQL("CREATE TABLE pages(id INTEGER PRIMARY KEY AUTOINCREMENT,document_id INTEGER NOT NULL,page_no INTEGER NOT NULL,text TEXT,normalized TEXT,FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,body TEXT,created_at INTEGER,updated_at INTEGER)");
        db.execSQL("CREATE TABLE reminders(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,body TEXT,at_millis INTEGER,done INTEGER DEFAULT 0,created_at INTEGER,delivery_state TEXT DEFAULT 'pending',last_delivery_error TEXT)");
        db.execSQL("CREATE TABLE risks(id INTEGER PRIMARY KEY AUTOINCREMENT,hazard TEXT,consequence TEXT,likelihood INTEGER,severity INTEGER,score INTEGER,controls TEXT,action TEXT,responsible TEXT,status TEXT,created_at INTEGER)");
        db.execSQL("CREATE TABLE incidents(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,event_date INTEGER,description TEXT,immediate_action TEXT,root_cause TEXT,corrective_action TEXT,status TEXT,created_at INTEGER)");
        db.execSQL("CREATE TABLE inspection_findings(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,category TEXT,evidence TEXT,certainty TEXT,hazard TEXT,consequence TEXT,immediate_action TEXT,corrective_action TEXT,priority TEXT,confidence TEXT,source TEXT NOT NULL DEFAULT 'AI',image_path TEXT,risk_id INTEGER,status TEXT NOT NULL DEFAULT 'تاییدشده',created_at INTEGER NOT NULL,FOREIGN KEY(risk_id) REFERENCES risks(id) ON DELETE SET NULL)");
        db.execSQL("CREATE TABLE ncrs(id INTEGER PRIMARY KEY AUTOINCREMENT,ncr_no TEXT NOT NULL UNIQUE,title TEXT NOT NULL,description TEXT,requirement TEXT,immediate_action TEXT,corrective_action TEXT,responsible TEXT,due_at INTEGER,status TEXT NOT NULL DEFAULT 'باز',finding_id INTEGER,created_at INTEGER NOT NULL,FOREIGN KEY(finding_id) REFERENCES inspection_findings(id) ON DELETE SET NULL)");
        createIndexes(db);
    }
    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pages_doc ON pages(document_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pages_norm ON pages(normalized)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reminders_at ON reminders(done,at_millis)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reminders_delivery ON reminders(done,delivery_state,at_millis)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_docs_created ON documents(created_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_docs_title_norm ON documents(title_normalized)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_docs_category_norm ON documents(category_normalized)");
        ensureHashIndex(db);
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_risks_score ON risks(score DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_incidents_date ON incidents(event_date DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_findings_created ON inspection_findings(created_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_findings_risk ON inspection_findings(risk_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ncr_created ON ncrs(created_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ncr_finding ON ncrs(finding_id)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV) {
        db.beginTransaction();
        try {
            ensureCoreTables(db);
            ensureColumns(db);
            if(oldV < 10) backfillNormalizedDocumentText(db);
            ensureReminderDeliveryState(db);
            createIndexes(db);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private void ensureCoreTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS documents(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,category TEXT,filename TEXT,filepath TEXT, file_hash TEXT,created_at INTEGER,title_normalized TEXT,category_normalized TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS pages(id INTEGER PRIMARY KEY AUTOINCREMENT,document_id INTEGER,page_no INTEGER,text TEXT,normalized TEXT,FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS notes(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,body TEXT,created_at INTEGER,updated_at INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS reminders(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,body TEXT,at_millis INTEGER,done INTEGER DEFAULT 0,created_at INTEGER,delivery_state TEXT DEFAULT 'pending',last_delivery_error TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS risks(id INTEGER PRIMARY KEY AUTOINCREMENT,hazard TEXT,consequence TEXT,likelihood INTEGER,severity INTEGER,score INTEGER,controls TEXT,action TEXT,responsible TEXT,status TEXT,created_at INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS incidents(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,event_date INTEGER,description TEXT,immediate_action TEXT,root_cause TEXT,corrective_action TEXT,status TEXT,created_at INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS inspection_findings(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,category TEXT,evidence TEXT,certainty TEXT,hazard TEXT,consequence TEXT,immediate_action TEXT,corrective_action TEXT,priority TEXT,confidence TEXT,source TEXT NOT NULL DEFAULT 'AI',image_path TEXT,risk_id INTEGER,status TEXT NOT NULL DEFAULT 'تاییدشده',created_at INTEGER NOT NULL,FOREIGN KEY(risk_id) REFERENCES risks(id) ON DELETE SET NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS ncrs(id INTEGER PRIMARY KEY AUTOINCREMENT,ncr_no TEXT NOT NULL UNIQUE,title TEXT NOT NULL,description TEXT,requirement TEXT,immediate_action TEXT,corrective_action TEXT,responsible TEXT,due_at INTEGER,status TEXT NOT NULL DEFAULT 'باز',finding_id INTEGER,created_at INTEGER NOT NULL,FOREIGN KEY(finding_id) REFERENCES inspection_findings(id) ON DELETE SET NULL)");
    }

    private void ensureColumns(SQLiteDatabase db) {
        addColumnIfMissing(db,"documents","title TEXT");
        addColumnIfMissing(db,"documents","category TEXT");
        addColumnIfMissing(db,"documents","filename TEXT");
        addColumnIfMissing(db,"documents","filepath TEXT");
        addColumnIfMissing(db,"documents","file_hash TEXT");
        addColumnIfMissing(db,"documents","created_at INTEGER");
        addColumnIfMissing(db,"documents","title_normalized TEXT");
        addColumnIfMissing(db,"documents","category_normalized TEXT");
        addColumnIfMissing(db,"pages","document_id INTEGER");
        addColumnIfMissing(db,"pages","page_no INTEGER");
        addColumnIfMissing(db,"pages","text TEXT");
        addColumnIfMissing(db,"pages","normalized TEXT");
        addColumnIfMissing(db,"notes","title TEXT");
        addColumnIfMissing(db,"notes","body TEXT");
        addColumnIfMissing(db,"notes","created_at INTEGER");
        addColumnIfMissing(db,"notes","updated_at INTEGER");
        addColumnIfMissing(db,"reminders","title TEXT");
        addColumnIfMissing(db,"reminders","body TEXT");
        addColumnIfMissing(db,"reminders","at_millis INTEGER");
        addColumnIfMissing(db,"reminders","done INTEGER DEFAULT 0");
        addColumnIfMissing(db,"reminders","created_at INTEGER");
        addColumnIfMissing(db,"reminders","delivery_state TEXT DEFAULT 'pending'");
        addColumnIfMissing(db,"reminders","last_delivery_error TEXT");
        addColumnIfMissing(db,"risks","hazard TEXT");
        addColumnIfMissing(db,"risks","consequence TEXT");
        addColumnIfMissing(db,"risks","likelihood INTEGER");
        addColumnIfMissing(db,"risks","severity INTEGER");
        addColumnIfMissing(db,"risks","score INTEGER");
        addColumnIfMissing(db,"risks","controls TEXT");
        addColumnIfMissing(db,"risks","action TEXT");
        addColumnIfMissing(db,"risks","responsible TEXT");
        addColumnIfMissing(db,"risks","status TEXT");
        addColumnIfMissing(db,"risks","created_at INTEGER");
        addColumnIfMissing(db,"incidents","title TEXT");
        addColumnIfMissing(db,"incidents","event_date INTEGER");
        addColumnIfMissing(db,"incidents","description TEXT");
        addColumnIfMissing(db,"incidents","immediate_action TEXT");
        addColumnIfMissing(db,"incidents","root_cause TEXT");
        addColumnIfMissing(db,"incidents","corrective_action TEXT");
        addColumnIfMissing(db,"incidents","status TEXT");
        addColumnIfMissing(db,"incidents","created_at INTEGER");
        addColumnIfMissing(db,"ncrs","ncr_no TEXT"); addColumnIfMissing(db,"ncrs","title TEXT"); addColumnIfMissing(db,"ncrs","description TEXT"); addColumnIfMissing(db,"ncrs","requirement TEXT"); addColumnIfMissing(db,"ncrs","immediate_action TEXT"); addColumnIfMissing(db,"ncrs","corrective_action TEXT"); addColumnIfMissing(db,"ncrs","responsible TEXT"); addColumnIfMissing(db,"ncrs","due_at INTEGER"); addColumnIfMissing(db,"ncrs","status TEXT DEFAULT 'باز'"); addColumnIfMissing(db,"ncrs","finding_id INTEGER"); addColumnIfMissing(db,"ncrs","created_at INTEGER");
    }

    /**
     * Repairs legacy reminder rows after schema upgrades. Older databases may have
     * a newly-added nullable delivery_state column or rows created before delivery
     * tracking existed. Such rows must remain deliverable rather than being treated
     * as failed or silently skipped.
     */
    private static void ensureReminderDeliveryState(SQLiteDatabase db){
        db.execSQL("UPDATE reminders SET delivery_state='pending' WHERE delivery_state IS NULL OR TRIM(delivery_state)=''");
        db.execSQL("UPDATE reminders SET delivery_state='pending' WHERE delivery_state NOT IN ('pending','scheduled','delivered','failed')");
        db.execSQL("UPDATE reminders SET last_delivery_error='' WHERE last_delivery_error IS NULL");
    }

    private static void addColumnIfMissing(SQLiteDatabase db,String table,String definition){
        String col=definition.substring(0,definition.indexOf(' '));
        if(!hasColumn(db,table,col)) db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+definition);
    }
    private static boolean hasColumn(SQLiteDatabase db,String table,String col){ Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null); try{while(c.moveToNext())if(col.equalsIgnoreCase(c.getString(1)))return true;return false;}finally{c.close();} }
    private static void backfillNormalizedDocumentText(SQLiteDatabase db){
        Cursor c=db.rawQuery("SELECT id,COALESCE(title,''),COALESCE(category,'') FROM documents",null);
        try{while(c.moveToNext()){ContentValues v=new ContentValues();v.put("title_normalized",PersianNormalizer.normalize(c.getString(1)));v.put("category_normalized",PersianNormalizer.normalize(c.getString(2)));db.update("documents",v,"id=?",new String[]{String.valueOf(c.getLong(0))});}}finally{c.close();}
    }
    private static void ensureHashIndex(SQLiteDatabase db){
        try { db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_docs_hash ON documents(file_hash)"); }
        catch(Exception e){ db.execSQL("CREATE INDEX IF NOT EXISTS idx_docs_hash_nonunique ON documents(file_hash)"); }
    }

    public boolean hasHash(String h){ if(h==null||h.isEmpty())return false; Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM documents WHERE file_hash=? LIMIT 1",new String[]{h}); try{return c.moveToFirst();}finally{c.close();} }
    public long addDocument(String title,String cat,String filename,String path,String hash){ ContentValues v=new ContentValues();v.put("title",limit(title,300));v.put("category",limit(cat,100));v.put("filename",limit(filename,300));v.put("filepath",path);v.put("file_hash",hash);v.put("created_at",System.currentTimeMillis());v.put("title_normalized",PersianNormalizer.normalize(title));v.put("category_normalized",PersianNormalizer.normalize(cat));return getWritableDatabase().insertOrThrow("documents",null,v); }
    public void addPage(long doc,int page,String raw){ ContentValues v=new ContentValues();v.put("document_id",doc);v.put("page_no",page);v.put("text",raw==null?"":raw);v.put("normalized",PersianNormalizer.normalize(raw));getWritableDatabase().insertOrThrow("pages",null,v); }

    public ArrayList<SearchHit> searchHits(String q){
        ArrayList<SearchHit> out=new ArrayList<>(); String n=PersianNormalizer.normalize(q); if(n.isEmpty()||n.length()>200)return out;
        String[] terms=n.split("\\s+"); String like="%"+escapeLike(n)+"%";
        StringBuilder where=new StringBuilder("p.normalized LIKE ? ESCAPE '\\' OR d.title_normalized LIKE ? ESCAPE '\\' OR d.category_normalized LIKE ? ESCAPE '\\'");
        ArrayList<String> args=new ArrayList<>(Arrays.asList(like,like,like));
        for(String term:terms){ if(term.length()>1&&term.length()<=80){ where.append(" OR p.normalized LIKE ? ESCAPE '\\'"); args.add("%"+escapeLike(term)+"%"); } }
        Cursor c=getReadableDatabase().rawQuery("SELECT d.title,COALESCE(d.category,''),p.page_no,p.text,p.normalized FROM pages p JOIN documents d ON d.id=p.document_id WHERE ("+where+") LIMIT 120",args.toArray(new String[0]));
        try{while(c.moveToNext()){String title=c.getString(0),cat=c.getString(1),raw=c.getString(3),norm=c.getString(4);int score=score(n,norm,PersianNormalizer.normalize(title),PersianNormalizer.normalize(cat));if(score>0)out.add(new SearchHit(title,cat,c.getInt(2),snippet(raw,terms),score));}}
        finally{c.close();}
        Collections.sort(out,(a,b)->Integer.compare(b.score,a.score)); if(out.size()>100)return new ArrayList<>(out.subList(0,100)); return out;
    }
    private static String escapeLike(String s){return s.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
    public ArrayList<String> search(String q){ ArrayList<String> out=new ArrayList<>();for(SearchHit h:searchHits(q))out.add(h.title+" — صفحه "+h.page+(h.category.isEmpty()?"":" — "+h.category)+" — امتیاز "+h.score+"\n"+h.snippet);return out; }
    private int score(String q,String page,String title,String category){ int s=0;if(page.contains(q))s+=70;if(title.contains(q))s+=100;if(category.contains(q))s+=30;String[] ts=q.split("\\s+");int matched=0;for(String t:ts){if(t.length()>1){if(page.contains(t)){matched++;s+=10;}else if(title.contains(t)||category.contains(t))matched++;}}if(ts.length>1&&matched==ts.length)s+=35;return s; }
    private String snippet(String raw,String[] terms){ if(raw==null||raw.isEmpty())return "";String clean=raw.replaceAll("[\\r\\n\\t]+"," ").replaceAll(" {2,}"," ").trim();if(clean.length()<=260)return clean;int best=0;int cursor=0;String[] words=clean.split("\\s+");outer:for(String word:words){String nw=PersianNormalizer.normalize(word);for(String t:terms){if(t.length()>1&&nw.contains(t)){best=cursor;break outer;}}cursor+=word.length()+1;}int a=Math.max(0,best-90),b=Math.min(clean.length(),a+250);return (a>0?"…":"")+clean.substring(a,b)+(b<clean.length()?"…":""); }

    /** Reliable local write: transaction + short retry for transient SQLite locks. */
    private long insertReliable(String table, ContentValues values) {
        if (values == null) throw new IllegalArgumentException("داده‌ای برای ذخیره وجود ندارد");
        SQLiteException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            SQLiteDatabase d = getWritableDatabase();
            d.beginTransaction();
            try {
                long id = d.insertOrThrow(table, null, values);
                d.setTransactionSuccessful();
                return id;
            } catch (SQLiteException e) {
                last = e;
                if (!isTransientLock(e) || attempt == 2) throw e;
            } finally {
                d.endTransaction();
            }
            try { Thread.sleep(80L * (attempt + 1)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("ذخیره متوقف شد", e); }
        }
        throw last == null ? new IllegalStateException("ذخیره ناموفق بود") : last;
    }
    private static boolean isTransientLock(SQLiteException e) {
        String m = e.getMessage(); return m != null && (m.toLowerCase(Locale.ROOT).contains("locked") || m.toLowerCase(Locale.ROOT).contains("busy"));
    }
    public long addNote(String title,String body){
        String t=title==null?"":title.trim(), b=body==null?"":body.trim();
        if(t.isEmpty() && b.isEmpty()) throw new IllegalArgumentException("عنوان یا متن یادداشت خالی است");
        if(t.length()>500) throw new IllegalArgumentException("عنوان یادداشت بیش از ۵۰۰ نویسه است");
        if(b.length()>MAX_NOTE_CHARS) throw new IllegalArgumentException("متن یادداشت بیش از ۲۵۰٬۰۰۰ نویسه است");
        ContentValues v=new ContentValues(); long now=System.currentTimeMillis();
        v.put("title",t);v.put("body",b);v.put("created_at",now);v.put("updated_at",now);
        return insertReliable("notes",v);
    }
    public long addReminder(String title,String body,long at){if(at<=0)throw new IllegalArgumentException("زمان یادآوری نامعتبر است");String t=title==null?"":title.trim();if(t.isEmpty())throw new IllegalArgumentException("عنوان یادآوری خالی است");ContentValues v=new ContentValues();v.put("title",limit(t,500));v.put("body",limit(body,10000));v.put("at_millis",at);v.put("done",0);v.put("created_at",System.currentTimeMillis());v.put("delivery_state","pending");v.put("last_delivery_error","");return insertReliable("reminders",v);}
    public ReminderRow getReminder(long id){Cursor c=getReadableDatabase().rawQuery("SELECT id,COALESCE(title,''),COALESCE(body,''),at_millis,done,COALESCE(delivery_state,'pending'),COALESCE(last_delivery_error,'') FROM reminders WHERE id=?",new String[]{String.valueOf(id)});try{if(!c.moveToFirst())return null;ReminderRow r=new ReminderRow();r.id=c.getLong(0);r.title=c.getString(1);r.body=c.getString(2);r.at=c.getLong(3);r.done=c.getInt(4)!=0;r.deliveryState=c.getString(5);r.lastDeliveryError=c.getString(6);return r;}finally{c.close();}}
    public void setReminderDone(long id,boolean done){ContentValues v=new ContentValues();v.put("done",done?1:0);if(done){v.put("delivery_state","delivered");v.put("last_delivery_error","");}else{v.put("delivery_state","pending");v.put("last_delivery_error","");}getWritableDatabase().update("reminders",v,"id=?",new String[]{String.valueOf(id)});}
    /**
     * Persists a reminder delivery state. Unknown states are normalized to pending so
     * future versions cannot accidentally make a reminder permanently undiscoverable.
     */
    public void markReminderDelivery(long id,String state,String error){
        if(id<=0)return;
        String normalized=("pending".equals(state)||"scheduled".equals(state)||"delivered".equals(state)||"failed".equals(state))?state:"pending";
        ContentValues v=new ContentValues();
        v.put("delivery_state",normalized==null?"pending":normalized);
        v.put("last_delivery_error",limit(error,500));
        getWritableDatabase().update("reminders",v,"id=?",new String[]{String.valueOf(id)});
    }
    public void deleteReminder(long id){getWritableDatabase().delete("reminders","id=?",new String[]{String.valueOf(id)});}
    public long addRisk(String h,String c,int l,int s,String controls,String action,String resp){if(l<1||l>5||s<1||s>5)throw new IllegalArgumentException("مقادیر احتمال و شدت باید بین ۱ تا ۵ باشند");String hz=h==null?"":h.trim();if(hz.isEmpty())throw new IllegalArgumentException("خطر یا فعالیت خالی است");ContentValues v=new ContentValues();v.put("hazard",limit(hz,500));v.put("consequence",limit(c,2000));v.put("likelihood",l);v.put("severity",s);v.put("score",l*s);v.put("controls",limit(controls,5000));v.put("action",limit(action,5000));v.put("responsible",limit(resp,500));v.put("status","باز");v.put("created_at",System.currentTimeMillis());return insertReliable("risks",v);}
    public void setRiskStatus(long id,String status){ContentValues v=new ContentValues();v.put("status",limit(status,50));getWritableDatabase().update("risks",v,"id=?",new String[]{String.valueOf(id)});}
    public void deleteRisk(long id){getWritableDatabase().delete("risks","id=?",new String[]{String.valueOf(id)});}
    public long addIncident(String t,long date,String d,String ia,String rc,String ca){String title=t==null?"":t.trim();if(title.isEmpty())throw new IllegalArgumentException("عنوان رویداد خالی است");if(date<=0)throw new IllegalArgumentException("تاریخ رویداد نامعتبر است");ContentValues v=new ContentValues();v.put("title",limit(title,500));v.put("event_date",date);v.put("description",limit(d,10000));v.put("immediate_action",limit(ia,5000));v.put("root_cause",limit(rc,5000));v.put("corrective_action",limit(ca,5000));v.put("status","باز");v.put("created_at",System.currentTimeMillis());return insertReliable("incidents",v);}
    public void setIncidentStatus(long id,String status){ContentValues v=new ContentValues();v.put("status",limit(status,50));getWritableDatabase().update("incidents",v,"id=?",new String[]{String.valueOf(id)});}
    public void deleteIncident(long id){getWritableDatabase().delete("incidents","id=?",new String[]{String.valueOf(id)});}
    public void deleteNote(long id){getWritableDatabase().delete("notes","id=?",new String[]{String.valueOf(id)});}
    public void deleteDocument(long id){
        if(id<=0)return; SQLiteDatabase d=getWritableDatabase(); String path=null; Cursor c=d.rawQuery("SELECT filepath FROM documents WHERE id=?",new String[]{String.valueOf(id)});
        try{if(c.moveToFirst())path=c.getString(0);}finally{c.close();}
        d.delete("documents","id=?",new String[]{String.valueOf(id)});
        deleteOwnedLibraryFile(path);
    }
    private void deleteOwnedLibraryFile(String path){
        if(path==null||path.isEmpty())return; try{File f=new File(path).getCanonicalFile();File base=new File(getContext().getFilesDir(),"library").getCanonicalFile();if(f.getPath().startsWith(base.getPath()+File.separator)&&f.isFile()&&!java.nio.file.Files.isSymbolicLink(f.toPath()))f.delete();}catch(Exception ignored){}
    }
    private static String limit(String s,int n){if(s==null)return "";return s.length()>n?s.substring(0,n):s;}

    public ArrayList<DocumentRow> listDocuments(){ArrayList<DocumentRow> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT d.id,d.title,COALESCE(d.category,''),d.filepath,d.created_at,(SELECT COUNT(*) FROM pages p WHERE p.document_id=d.id) FROM documents d ORDER BY d.created_at DESC LIMIT 100",null);try{while(c.moveToNext()){DocumentRow r=new DocumentRow();r.id=c.getLong(0);r.title=c.getString(1);r.category=c.getString(2);r.path=c.getString(3);r.createdAt=c.getLong(4);r.pages=c.getInt(5);out.add(r);}}finally{c.close();}return out;}
    public ArrayList<NoteRow> listNotes(int limit){limit=clampLimit(limit);ArrayList<NoteRow> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,COALESCE(title,''),COALESCE(body,''),updated_at FROM notes ORDER BY updated_at DESC LIMIT ?",new String[]{String.valueOf(limit)});try{while(c.moveToNext()){NoteRow r=new NoteRow();r.id=c.getLong(0);r.title=c.getString(1);r.body=c.getString(2);r.updatedAt=c.getLong(3);out.add(r);}}finally{c.close();}return out;}
    public ArrayList<ReminderRow> listReminders(int limit){limit=clampLimit(limit);ArrayList<ReminderRow> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,COALESCE(title,''),COALESCE(body,''),at_millis,done,COALESCE(delivery_state,'pending'),COALESCE(last_delivery_error,'') FROM reminders ORDER BY done ASC,at_millis ASC LIMIT ?",new String[]{String.valueOf(limit)});try{while(c.moveToNext()){ReminderRow r=new ReminderRow();r.id=c.getLong(0);r.title=c.getString(1);r.body=c.getString(2);r.at=c.getLong(3);r.done=c.getInt(4)!=0;r.deliveryState=c.getString(5);r.lastDeliveryError=c.getString(6);out.add(r);}}finally{c.close();}return out;}
    public RiskRow getRisk(long id){if(id<=0)return null;Cursor c=getReadableDatabase().rawQuery("SELECT id,hazard,COALESCE(consequence,''),likelihood,severity,score,COALESCE(controls,''),COALESCE(action,''),COALESCE(responsible,''),COALESCE(status,'باز') FROM risks WHERE id=? LIMIT 1",new String[]{String.valueOf(id)});try{if(!c.moveToFirst())return null;RiskRow r=new RiskRow();r.id=c.getLong(0);r.hazard=c.getString(1);r.consequence=c.getString(2);r.likelihood=c.getInt(3);r.severity=c.getInt(4);r.score=c.getInt(5);r.controls=c.getString(6);r.action=c.getString(7);r.responsible=c.getString(8);r.status=c.getString(9);return r;}finally{c.close();}}
    public ArrayList<RiskRow> listRisks(int limit){limit=clampLimit(limit);ArrayList<RiskRow> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,hazard,COALESCE(consequence,''),likelihood,severity,score,COALESCE(controls,''),COALESCE(action,''),COALESCE(responsible,''),COALESCE(status,'باز') FROM risks ORDER BY score DESC,created_at DESC LIMIT ?",new String[]{String.valueOf(limit)});try{while(c.moveToNext()){RiskRow r=new RiskRow();r.id=c.getLong(0);r.hazard=c.getString(1);r.consequence=c.getString(2);r.likelihood=c.getInt(3);r.severity=c.getInt(4);r.score=c.getInt(5);r.controls=c.getString(6);r.action=c.getString(7);r.responsible=c.getString(8);r.status=c.getString(9);out.add(r);}}finally{c.close();}return out;}
    public ArrayList<IncidentRow> listIncidents(int limit){limit=clampLimit(limit);ArrayList<IncidentRow> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,title,COALESCE(description,''),COALESCE(immediate_action,''),COALESCE(root_cause,''),COALESCE(corrective_action,''),COALESCE(status,'باز'),event_date FROM incidents ORDER BY event_date DESC LIMIT ?",new String[]{String.valueOf(limit)});try{while(c.moveToNext()){IncidentRow r=new IncidentRow();r.id=c.getLong(0);r.title=c.getString(1);r.description=c.getString(2);r.immediate=c.getString(3);r.rootCause=c.getString(4);r.corrective=c.getString(5);r.status=c.getString(6);r.eventDate=c.getLong(7);out.add(r);}}finally{c.close();}return out;}
    /** Returns every unfinished reminder, including overdue items, so they are never silently lost after reboot. */
    public ArrayList<ReminderRow> pendingReminders(long now){ArrayList<ReminderRow> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,COALESCE(title,''),COALESCE(body,''),at_millis,done,COALESCE(delivery_state,'pending'),COALESCE(last_delivery_error,'') FROM reminders WHERE done=0 AND at_millis>0 AND COALESCE(delivery_state,'pending')<>'failed' ORDER BY at_millis ASC LIMIT 100",null);try{while(c.moveToNext()){ReminderRow r=new ReminderRow();r.id=c.getLong(0);r.title=c.getString(1);r.body=c.getString(2);r.at=c.getLong(3);r.done=false;r.deliveryState=c.getString(5);r.lastDeliveryError=c.getString(6);out.add(r);}}finally{c.close();}return out;}
    public long addInspectionFinding(String title,String category,String evidence,String certainty,String hazard,String consequence,String immediate,String corrective,String priority,String confidence,String source,String imagePath){
        ContentValues v=new ContentValues(); v.put("title",limit(title,500)); v.put("category",limit(category,300)); v.put("evidence",limit(evidence,3000)); v.put("certainty",limit(certainty,100)); v.put("hazard",limit(hazard,3000)); v.put("consequence",limit(consequence,3000)); v.put("immediate_action",limit(immediate,3000)); v.put("corrective_action",limit(corrective,5000)); v.put("priority",limit(priority,100)); v.put("confidence",limit(confidence,100)); v.put("source",limit(source==null?"AI":source,30)); v.put("image_path",limit(imagePath,1000)); v.put("status","تاییدشده"); v.put("created_at",System.currentTimeMillis()); return insertReliable("inspection_findings",v);
    }
    public void linkFindingToRisk(long findingId,long riskId){ContentValues v=new ContentValues();v.put("risk_id",riskId);getWritableDatabase().update("inspection_findings",v,"id=?",new String[]{String.valueOf(findingId)});}
    public ArrayList<FindingRow> listInspectionFindings(int limit){limit=clampLimit(limit);ArrayList<FindingRow> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,title,COALESCE(category,''),COALESCE(evidence,''),COALESCE(certainty,''),COALESCE(hazard,''),COALESCE(consequence,''),COALESCE(immediate_action,''),COALESCE(corrective_action,''),COALESCE(priority,''),COALESCE(confidence,''),COALESCE(source,'AI'),COALESCE(image_path,''),risk_id,status,created_at FROM inspection_findings ORDER BY created_at DESC LIMIT ?",new String[]{String.valueOf(limit)});try{while(c.moveToNext()){FindingRow r=new FindingRow();r.id=c.getLong(0);r.title=c.getString(1);r.category=c.getString(2);r.evidence=c.getString(3);r.certainty=c.getString(4);r.hazard=c.getString(5);r.consequence=c.getString(6);r.immediate=c.getString(7);r.corrective=c.getString(8);r.priority=c.getString(9);r.confidence=c.getString(10);r.source=c.getString(11);r.imagePath=c.getString(12);r.riskId=c.isNull(13)?-1:c.getLong(13);r.status=c.getString(14);r.createdAt=c.getLong(15);out.add(r);}}finally{c.close();}return out;}
    public static class FindingRow {public long id,riskId,createdAt;public String title,category,evidence,certainty,hazard,consequence,immediate,corrective,priority,confidence,source,imagePath,status;}

    public static class NcrRow { public long id,findingId,createdAt,dueAt; public String ncrNo,title,description,requirement,immediate,corrective,responsible,status; }
    public long addNcr(long findingId,String title,String description,String requirement,String immediate,String corrective,String responsible,long dueAt){
        String t=limit(title,500); if(t.trim().isEmpty()) throw new IllegalArgumentException("عنوان NCR خالی است");
        long now=System.currentTimeMillis(); String no="NCR-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",java.util.Locale.US).format(new java.util.Date(now))+"-"+findingId;
        ContentValues v=new ContentValues(); v.put("ncr_no",no);v.put("title",t);v.put("description",limit(description,5000));v.put("requirement",limit(requirement,3000));v.put("immediate_action",limit(immediate,3000));v.put("corrective_action",limit(corrective,5000));v.put("responsible",limit(responsible,500));v.put("due_at",dueAt>0?dueAt:0);v.put("status","باز");v.put("finding_id",findingId>0?findingId:null);v.put("created_at",now);return insertReliable("ncrs",v);
    }
    public ArrayList<NcrRow> listNcrs(int limit){limit=clampLimit(limit);ArrayList<NcrRow> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,COALESCE(ncr_no,''),COALESCE(title,''),COALESCE(description,''),COALESCE(requirement,''),COALESCE(immediate_action,''),COALESCE(corrective_action,''),COALESCE(responsible,''),COALESCE(due_at,0),COALESCE(status,'باز'),COALESCE(finding_id,0),created_at FROM ncrs ORDER BY created_at DESC LIMIT ?",new String[]{String.valueOf(limit)});try{while(c.moveToNext()){NcrRow r=new NcrRow();r.id=c.getLong(0);r.ncrNo=c.getString(1);r.title=c.getString(2);r.description=c.getString(3);r.requirement=c.getString(4);r.immediate=c.getString(5);r.corrective=c.getString(6);r.responsible=c.getString(7);r.dueAt=c.getLong(8);r.status=c.getString(9);r.findingId=c.getLong(10);r.createdAt=c.getLong(11);out.add(r);}}finally{c.close();}return out;}
    public void deleteNcr(long id){if(id>0)getWritableDatabase().delete("ncrs","id=?",new String[]{String.valueOf(id)});}

    public Stats stats(){Stats s=new Stats();s.documents=count("documents");s.pages=count("pages");s.notes=count("notes");s.reminders=count("reminders");s.pendingReminders=countWhere("reminders","done=0 AND at_millis>=? AND COALESCE(delivery_state,'pending')<>'failed'",new String[]{String.valueOf(System.currentTimeMillis())});s.risks=count("risks");s.highRisks=countWhere("risks","score>=12 AND status<>?",new String[]{"بسته"});s.incidents=count("incidents");s.openIncidents=countWhere("incidents","status<>?",new String[]{"بسته"});return s;}
    private int countWhere(String table,String where,String[] args){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table+" WHERE "+where,args);try{c.moveToFirst();return c.getInt(0);}finally{c.close();}}
    public int count(String table){if(!Arrays.asList("documents","pages","notes","reminders","risks","incidents","inspection_findings","ncrs").contains(table))throw new IllegalArgumentException("invalid table");Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table,null);try{c.moveToFirst();return c.getInt(0);}finally{c.close();}}
    private static int clampLimit(int n){return Math.max(1,Math.min(n,200));}
    public boolean integrityOk(){
        SQLiteDatabase d=getReadableDatabase();
        Cursor c=d.rawQuery("PRAGMA integrity_check",null);
        try{ if(!c.moveToFirst()||!"ok".equalsIgnoreCase(c.getString(0))) return false; } finally { c.close(); }
        Cursor f=d.rawQuery("PRAGMA foreign_key_check",null);
        try{ return !f.moveToFirst(); } finally { f.close(); }
    }
}
