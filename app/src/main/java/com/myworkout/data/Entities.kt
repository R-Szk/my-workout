package com.myworkout.data

import androidx.room.*

@Entity(tableName = "machines", indices = [Index(value = ["catalogKey"], unique = true)])
data class Machine(
    @PrimaryKey val id: Long,
    val name: String,
    val imageAsset: String? = null,
    val notes: String = "",
    val visualKey: String? = null,
    val catalogKey: String? = null,
)

@Entity(tableName = "routines", indices = [Index(value = ["catalogKey"], unique = true)])
data class Routine(@PrimaryKey val id: Long, val name: String, val catalogKey: String? = null)

@Entity(
    tableName = "routine_exercises",
    primaryKeys = ["routineId", "position"],
    foreignKeys = [
        ForeignKey(entity = Routine::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Machine::class, parentColumns = ["id"], childColumns = ["machineId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("machineId")],
)
data class RoutineExercise(
    val routineId: Long,
    val machineId: Long,
    val position: Int,
    val targetSets: Int,
    val targetReps: Int,
)

data class ExerciseWithMachine(
    @Embedded val exercise: RoutineExercise,
    @Relation(parentColumn = "machineId", entityColumn = "id") val machine: Machine,
)

// Room relation collections do not guarantee ordering; sort at the repository boundary.
data class RoutineWithExercises(
    @Embedded val routine: Routine,
    @Relation(entity = RoutineExercise::class, parentColumn = "id", entityColumn = "routineId")
    val exercises: List<ExerciseWithMachine>,
)

enum class SessionStatus { ACTIVE, PAUSED, COMPLETED }
enum class ExerciseStatus { PENDING, SKIPPED }

@Entity(tableName = "workout_sessions", indices = [Index(value = ["activeSlot"], unique = true)])
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val routineName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val currentPosition: Int = 0,
    // SQLite allows multiple NULLs in a unique index, but only one unfinished workout.
    val activeSlot: Int? = 1,
    val routineKey: String? = null,
    val activeDurationMillis: Long? = null,
    val activeStartedAt: Long? = null,
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [ForeignKey(entity = WorkoutSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["sessionId", "position"], unique = true)],
)
data class SessionExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val machineId: Long,
    val machineName: String,
    val notes: String,
    val position: Int,
    val targetSets: Int,
    val targetReps: Int,
    @ColumnInfo(defaultValue = "'PENDING'") val status: ExerciseStatus = ExerciseStatus.PENDING,
    val visualKey: String? = null,
    val loadType: LoadType? = null,
)

@Entity(
    tableName = "set_records",
    foreignKeys = [ForeignKey(entity = SessionExercise::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["sessionExerciseId", "sequence"], unique = true), Index(value = ["requestId"], unique = true)],
)
data class SetRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val sequence: Int,
    val weightKg: Double?,
    val reps: Int,
    val completedAt: Long,
    val requestId: String,
)

data class ExerciseHistory(
    @Embedded val exercise: SessionExercise,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId") val sets: List<SetRecord>,
)
data class WorkoutHistory(
    @Embedded val session: WorkoutSession,
    @Relation(entity = SessionExercise::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<ExerciseHistory>,
)
