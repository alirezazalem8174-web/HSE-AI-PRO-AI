package com.ekbatan.hse;

import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;

/**
 * Application data boundary. Features use this facade instead of reaching into SQLite directly.
 * The database implementation remains centralized in LibraryDb for this incremental architecture step.
 */
public final class HseDataRepository {
    private final LibraryDb db;

    public HseDataRepository(LibraryDb db) {
        if (db == null) throw new IllegalArgumentException("db == null");
        this.db = db;
    }

    public ArrayList<LibraryDb.DocumentRow> listDocuments() { return db.listDocuments(); }
    public void deleteDocument(long id) { db.deleteDocument(id); }
    public ArrayList<LibraryDb.SearchHit> searchHits(String query) { return db.searchHits(query); }

    public long addNote(String title, String body) { return db.addNote(title, body); }
    public ArrayList<LibraryDb.NoteRow> listNotes(int limit) { return db.listNotes(limit); }
    public void deleteNote(long id) { db.deleteNote(id); }

    public long addReminder(String title, String body, long at) { return db.addReminder(title, body, at); }
    public ArrayList<LibraryDb.ReminderRow> listReminders(int limit) { return db.listReminders(limit); }
    public LibraryDb.ReminderRow getReminder(long id) { return db.getReminder(id); }
    public void setReminderDone(long id, boolean done) { db.setReminderDone(id, done); }
    public void deleteReminder(long id) { db.deleteReminder(id); }
    public ArrayList<LibraryDb.ReminderRow> pendingReminders(long now) { return db.pendingReminders(now); }

    public long addRisk(String hazard, String consequence, int likelihood, int severity, String controls, String action, String responsible) {
        return db.addRisk(hazard, consequence, likelihood, severity, controls, action, responsible);
    }
    public ArrayList<LibraryDb.RiskRow> listRisks(int limit) { return db.listRisks(limit); }
    public LibraryDb.RiskRow getRisk(long id) { return db.getRisk(id); }
    public void deleteRisk(long id) { db.deleteRisk(id); }

    public long addIncident(String title, long eventDate, String description, String immediate, String rootCause, String corrective) {
        return db.addIncident(title, eventDate, description, immediate, rootCause, corrective);
    }
    public ArrayList<LibraryDb.IncidentRow> listIncidents(int limit) { return db.listIncidents(limit); }
    public void deleteIncident(long id) { db.deleteIncident(id); }

    public long addInspectionFinding(HseAiFinding f, String imagePath) { if(f==null||!f.usable()) throw new IllegalArgumentException("یافته HSE نامعتبر است"); return db.addInspectionFinding(f.title(),f.category,f.evidence,f.certainty,f.hazard,f.consequence,f.immediateAction,f.correctiveAction,f.priority,f.confidence,"AI",imagePath); }
    public void linkFindingToRisk(long findingId,long riskId) { db.linkFindingToRisk(findingId,riskId); }
    public ArrayList<LibraryDb.FindingRow> listInspectionFindings(int limit) { return db.listInspectionFindings(limit); }

    public ArrayList<LibraryDb.NcrRow> listNcrs(int limit) { return db.listNcrs(limit); }
    public long addNcr(long findingId,String title,String description,String requirement,String immediate,String corrective,String responsible,long dueAt) { return db.addNcr(findingId,title,description,requirement,immediate,corrective,responsible,dueAt); }
    public void deleteNcr(long id) { db.deleteNcr(id); }

    public LibraryDb.Stats stats() { return db.stats(); }
    public boolean integrityOk() { return db.integrityOk(); }

    /** Internal bridge used only by HseAppServices for legacy import/backup APIs. */
    LibraryDb rawDatabaseForService() { return db; }

    /** Reserved for future repository-backed transactions without leaking the DB to features. */
    public <T> T readTransaction(DbWork<T> work) throws Exception {
        if (work == null) throw new IllegalArgumentException("work == null");
        SQLiteDatabase readable = db.getReadableDatabase();
        return work.run(readable);
    }

    public interface DbWork<T> { T run(SQLiteDatabase db) throws Exception; }
}
