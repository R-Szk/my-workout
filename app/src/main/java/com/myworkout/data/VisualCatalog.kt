package com.myworkout.data

enum class Muscle(val label: String, val back: Boolean = false) {
    CHEST("大胸筋"), UPPER_CHEST("大胸筋上部"), FRONT_DELTS("三角筋前部"),
    SIDE_DELTS("三角筋中部"), REAR_DELTS("三角筋後部", true), TRICEPS("上腕三頭筋", true),
    BICEPS("上腕二頭筋"), LATS("広背筋", true), MID_BACK("背中中央", true),
    QUADS("大腿四頭筋"), GLUTES("大臀筋", true), GLUTE_MED("中臀筋", true),
    HAMSTRINGS("ハムストリングス", true), ADDUCTORS("内転筋群"), ABS("腹直筋"),
    OBLIQUES("腹斜筋"), ERECTORS("脊柱起立筋", true),
}
enum class LoadType { EXTERNAL, ASSISTANCE }
enum class MuscleRole { PRIMARY, SECONDARY }
data class VisualAsset(val key: String, val name: String, val machineImage: String, val muscleImage: String)
data class VisualProfile(
    val key: String, val label: String, val assetKey: String,
    val primary: Set<Muscle>, val secondary: Set<Muscle> = emptySet(),
    val loadType: LoadType = LoadType.EXTERNAL,
    val useAssetMuscleImage: Boolean = true,
) {
    val machineImage: String get() = VisualCatalog.assets.first { it.key == assetKey }.machineImage
    val muscleImage: String? get() = if (useAssetMuscleImage) VisualCatalog.assets.first { it.key == assetKey }.muscleImage else null
}

// File names and equipment identities follow design-assets/manifest.json.
// Image binaries stay local; no licensed photo is embedded in this source file.
object VisualCatalog {
    val assets = listOf(
        VisualAsset("leg_extension", "レッグエクステンション", "machines/leg_extension.png", "muscles/leg_extension.png"),
        VisualAsset("seated_leg_curl", "シーテッドレッグカール", "machines/seated_leg_curl.png", "muscles/seated_leg_curl.png"),
        VisualAsset("hip_abductor", "ヒップアブダクター", "machines/hip_abductor.png", "muscles/hip_abductor.png"),
        VisualAsset("hip_adductor", "ヒップアダクター", "machines/hip_adductor.png", "muscles/hip_adductor.png"),
        VisualAsset("lat_pulldown_seated_row", "ラットプルダウン・シーテッドロー", "machines/lat_pulldown_seated_row.png", "muscles/lat_pulldown_seated_row.png"),
        VisualAsset("dip_chin_assist", "ディップ・チンアシスト", "machines/dip_chin_assist.png", "muscles/dip_chin_assist.png"),
        VisualAsset("lateral_raise", "ラテラルレイズ", "machines/lateral_raise.png", "muscles/lateral_raise.png"),
        VisualAsset("chest_press", "チェストプレス", "machines/chest_press.png", "muscles/chest_press.png"),
        VisualAsset("shoulder_press_selectorized", "ショルダープレス", "machines/shoulder_press_selectorized.png", "muscles/shoulder_press_selectorized.png"),
        VisualAsset("rear_delt_fly", "リアデルト・フライ", "machines/rear_delt_fly.png", "muscles/rear_delt_fly.png"),
        VisualAsset("rotary_torso", "ロータリートルソー", "machines/rotary_torso.png", "muscles/rotary_torso.png"),
        VisualAsset("abdominal_crunch", "アブドミナルクランチ", "machines/abdominal_crunch.png", "muscles/abdominal_crunch.png"),
        VisualAsset("leg_press_selectorized", "レッグプレス", "machines/leg_press_selectorized.png", "muscles/leg_press_selectorized.png"),
        VisualAsset("functional_trainer", "ファンクショナルトレーナー", "machines/functional_trainer.png", "muscles/functional_trainer.png"),
        VisualAsset("vertical_incline_bench_press", "バーティカルインクラインベンチプレス", "machines/vertical_incline_bench_press.png", "muscles/vertical_incline_bench_press.png"),
        VisualAsset("smith_machine", "スミスマシン", "machines/smith_machine.png", "muscles/smith_machine.png"),
        VisualAsset("leg_press_plate_loaded", "レッグプレス", "machines/leg_press_plate_loaded.png", "muscles/leg_press_plate_loaded.png"),
        VisualAsset("hack_squat", "ハックスクワット", "machines/hack_squat.png", "muscles/hack_squat.png"),
        VisualAsset("incline_lever_row", "インクラインレバーロー", "machines/incline_lever_row.png", "muscles/incline_lever_row.png"),
        VisualAsset("high_row", "ハイロー", "machines/high_row.png", "muscles/high_row.png"),
        VisualAsset("belt_squat", "ベルトスクワット", "machines/belt_squat.png", "muscles/belt_squat.png"),
        VisualAsset("vkr_chin", "VKRチン", "machines/vkr_chin.png", "muscles/vkr_chin.png"),
        VisualAsset("seated_row_plate_loaded", "シーテッドロー", "machines/seated_row_plate_loaded.png", "muscles/seated_row_plate_loaded.png"),
        VisualAsset("incline_bench_press", "インクラインベンチプレス", "machines/incline_bench_press.png", "muscles/incline_bench_press.png"),
        VisualAsset("preacher_curl", "プリチャーカール", "machines/preacher_curl.png", "muscles/preacher_curl.png"),
        VisualAsset("olympic_bench", "オリンピックベンチ", "machines/olympic_bench.png", "muscles/olympic_bench.png"),
        VisualAsset("shoulder_press_plate_loaded", "ショルダープレス", "machines/shoulder_press_plate_loaded.png", "muscles/shoulder_press_plate_loaded.png"),
        VisualAsset("adjustable_incline_bench", "アジャスタブルインクラインベンチ", "machines/adjustable_incline_bench.png", "muscles/adjustable_incline_bench.png"),
        VisualAsset("back_extension_bench", "バックエクステンションベンチ", "machines/back_extension_bench.png", "muscles/back_extension_bench.png"),
    )
    private fun profile(key: String, main: Muscle, vararg helper: Muscle) = VisualProfile(
        key, assets.first { it.key == key }.name + when {
            key.endsWith("selectorized") -> "（ウェイトスタック）"
            key.endsWith("plate_loaded") -> "（プレート式）"
            else -> ""
        }, key, setOf(main), helper.toSet(),
        useAssetMuscleImage = key !in setOf("seated_row_plate_loaded", "incline_lever_row"),
    )
    val profiles: List<VisualProfile> = listOf(
        profile("chest_press", Muscle.CHEST, Muscle.TRICEPS, Muscle.FRONT_DELTS),
        profile("shoulder_press_selectorized", Muscle.FRONT_DELTS, Muscle.SIDE_DELTS, Muscle.TRICEPS),
        profile("shoulder_press_plate_loaded", Muscle.FRONT_DELTS, Muscle.SIDE_DELTS, Muscle.TRICEPS),
        profile("lateral_raise", Muscle.SIDE_DELTS),
        profile("vertical_incline_bench_press", Muscle.UPPER_CHEST, Muscle.FRONT_DELTS, Muscle.TRICEPS),
        profile("incline_bench_press", Muscle.UPPER_CHEST, Muscle.FRONT_DELTS, Muscle.TRICEPS),
        profile("preacher_curl", Muscle.BICEPS),
        profile("leg_press_selectorized", Muscle.QUADS, Muscle.GLUTES),
        profile("leg_press_plate_loaded", Muscle.QUADS, Muscle.GLUTES),
        profile("seated_row_plate_loaded", Muscle.MID_BACK, Muscle.LATS, Muscle.BICEPS),
        profile("leg_extension", Muscle.QUADS), profile("seated_leg_curl", Muscle.HAMSTRINGS),
        profile("hip_abductor", Muscle.GLUTE_MED), profile("hip_adductor", Muscle.ADDUCTORS),
        profile("rear_delt_fly", Muscle.REAR_DELTS, Muscle.MID_BACK),
        profile("rotary_torso", Muscle.OBLIQUES), profile("abdominal_crunch", Muscle.ABS),
        profile("hack_squat", Muscle.QUADS, Muscle.GLUTES), profile("belt_squat", Muscle.QUADS, Muscle.GLUTES),
        profile("incline_lever_row", Muscle.MID_BACK, Muscle.LATS, Muscle.BICEPS),
        profile("high_row", Muscle.LATS, Muscle.MID_BACK, Muscle.BICEPS),
        profile("olympic_bench", Muscle.CHEST, Muscle.TRICEPS, Muscle.FRONT_DELTS),
        profile("back_extension_bench", Muscle.ERECTORS, Muscle.GLUTES, Muscle.HAMSTRINGS),
        VisualProfile("lat_pulldown", "ラットプルダウン", "lat_pulldown_seated_row", setOf(Muscle.LATS), setOf(Muscle.MID_BACK, Muscle.BICEPS)),
        VisualProfile("seated_row_cable", "シーテッドロー（複合機）", "lat_pulldown_seated_row", setOf(Muscle.MID_BACK), setOf(Muscle.LATS, Muscle.BICEPS), useAssetMuscleImage = false),
        VisualProfile("assisted_dip", "アシストディップス", "dip_chin_assist", setOf(Muscle.CHEST, Muscle.TRICEPS), setOf(Muscle.FRONT_DELTS), LoadType.ASSISTANCE, false),
        VisualProfile("assisted_chin", "アシストチンアップ", "dip_chin_assist", setOf(Muscle.LATS), setOf(Muscle.BICEPS, Muscle.MID_BACK), LoadType.ASSISTANCE, false),
    )
    fun find(key: String?): VisualProfile? = profiles.find { it.key == key }
    fun defaultForMachine(id: Long): VisualProfile? = find(when (id) {
        1L -> "chest_press"; 2L -> "lat_pulldown"; 4L -> "lateral_raise"
        6L -> "vertical_incline_bench_press"; 8L -> "preacher_curl"; else -> null
    })
    fun options(id: Long): List<VisualProfile> = when (id) {
        2L -> listOfNotNull(find("lat_pulldown"), find("seated_row_cable"))
        3L -> listOfNotNull(find("shoulder_press_selectorized"), find("shoulder_press_plate_loaded"))
        5L -> listOfNotNull(find("assisted_dip"), find("assisted_chin"))
        7L -> listOfNotNull(find("seated_row_cable"), find("seated_row_plate_loaded"))
        9L -> listOfNotNull(find("leg_press_selectorized"), find("leg_press_plate_loaded"))
        else -> listOfNotNull(defaultForMachine(id))
    }
    fun resolve(exercise: SessionExercise): VisualProfile? = find(exercise.visualKey)
        ?: if (exercise.visualKey == null) defaultForMachine(exercise.machineId) else null
    fun comparisonKey(exercise: SessionExercise): String? = resolve(exercise)?.let { "${exercise.machineId}:${it.key}" }
}
