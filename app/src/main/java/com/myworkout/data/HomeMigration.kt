package com.myworkout.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Add home catalog identity and make bodyweight records genuinely weightless. Never recreate user data. */
object HomeMigration : Migration(4,5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val sequence = db.query("SELECT seq FROM sqlite_sequence WHERE name='set_records'").use { if (it.moveToFirst()) it.getLong(0) else 0L }
        db.execSQL("ALTER TABLE machines ADD COLUMN catalogKey TEXT")
        db.execSQL("CREATE UNIQUE INDEX index_machines_catalogKey ON machines(catalogKey)")
        db.execSQL("ALTER TABLE routines ADD COLUMN catalogKey TEXT")
        db.execSQL("CREATE UNIQUE INDEX index_routines_catalogKey ON routines(catalogKey)")
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN routineKey TEXT")
        db.execSQL("ALTER TABLE session_exercises ADD COLUMN loadType TEXT")
        db.execSQL("""CREATE TABLE set_records_home (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            sessionExerciseId INTEGER NOT NULL, sequence INTEGER NOT NULL, weightKg REAL,
            reps INTEGER NOT NULL, completedAt INTEGER NOT NULL, requestId TEXT NOT NULL,
            FOREIGN KEY(sessionExerciseId) REFERENCES session_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("INSERT INTO set_records_home SELECT * FROM set_records")
        db.execSQL("DROP TABLE set_records")
        db.execSQL("ALTER TABLE set_records_home RENAME TO set_records")
        db.execSQL("CREATE UNIQUE INDEX index_set_records_sessionExerciseId_sequence ON set_records(sessionExerciseId, sequence)")
        db.execSQL("CREATE UNIQUE INDEX index_set_records_requestId ON set_records(requestId)")
        db.execSQL("DELETE FROM sqlite_sequence WHERE name='set_records'")
        db.execSQL("INSERT INTO sqlite_sequence(name,seq) VALUES('set_records',?)",arrayOf(sequence))
    }
}
